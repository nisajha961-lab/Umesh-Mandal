// "Firing For Cash" Core Game Client & UI Controller
// Includes the HTML5 2D Canvas game loop, Ad simulation metrics, virtual stats, security bot caps, and admin gateways.

class GameApp {
  constructor() {
    this.currentUser = null;
    this.virtualCoins = 0;
    this.totalScore = 0;
    this.sessionRewardedAdCount = 0;
    this.adImpressions = { banner: 0, interstitial: 0, rewarded: 0 };
    this.titleClickCount = 0;
    this.isAdminMode = false;

    // Capping Protection: local sliding window array of {timestamp: ms, coins: number}
    this.coinEarningHistory = [];

    // Core Canvas components
    this.canvas = document.getElementById("game-canvas");
    this.ctx = this.canvas ? this.canvas.getContext("2d") : null;
    this.gameRunning = false;
    this.gameLoopId = null;
    this.canvasWidth = 360;
    this.canvasHeight = 500;

    // Entities
    this.player = { x: 180, y: 440, size: 24, speed: 6, dx: 0 };
    this.bullets = [];
    this.enemies = [];
    this.particles = [];
    this.score = 0;
    this.destroyedTargets = 0;

    // Keys
    this.keys = { ArrowLeft: false, ArrowRight: false };

    this.bindEvents();
    this.initLocalStats();
  }

  initLocalStats() {
    // Load local stats if logged in
    const cachedUser = localStorage.getItem("current_user");
    if (cachedUser) {
      this.currentUser = JSON.parse(cachedUser);
      this.virtualCoins = parseInt(localStorage.getItem(`coins_${this.currentUser.email}`) || "0");
      this.totalScore = parseInt(localStorage.getItem(`score_${this.currentUser.email}`) || "0");
      this.showDashboard();
    }
  }

