// ════════════════════════════════════
// ECLIPSE — storage.js v5
// ════════════════════════════════════
const Store = {

  getSettings()         { try { return JSON.parse(localStorage.getItem('eclipse_settings') || '{}'); } catch { return {}; } },
  saveSetting(key, val) { try { const s=this.getSettings(); s[key]=val; localStorage.setItem('eclipse_settings', JSON.stringify(s)); } catch(e) {} },
  getSetting(key, def)  { const s=this.getSettings(); return s[key]!==undefined ? s[key] : def; },

  // History with individual-delete support
  addHistory(url, title) {
    try {
      if (!url || url.startsWith('file://')) return;
      const h = this.getHistory().filter(i => i.url !== url);
      h.unshift({ url, title: title || url, time: Date.now() });
      localStorage.setItem('eclipse_history', JSON.stringify(h.slice(0, 150)));
    } catch(e) {}
  },
  getHistory() {
    try { return JSON.parse(localStorage.getItem('eclipse_history') || '[]'); } catch { return []; }
  },
  clearHistory() { localStorage.removeItem('eclipse_history'); },
  deleteHistoryItem(idx) {
    try {
      const h = this.getHistory();
      h.splice(idx, 1);
      localStorage.setItem('eclipse_history', JSON.stringify(h));
    } catch(e) {}
  },

  // Bookmarks
  addBookmark(url, title, icon) {
    try {
      const b = this.getBookmarks();
      if (b.find(i => i.url === url)) return false;
      b.push({ url, title: title || url, icon: icon || '🔖', time: Date.now() });
      localStorage.setItem('eclipse_bookmarks', JSON.stringify(b));
      return true;
    } catch(e) { return false; }
  },
  getBookmarks() {
    try { return JSON.parse(localStorage.getItem('eclipse_bookmarks') || '[]'); } catch { return []; }
  },
  removeBookmark(url) {
    try {
      const b = this.getBookmarks().filter(i => i.url !== url);
      localStorage.setItem('eclipse_bookmarks', JSON.stringify(b));
    } catch(e) {}
  },

  // Ad stats (reset per session)
  getAdStats() {
    try {
      if (!sessionStorage.getItem('ecl_session')) {
        sessionStorage.setItem('ecl_session', '1');
        localStorage.setItem('eclipse_adstats', JSON.stringify({ads:0,trackers:0}));
      }
      return JSON.parse(localStorage.getItem('eclipse_adstats') || '{"ads":0,"trackers":0}');
    } catch { return {ads:0, trackers:0}; }
  },
  updateAdStats(ads, trackers) {
    try {
      const s = this.getAdStats();
      s.ads += ads; s.trackers += trackers;
      localStorage.setItem('eclipse_adstats', JSON.stringify(s));
    } catch(e) {}
  },

  // Quick sites
  getQuickSites() {
    try {
      const saved = JSON.parse(localStorage.getItem('eclipse_sites') || 'null');
      if (saved) return saved;
      return [
        { url:'https://www.google.com',    label:'Google'    },
        { url:'https://www.youtube.com',   label:'YouTube'   },
        { url:'https://twitter.com',       label:'Twitter'   },
        { url:'https://www.reddit.com',    label:'Reddit'    },
        { url:'https://github.com',        label:'GitHub'    },
        { url:'https://www.instagram.com', label:'Instagram' },
        { url:'https://www.wikipedia.org', label:'Wikipedia' },
      ];
    } catch { return []; }
  },
  saveQuickSites(s) { try { localStorage.setItem('eclipse_sites', JSON.stringify(s)); } catch(e) {} },

  // Music playlist
  getSavedPlaylist() { try { return JSON.parse(localStorage.getItem('eclipse_playlist') || '[]'); } catch { return []; } },
  savePlaylist(pl)   { try { localStorage.setItem('eclipse_playlist', JSON.stringify(pl)); } catch(e) {} },

  // Tabs (session)
  getTabs() {
    try { return JSON.parse(sessionStorage.getItem('eclipse_tabs') || 'null') || [{id:1,url:'home',title:'New Tab'}]; }
    catch { return [{id:1,url:'home',title:'New Tab'}]; }
  },
  saveTabs(tabs) { try { sessionStorage.setItem('eclipse_tabs', JSON.stringify(tabs)); } catch(e) {} }
};
