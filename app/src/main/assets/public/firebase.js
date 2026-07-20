// Firebase module for "Firing For Cash"
// Handles real-time DB synchronization, authentication helpers, and network presence monitoring

class FirebaseManager {
  constructor() {
    this.db = null;
    this.app = null;
    this.isOnline = navigator.onLine;
    this.initNetworkMonitoring();
  }

  init() {
    try {
      const config = {
        apiKey: window.CONFIG.firebase.apiKey,
        databaseURL: window.CONFIG.firebase.databaseURL,
        projectId: "firing-for-cash-default-rtdb",
        appId: window.CONFIG.firebase.appId,
        messagingSenderId: window.CONFIG.firebase.messagingSenderId
      };

      // Check if firebase is available from imported CDN scripts
      if (typeof firebase !== 'undefined') {
        this.app = firebase.initializeApp(config);
        this.db = firebase.database();
        console.log("Firebase initialized successfully");
        this.listenToPresence();
      } else {
        console.warn("Firebase script not loaded yet. Operating in local-fallback mode.");
      }
    } catch (e) {
      console.error("Failed to initialize Firebase:", e);
    }
  }

  initNetworkMonitoring() {
    // Standard JS network listeners
    window.addEventListener('online', () => this.handleNetworkChange(true));
    window.addEventListener('offline', () => this.handleNetworkChange(false));
    
    // Custom bridge function for Native Android (ACCESS_NETWORK_STATE drops)
    window.setNetworkOnline = (online) => {
      this.handleNetworkChange(online);
    };
  }

  handleNetworkChange(online) {
    this.isOnline = online;
    console.log(`Network status changed: ${online ? 'ONLINE' : 'OFFLINE'}`);
    
    // Dispatch event to freeze or resume game
    const event = new CustomEvent('networkStatusChange', { detail: { online } });
    window.dispatchEvent(event);
  }

  listenToPresence() {
    if (!this.db) return;
    const connectedRef = this.db.ref(".info/connected");
    connectedRef.on("value", (snap) => {
      if (snap.val() === true) {
        console.log("Connected to Firebase Realtime Database");
      } else {
        console.log("Disconnected from Firebase Realtime Database");
      }
    });
  }

  // Submit high score and virtual coin statistics to Leaderboard (Transactional DB writes)
  async submitLeaderboardData(userId, email, coins, score) {
    if (!this.isOnline) {
      this.saveLocalLeaderboardFallback(userId, email, coins, score);
      return;
    }

    if (!this.db) {
      this.saveLocalLeaderboardFallback(userId, email, coins, score);
      return;
    }

    try {
      const userRef = this.db.ref(`leaderboard/${userId}`);
      await userRef.transaction((currentData) => {
        if (currentData === null) {
          return { email, coins, score, timestamp: firebase.database.ServerValue.TIMESTAMP };
        } else {
          // Keep highest score and add new coins safely
          const updatedCoins = Math.max(currentData.coins || 0, coins);
          const updatedScore = Math.max(currentData.score || 0, score);
          return {
            email,
            coins: updatedCoins,
            score: updatedScore,
            timestamp: firebase.database.ServerValue.TIMESTAMP
          };
        }
      });
      console.log("Leaderboard synchronized via Firebase Transaction");
    } catch (error) {
      console.error("Firebase transaction failed, falling back to local storage:", error);
      this.saveLocalLeaderboardFallback(userId, email, coins, score);
    }
  }

  saveLocalLeaderboardFallback(userId, email, coins, score) {
    let locals = JSON.parse(localStorage.getItem('local_leaderboard') || '{}');
    locals[userId] = {
      email,
      coins: Math.max((locals[userId]?.coins || 0), coins),
      score: Math.max((locals[userId]?.score || 0), score),
      timestamp: Date.now()
    };
    localStorage.setItem('local_leaderboard', JSON.stringify(locals));
    console.log("Leaderboard saved to local cache due to offline state.");
  }

  // Retrieve top leaderboard list
  fetchLeaderboard(callback) {
    if (!this.isOnline || !this.db) {
      const locals = JSON.parse(localStorage.getItem('local_leaderboard') || '{}');
      const sorted = Object.values(locals).sort((a, b) => b.score - a.score).slice(0, 10);
      callback(sorted);
      return;
    }

    const leaderboardRef = this.db.ref('leaderboard').orderByChild('score').limitToLast(15);
    leaderboardRef.on('value', (snapshot) => {
      const list = [];
      snapshot.forEach((child) => {
        list.push({ id: child.key, ...child.val() });
      });
      list.reverse(); // Descending order
      callback(list);
    }, (error) => {
      console.error("Failed to fetch leaderboard from Firebase:", error);
    });
  }
}

// Global instance
window.firebaseManager = new FirebaseManager();
document.addEventListener("DOMContentLoaded", () => {
  window.firebaseManager.init();
});
