const CONFIG = {
  // AdMob Credentials
  admob: {
    appId: "ca-app-pub-4720255455581147~7317442721",
    bannerId: "ca-app-pub-4720255455581147/9700219017",
    interstitialId: "ca-app-pub-4720255455581147/6968591382",
    rewardedId: "ca-app-pub-4720255455581147/8195565655"
  },

  // Indian Ad Network Estimated eCPM (in ₹ INR)
  ecpm: {
    rewarded: 70.0,      // per 1000 impressions (₹70)
    interstitial: 30.0,  // per 1000 impressions (₹30)
    banner: 10.0         // per 1000 impressions (₹10)
  },

  // Game Economics
  economics: {
    targetsPerCoin: 20, // 20 target destructions = 1 virtual coin
    maxRewardedPerSession: 4 // Up to 4 rewards to revive per session
  },

  // Anti-Bot Capping Protection
  security: {
    maxCoinsPerWindow: 20,       // Max 20 virtual coins
    timeWindowMs: 30 * 60 * 1000  // Per 30 minutes
  },

  // Firebase Credentials
  firebase: {
    apiKey: "AIzaSyDLcLS6yNIBCf2P3oxUIJ0XX6tBN8ZhC6k",
    appId: "1:845367218033:android:91bffd7f28bfc030c3b0d0",
    messagingSenderId: "845367218033",
    databaseURL: "https://firing-for-cash-default-rtdb.asia-southeast1.firebasedatabase.app"
  },

  // General app info
  pin: "1041"
};

// Make config globally available
window.CONFIG = CONFIG;