  bindEvents() {
    // Tab Navigation
    const tabs = document.querySelectorAll("#bottom-tabs .nav-tab");
    tabs.forEach(tab => {
      tab.addEventListener("click", (e) => {
        const target = e.currentTarget.getAttribute("data-target");
        this.switchScreen(target);
        
        // Style changes
        tabs.forEach(t => t.classList.remove("active"));
        e.currentTarget.classList.add("active");
      });
    });

    // Auth Submission
    const authForm = document.getElementById("auth-form");
    if (authForm) {
      authForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const email = document.getElementById("auth-email").value.trim();
        const pass = document.getElementById("auth-password").value.trim();
        
        // Simple client auth mock
        this.currentUser = { email, userId: btoa(email).replace(/=/g, "") };
        localStorage.setItem("current_user", JSON.stringify(this.currentUser));
        
        this.virtualCoins = parseInt(localStorage.getItem(`coins_${this.currentUser.email}`) || "0");
        this.totalScore = parseInt(localStorage.getItem(`score_${this.currentUser.email}`) || "0");
        
        this.showDashboard();
      });
    }

    // Launch game button
    const btnLaunch = document.getElementById("btn-launch-game");
    if (btnLaunch) {
      btnLaunch.addEventListener("click", () => {
        this.switchScreen("screen-game");
        document.getElementById("bottom-tabs").style.display = "none";
        this.startGame();
      });
    }

    // Touch controls on canvas
    if (this.canvas) {
      this.canvas.addEventListener("touchstart", (e) => {
        const touch = e.touches[0];
        const rect = this.canvas.getBoundingClientRect();
        const relativeX = touch.clientX - rect.left;
        const width = rect.right - rect.left;
        
        // Tap left or right half
        if (relativeX < width / 2) {
          this.player.dx = -this.player.speed;
        } else {
          this.player.dx = this.player.speed;
        }
        
        // Simple auto-shoot on touch
        this.shootBullet();
      });

      this.canvas.addEventListener("touchmove", (e) => {
        const touch = e.touches[0];
        const rect = this.canvas.getBoundingClientRect();
        const relativeX = touch.clientX - rect.left;
        const width = rect.right - rect.left;
        
        if (relativeX < width / 2) {
          this.player.dx = -this.player.speed;
        } else {
          this.player.dx = this.player.speed;
        }
      });

      this.canvas.addEventListener("touchend", () => {
        this.player.dx = 0;
      });

      // Desktop backup keys
      window.addEventListener("keydown", (e) => {
        if (e.key === "ArrowLeft" || e.key === "a") this.player.dx = -this.player.speed;
        if (e.key === "ArrowRight" || e.key === "d") this.player.dx = this.player.speed;
        if (e.key === " " || e.key === "ArrowUp") this.shootBullet();
      });

      window.addEventListener("keyup", (e) => {
        if (["ArrowLeft", "a", "ArrowRight", "d"].includes(e.key)) {
          this.player.dx = 0;
        }
      });
    }

    // Game Over actions
    document.getElementById("game-btn-revive").addEventListener("click", () => this.handleRevive());
    document.getElementById("game-btn-double").addEventListener("click", () => this.handleDoubleCoins());
    document.getElementById("game-btn-exit").addEventListener("click", () => {
      this.stopGameLoop();
      document.getElementById("bottom-tabs").style.display = "flex";
      this.showDashboard();
    });

    // Withdrawal Form conversions
    const withdrawAmountInput = document.getElementById("withdraw-amount");
    if (withdrawAmountInput) {
      withdrawAmountInput.addEventListener("input", (e) => {
        const amount = parseInt(e.target.value) || 0;
        // Formula: Let's calculate eCPM-based earnings yield. For mock math, 1 Coin is ₹0.10
        const raw = amount * 0.10;
        const tax = raw * 0.30;
        const net = raw - tax;
        
        document.getElementById("withdraw-raw-val").innerText = `₹${raw.toFixed(2)}`;
        document.getElementById("withdraw-tax-val").innerText = `₹${tax.toFixed(2)}`;
        document.getElementById("withdraw-net-val").innerText = `₹${net.toFixed(2)}`;
      });
    }

    const withdrawalForm = document.getElementById("withdrawal-form");
    if (withdrawalForm) {
      withdrawalForm.addEventListener("submit", (e) => {
        e.preventDefault();
        this.handleWithdrawalSubmit();
      });
    }

    // Secret Admin Gate sequence
    const trigger = document.getElementById("secret-title-trigger");
    if (trigger) {
      trigger.addEventListener("click", () => {
        this.titleClickCount++;
        console.log(`Title Click Count: ${this.titleClickCount}`);
        if (this.titleClickCount === 5) {
          this.titleClickCount = 0;
          this.switchScreen("screen-admin");
          document.getElementById("admin-auth-panel").style.display = "block";
          document.getElementById("admin-content-panel").style.display = "none";
        }
      });
    }

    // Admin authorization
    document.getElementById("btn-submit-admin-pin").addEventListener("click", () => {
      const pin = document.getElementById("admin-pin").value;
      if (pin === window.CONFIG.pin) {
        document.getElementById("admin-auth-panel").style.display = "none";
        document.getElementById("admin-content-panel").style.display = "block";
        this.isAdminMode = true;
        this.renderAdminMetrics();
      } else {
        alert("ACCESS_DENIED: ROOT PROTECTED PIN INVALID");
      }
    });

    // Admin modification
    document.getElementById("btn-admin-set-coins").addEventListener("click", () => {
      const targetCoins = parseInt(document.getElementById("admin-set-coins").value) || 0;
      this.virtualCoins = targetCoins;
      if (this.currentUser) {
        localStorage.setItem(`coins_${this.currentUser.email}`, this.virtualCoins);
      }
      alert(`Virtual coins updated to: ${targetCoins}`);
      this.renderAdminMetrics();
    });

    document.getElementById("btn-admin-export-logs").addEventListener("click", () => {
      this.exportDiagnosticLogs();
    });

    document.getElementById("btn-admin-exit").addEventListener("click", () => {
      this.isAdminMode = false;
      this.showDashboard();
    });

    // Network interruption listener
    window.addEventListener("networkStatusChange", (e) => {
      const online = e.detail.online;
      const overlay = document.getElementById("network-overlay");
      const statusText = document.getElementById("connection-status");
      
      if (online) {
        overlay.classList.remove("visible");
        statusText.style.color = "var(--neon-green)";
        statusText.innerText = "SYS_ACTIVE: ONLINE";
        if (this.gameRunning && this.gameLoopId === null) {
          this.resumeGameLoop();
        }
      } else {
        overlay.classList.add("visible");
        statusText.style.color = "var(--neon-magenta)";
        statusText.innerText = "SYS_ACTIVE: OFFLINE";
        if (this.gameRunning) {
          this.pauseGameLoop();
        }
      }
    });
  }

  switchScreen(screenId) {
    const screens = document.querySelectorAll(".screen");
    screens.forEach(s => s.classList.remove("active"));
    const target = document.getElementById(screenId);
    if (target) {
      target.classList.add("active");
    }
    
    if (screenId === "screen-leaderboard") {
      this.updateLeaderboardUI();
    }
  }

  showDashboard() {
    this.switchScreen("screen-dashboard");
    document.getElementById("bottom-tabs").style.display = "flex";
    
    // Set text fields
    document.getElementById("user-display-email").innerText = `USER: ${this.currentUser ? this.currentUser.email : 'GUEST'}`;
    document.getElementById("dash-cyber-coins").innerText = this.virtualCoins;
    
    // Computed value: 1 Virtual Cyber Coin = ₹0.10
    const estVal = this.virtualCoins * 0.10;
    document.getElementById("dash-earnings").innerText = `₹${estVal.toFixed(2)}`;
  }

  // ----------------------------------------------------
  // Anti-Bot Capping Protection logic
  // Max 20 virtual coins per 30 minutes
  // ----------------------------------------------------
  checkCappingLimits() {
    const now = Date.now();
    const halfHourAgo = now - window.CONFIG.security.timeWindowMs;
    
    // Prune entries older than 30 minutes
    this.coinEarningHistory = this.coinEarningHistory.filter(h => h.timestamp > halfHourAgo);
    
    // Sum coins earned in the window
    const totalEarned = this.coinEarningHistory.reduce((sum, item) => sum + item.coins, 0);
    return totalEarned >= window.CONFIG.security.maxCoinsPerWindow;
  }

  recordCoinEarned(quantity) {
    this.coinEarningHistory.push({ timestamp: Date.now(), coins: quantity });
  }

  // ----------------------------------------------------
  // Ad Simulation Logic
  // ----------------------------------------------------
  showBannerAd() {
    this.adImpressions.banner++;
    console.log("Banner Ad simulated. ID: " + window.CONFIG.admob.bannerId);
  }

  showInterstitialAd() {
    this.adImpressions.interstitial++;
    console.log("Interstitial Ad simulated. ID: " + window.CONFIG.admob.interstitialId);
    alert("[AdMob Simulator] Interstitial Ad Displayed (eCPM baseline ₹30)");
  }

  showRewardedAd(onEarnedReward) {
    this.adImpressions.rewarded++;
    console.log("Rewarded Ad simulated. ID: " + window.CONFIG.admob.rewardedId);
    alert("[AdMob Simulator] Rewarded Video Ad Displayed (eCPM baseline ₹70)");
    onEarnedReward();
  }

  // ----------------------------------------------------
  // Space Defender HTML5 Canvas Game Engine
  // ----------------------------------------------------
  startGame() {
    this.gameRunning = true;
    this.score = 0;
    this.destroyedTargets = 0;
    this.sessionRewardedAdCount = 0;
    this.bullets = [];
    this.enemies = [];
    this.particles = [];
    this.player.x = 180;
    this.player.dx = 0;

    // Canvas size adjustment
    const viewport = this.canvas.parentElement;
    this.canvas.width = viewport.clientWidth;
    this.canvas.height = viewport.clientHeight;
    this.canvasWidth = this.canvas.width;
    this.canvasHeight = this.canvas.height;

    // Trigger regular Banner simulation
    this.showBannerAd();

    document.getElementById("game-overlay-panel").style.display = "none";
    this.resumeGameLoop();
  }

  resumeGameLoop() {
    this.gameLoopId = requestAnimationFrame(() => this.loop());
  }

  pauseGameLoop() {
    if (this.gameLoopId) {
      cancelAnimationFrame(this.gameLoopId);
      this.gameLoopId = null;
    }
  }

  stopGameLoop() {
    this.gameRunning = false;
    this.pauseGameLoop();
  }

  loop() {
    if (!this.gameRunning) return;
    
    this.update();
    this.draw();
    
    this.gameLoopId = requestAnimationFrame(() => this.loop());
  }

  update() {
    // Move player
    this.player.x += this.player.dx;
    // Bounds check
    if (this.player.x < this.player.size) this.player.x = this.player.size;
    if (this.player.x > this.canvasWidth - this.player.size) {
      this.player.x = this.canvasWidth - this.player.size;
    }

    // Spawning enemies / network target nodes
    if (Math.random() < 0.02) {
      this.enemies.push({
        x: Math.random() * (this.canvasWidth - 40) + 20,
        y: -20,
        size: 15,
        speed: 2 + Math.random() * 2
      });
    }

    // Move bullets
    this.bullets.forEach((bullet, index) => {
      bullet.y -= bullet.speed;
      if (bullet.y < 0) {
        this.bullets.splice(index, 1);
      }
    });

    // Move enemies
    this.enemies.forEach((enemy, index) => {
      enemy.y += enemy.speed;
      
      // Collision with player
      const distToPlayer = Math.hypot(enemy.x - this.player.x, enemy.y - this.player.y);
      if (distToPlayer < enemy.size + this.player.size) {
        this.enemies.splice(index, 1);
        this.triggerGameOver();
      }

      // Out of bounds check
      if (enemy.y > this.canvasHeight + 20) {
        this.enemies.splice(index, 1);
      }
    });

    // Handle bullet-target collision vectors
    this.bullets.forEach((b, bIdx) => {
      this.enemies.forEach((e, eIdx) => {
        const dist = Math.hypot(b.x - e.x, b.y - e.y);
        if (dist < e.size + 4) {
          // Explode particles
          this.createExplosion(e.x, e.y);
          
          this.bullets.splice(bIdx, 1);
          this.enemies.splice(eIdx, 1);
          
          this.score += 100;
          this.destroyedTargets++;
          
          // Math Matrix: 20 targets = 1 virtual coin
          if (this.destroyedTargets % window.CONFIG.economics.targetsPerCoin === 0) {
            this.rewardCoinFromPlay();
          }
          
          this.updateHUD();
        }
      });
    });

    // Update explosions particles
    this.particles.forEach((p, index) => {
      p.x += p.vx;
      p.y += p.vy;
      p.alpha -= 0.02;
      if (p.alpha <= 0) {
        this.particles.splice(index, 1);
      }
    });
  }

  draw() {
    this.ctx.fillStyle = "#000000";
    this.ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);

    // Draw grid star fields
    this.ctx.strokeStyle = "rgba(0, 243, 255, 0.05)";
    this.ctx.lineWidth = 1;
    for (let x = 0; x < this.canvasWidth; x += 30) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, this.canvasHeight);
      this.ctx.stroke();
    }
    for (let y = 0; y < this.canvasHeight; y += 30) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(this.canvasWidth, y);
      this.ctx.stroke();
    }

    // Draw ship
    this.ctx.fillStyle = "transparent";
    this.ctx.strokeStyle = "var(--neon-cyan)";
    this.ctx.lineWidth = 2;
    this.ctx.shadowBlur = 10;
    this.ctx.shadowColor = "var(--neon-cyan)";
    
    this.ctx.beginPath();
    this.ctx.moveTo(this.player.x, this.player.y - this.player.size);
    this.ctx.lineTo(this.player.x - this.player.size, this.player.y + this.player.size);
    this.ctx.lineTo(this.player.x + this.player.size, this.player.y + this.player.size);
    this.ctx.closePath();
    this.ctx.stroke();
    this.ctx.shadowBlur = 0;

    // Draw bullets
    this.ctx.fillStyle = "var(--neon-green)";
    this.bullets.forEach(b => {
      this.ctx.beginPath();
      this.ctx.arc(b.x, b.y, 4, 0, Math.PI * 2);
      this.ctx.fill();
    });

    // Draw enemies
    this.ctx.strokeStyle = "var(--neon-magenta)";
    this.ctx.lineWidth = 2;
    this.enemies.forEach(e => {
      this.ctx.shadowBlur = 8;
      this.ctx.shadowColor = "var(--neon-magenta)";
      this.ctx.beginPath();
      this.ctx.arc(e.x, e.y, e.size, 0, Math.PI * 2);
      this.ctx.stroke();
      this.ctx.shadowBlur = 0;
    });

    // Draw explosion particles
    this.particles.forEach(p => {
      this.ctx.fillStyle = `rgba(255, 0, 85, ${p.alpha})`;
      this.ctx.beginPath();
      this.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      this.ctx.fill();
    });
  }

  shootBullet() {
    this.bullets.push({
      x: this.player.x,
      y: this.player.y - this.player.size,
      speed: 8
    });
  }

  createExplosion(x, y) {
    for (let i = 0; i < 8; i++) {
      this.particles.push({
        x,
        y,
        size: 2 + Math.random() * 3,
        vx: (Math.random() - 0.5) * 6,
        vy: (Math.random() - 0.5) * 6,
        alpha: 1
      });
    }
  }

  updateHUD() {
    document.getElementById("game-hud-score").innerText = this.score;
    document.getElementById("game-hud-destroyed").innerText = this.destroyedTargets;
    
    const count = Math.floor(this.destroyedTargets / window.CONFIG.economics.targetsPerCoin);
    document.getElementById("game-hud-coins").innerText = count;
  }

  rewardCoinFromPlay() {
    // Safe-check bot caps
    if (this.checkCappingLimits()) {
      document.getElementById("game-cooldown-text").innerText = "CAP_REACHED (COOLDOWN)";
      document.getElementById("game-cooldown-text").style.color = "var(--neon-magenta)";
      console.warn("Anti-bot threshold exceeded. Earning rejected.");
      return;
    }

    this.recordCoinEarned(1);
    this.virtualCoins += 1;
    
    // Sync locally
    if (this.currentUser) {
      localStorage.setItem(`coins_${this.currentUser.email}`, this.virtualCoins);
    }
  }

  triggerGameOver() {
    this.stopGameLoop();
    this.showInterstitialAd();

    // Calculate coins yielded this run
    const runCoins = Math.floor(this.destroyedTargets / window.CONFIG.economics.targetsPerCoin);
    const isCapped = this.checkCappingLimits();

    // Set UI elements
    document.getElementById("game-over-final-score").innerText = this.score;
    document.getElementById("game-over-earned-coins").innerText = runCoins;
    document.getElementById("game-over-capped").innerText = isCapped ? "YES (COOLDOWN)" : "NO";

    // Disable revive if limit exceeded
    const btnRevive = document.getElementById("game-btn-revive");
    if (this.sessionRewardedAdCount >= window.CONFIG.economics.maxRewardedPerSession) {
      btnRevive.innerText = "REVIVE_LIMIT_REACHED (MAX 4)";
      btnRevive.disabled = true;
    } else {
      btnRevive.innerText = `WATCH_REWARD_AD_REVIVE (${4 - this.sessionRewardedAdCount} Left)`;
      btnRevive.disabled = false;
    }

    // Save final stats to Firebase
    if (this.currentUser) {
      if (this.score > this.totalScore) {
        this.totalScore = this.score;
        localStorage.setItem(`score_${this.currentUser.email}`, this.totalScore);
      }
      window.firebaseManager.submitLeaderboardData(
        this.currentUser.userId,
        this.currentUser.email,
        this.virtualCoins,
        this.totalScore
      );
    }

    // Display overlay
    document.getElementById("game-overlay-panel").style.display = "flex";
  }

  handleRevive() {
    if (this.sessionRewardedAdCount >= window.CONFIG.economics.maxRewardedPerSession) return;
    
    this.showRewardedAd(() => {
      this.sessionRewardedAdCount++;
      // Resume game
      this.enemies = [];
      this.bullets = [];
      this.gameRunning = true;
      document.getElementById("game-overlay-panel").style.display = "none";
      this.resumeGameLoop();
    });
  }

  handleDoubleCoins() {
    const runCoins = Math.floor(this.destroyedTargets / window.CONFIG.economics.targetsPerCoin);
    if (runCoins <= 0) {
      alert("No coins to double this session.");
      return;
    }

    this.showRewardedAd(() => {
      // Check limits before granting double reward
      if (this.checkCappingLimits()) {
        alert("Anti-Bot Protection Activated. Double coin yield blocked.");
        return;
      }

      this.recordCoinEarned(runCoins);
      this.virtualCoins += runCoins;
      
      if (this.currentUser) {
        localStorage.setItem(`coins_${this.currentUser.email}`, this.virtualCoins);
        window.firebaseManager.submitLeaderboardData(
          this.currentUser.userId,
          this.currentUser.email,
          this.virtualCoins,
          this.totalScore
        );
      }

      alert(`Double coins granted! Earned additional: ${runCoins} Virtual Coins`);
      document.getElementById("game-btn-double").disabled = true;
      document.getElementById("game-hud-coins").innerText = Math.floor(this.destroyedTargets / window.CONFIG.economics.targetsPerCoin) * 2;
    });
  }

  // ----------------------------------------------------
  // Leaderboard Updates
  // ----------------------------------------------------
  updateLeaderboardUI() {
    const container = document.getElementById("leaderboard-container");
    container.innerHTML = "<li class='leaderboard-item'>Connecting to mesh database...</li>";

    window.firebaseManager.fetchLeaderboard((list) => {
      container.innerHTML = "";
      if (list.length === 0) {
        container.innerHTML = "<li class='leaderboard-item'>No current data logged. Play to compete.</li>";
        return;
      }
      
      list.forEach((item, index) => {
        const li = document.createElement("li");
        li.className = "leaderboard-item";
        li.innerHTML = `
          <span>#${index + 1} ${item.email}</span>
          <span>SCORE: ${item.score} (${item.coins} Coins)</span>
        `;
        container.appendChild(li);
      });
    });
  }

  // ----------------------------------------------------
  // Simulated Payout Request Logic
  // ----------------------------------------------------
  handleWithdrawalSubmit() {
    const method = document.getElementById("withdraw-method").value;
    const address = document.getElementById("withdraw-address").value.trim();
    const coins = parseInt(document.getElementById("withdraw-amount").value) || 0;
    const errorMsg = document.getElementById("withdraw-address-error");

    errorMsg.style.display = "none";

    // RegExp Validation based on method
    let isValid = false;
    if (method === "UPI") {
      // Basic UPI validator
      const upiRegex = /^[\w\.\-]+@[\w\-]+$/;
      isValid = upiRegex.test(address);
      if (!isValid) errorMsg.innerText = "INVALID_UPI_FORMAT: ID must match 'user@provider'";
    } else if (method === "PAYTM") {
      // 10 digit Indian number
      const paytmRegex = /^[6-9]\d{9}$/;
      isValid = paytmRegex.test(address);
      if (!isValid) errorMsg.innerText = "INVALID_PAYTM_NUMBER: Must be a valid 10-digit mobile";
    } else if (method === "PAYPAL") {
      // Standard Email pattern
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      isValid = emailRegex.test(address);
      if (!isValid) errorMsg.innerText = "INVALID_PAYPAY_EMAIL: Format not recognized";
    }

    if (!isValid) {
      errorMsg.style.display = "block";
      return;
    }

    if (coins < 100) {
      alert("EXCHANGE_REJECTED: Minimum exchange amount is 100 Virtual Coins.");
      return;
    }

    if (this.virtualCoins < coins) {
      alert("INSUFFICIENT_FUNDS: Your account balance is too low.");
      return;
    }

    // LOCK DATE EXCEPTION CHECK: Hard lock withdrawal until the 30th
    const now = new Date();
    if (now.getDate() !== 30) {
      alert("TRANSACTION_FAILED: EXCHANGE WINDOW SYSTEM LOCKED. Processing window opens exclusively on the 30th of the month.");
      return;
    }

    // Calculations
    const raw = coins * 0.10;
    const tax = raw * 0.30;
    const net = raw - tax;

    // Deduct
    this.virtualCoins -= coins;
    if (this.currentUser) {
      localStorage.setItem(`coins_${this.currentUser.email}`, this.virtualCoins);
      
      // Submit payout log to Firebase strictly for gamification (Safe metrics logging)
      if (window.firebaseManager.isOnline && window.firebaseManager.db) {
        const transRef = window.firebaseManager.db.ref(`withdrawals/${this.currentUser.userId}`).push();
        transRef.set({
          email: this.currentUser.email,
          method,
          address,
          coins,
          rawEarned: raw,
          taxDeducted: tax,
          netYield: net,
          timestamp: firebase.database.ServerValue.TIMESTAMP
        });
      }
    }

    alert(`EXCHANGE_ACCEPTED: Simulated request submitted successfully! \nNet Yield of ₹${net.toFixed(2)} is pending dispatch processing.`);
    this.showDashboard();
  }

  // ----------------------------------------------------
  // Admin Dashboard metrics
  // ----------------------------------------------------
  renderAdminMetrics() {
    document.getElementById("admin-total-impressions").innerText = 
      this.adImpressions.banner + this.adImpressions.interstitial + this.adImpressions.rewarded;
    
    // Revenue calculations based on CPM Math
    const rRev = (this.adImpressions.rewarded / 1000) * window.CONFIG.ecpm.rewarded;
    const iRev = (this.adImpressions.interstitial / 1000) * window.CONFIG.ecpm.interstitial;
    const bRev = (this.adImpressions.banner / 1000) * window.CONFIG.ecpm.banner;
    const tot = rRev + iRev + bRev;
    
    document.getElementById("admin-total-revenue").innerText = `₹${tot.toFixed(4)}`;
    document.getElementById("admin-active-caps").innerText = this.coinEarningHistory.length;
    document.getElementById("admin-set-coins").value = this.virtualCoins;
  }

  exportDiagnosticLogs() {
    const logData = {
      user: this.currentUser,
      stats: {
        coins: this.virtualCoins,
        score: this.totalScore
      },
      adMetrics: this.adImpressions,
      cappingWindow: this.coinEarningHistory,
      timestamp: new Date().toISOString(),
      integrityCheck: "SECURE v1.4.0"
    };

    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(logData, null, 2));
    const dlAnchorElem = document.createElement('a');
    dlAnchorElem.setAttribute("href", dataStr);
    dlAnchorElem.setAttribute("download", `firing_for_cash_diagnostic_${this.currentUser ? this.currentUser.userId : 'guest'}.json`);
    dlAnchorElem.click();
    console.log("Diagnostic logs exported successfully.");
  }
}

// Global Launcher
document.addEventListener("DOMContentLoaded", () => {
  window.gameApp = new GameApp();
});
