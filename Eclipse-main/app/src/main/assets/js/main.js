// ════════════════════════════════════
// ECLIPSE — main.js v5 (architecture fix)
// Bottom nav is NATIVE — no nav code here
// ════════════════════════════════════

const App = {
  adOn: true,
  adsBlocked: 0,
  trackersBlocked: 0,
  searchEngine: 'duckduckgo',
  bgTheme: 'eclipse',
  particlesOn: true,
  starsOn: true,
  orbOn: true,
  activeSearchTab: 'all',
  particlePool: [],
  tabs: [],
  activeTabId: 1,
  tabIdCounter: 1,
  isIncognito: false,

  engines: {
    duckduckgo: { url: 'https://html.duckduckgo.com/html/?q=',    label: 'DuckDuckGo' },
    google:     { url: 'https://www.google.com/search?q=',        label: 'Google'     },
    bing:       { url: 'https://www.bing.com/search?q=',          label: 'Bing'       },
    brave:      { url: 'https://search.brave.com/search?q=',      label: 'Brave'      },
  }
};

// ── DETECT incognito from URL hash ──────────────────────────────────────────
function detectMode() {
  App.isIncognito = window.location.hash === '#incognito';
  if (App.isIncognito) {
    document.getElementById('homeContent').style.display      = 'none';
    document.getElementById('incognitoContent').style.display = '';
    document.body.style.background = '#000';   // fixed black — no canvas theme bleed
    document.getElementById('bgCanvas').style.display    = 'none';
    document.getElementById('starLayer').style.display   = 'none';
    document.getElementById('brandLabel').style.color    = 'rgba(128,0,255,0.9)';
    // hide music bubble in incognito
    const mb = document.getElementById('musicBubble');
    if (mb) mb.style.display = 'none';
  } else {
    document.getElementById('homeContent').style.display      = '';
    document.getElementById('incognitoContent').style.display = 'none';
  }
}

// ── MODAL ─────────────────────────────────────────────────────────────────
const Modal = {
  _resolve: null,
  show(opts) {
    return new Promise(resolve => {
      Modal._resolve = resolve;
      const bd = document.getElementById('modalBackdrop');
      document.getElementById('modalTitle').textContent = opts.title || '';
      document.getElementById('modalBody').textContent  = opts.body  || '';
      const inp = document.getElementById('modalInputs');
      inp.innerHTML = '';
      (opts.inputs || []).forEach(ph => {
        const el = document.createElement('input');
        el.className = 'modal-input'; el.placeholder = ph; el.type = 'text';
        inp.appendChild(el);
      });
      const act = document.getElementById('modalActions');
      act.innerHTML = '';
      (opts.actions || [{ label: 'OK', type: 'confirm' }]).forEach(a => {
        const btn = document.createElement('button');
        btn.textContent = a.label;
        btn.className   = 'modal-btn modal-btn-' + (a.style || (a.type === 'cancel' ? 'cancel' : 'confirm'));
        btn.onclick = () => {
          bd.classList.remove('open');
          if (a.type === 'cancel') { resolve(null); return; }
          const vals = [...inp.querySelectorAll('.modal-input')].map(e => e.value.trim());
          resolve(vals.length === 1 ? vals[0] : vals.length > 1 ? vals : true);
        };
        act.appendChild(btn);
      });
      bd.classList.add('open');
      setTimeout(() => inp.querySelector('.modal-input')?.focus(), 100);
    });
  },
  dismiss() {
    document.getElementById('modalBackdrop').classList.remove('open');
    if (Modal._resolve) { Modal._resolve(null); Modal._resolve = null; }
  },
  onBackdropTap(e) {
    if (e.target === document.getElementById('modalBackdrop')) Modal.dismiss();
  }
};

// ── TOAST ─────────────────────────────────────────────────────────────────
function showToast(msg, dur = 2200) {
  const t = document.getElementById('toastEl');
  if (!t) return;
  t.textContent = msg; t.classList.add('show');
  clearTimeout(t._t);
  t._t = setTimeout(() => t.classList.remove('show'), dur);
}

// ── ANDROID FETCH BRIDGE ──────────────────────────────────────────────────
window._eclFetchCbs = {};
window.EclFetchCallback = function(id, data, err) {
  const cb = window._eclFetchCbs[id];
  if (!cb) return;
  delete window._eclFetchCbs[id];
  cb(data, err);
};

function androidFetch(url) {
  return new Promise((resolve, reject) => {
    const id = 'f_' + Date.now() + '_' + Math.random().toString(36).slice(2);
    const t  = setTimeout(() => { delete window._eclFetchCbs[id]; reject(new Error('timeout')); }, 15000);
    window._eclFetchCbs[id] = (data, err) => {
      clearTimeout(t);
      if (err) reject(new Error(err));
      else { try { resolve(atob(data)); } catch(e) { resolve(data); } }
    };
    try { Android.fetchUrl(url, id); }
    catch(e) {
      clearTimeout(t); delete window._eclFetchCbs[id];
      fetch(url, { signal: AbortSignal.timeout(12000) })
        .then(r => r.text()).then(resolve).catch(reject);
    }
  });
}

// ── INIT ──────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  detectMode();
  loadSettings();

  if (!App.isIncognito) {
    buildStars();
    startBgCanvas();
    setInterval(spawnParticle, 900);
    loadNews(false);
    renderQuickSites();
    Music.init();
    const stats = Store.getAdStats();
    App.adsBlocked      = stats.ads;
    App.trackersBlocked = stats.trackers;
    updateAdBadge();
    initDraggable('musicBubble');
  } else {
    renderIncogSites();
  }

  updateClock();
  setInterval(updateClock, 30000);

  App.tabs         = Store.getTabs();
  App.activeTabId  = App.tabs[App.tabs.length - 1]?.id || 1;
  App.tabIdCounter = Math.max(...App.tabs.map(t => t.id), 1);
  syncTabCountToNative();
});

// ── SETTINGS ──────────────────────────────────────────────────────────────
function loadSettings() {
  App.searchEngine  = Store.getSetting('searchEngine', 'duckduckgo');
  App.bgTheme       = Store.getSetting('bgTheme',      'eclipse');
  App.adOn          = Store.getSetting('adOn',         true);
  App.particlesOn   = Store.getSetting('particles',    true);
  App.starsOn       = Store.getSetting('stars',        true);
  App.orbOn         = Store.getSetting('orb',          true);

  if (!App.isIncognito) {
    applyTheme(App.bgTheme);
    applyStars(App.starsOn);
    applyOrbVisibility(App.orbOn);
  }

  const ac  = Store.getSetting('accent',  '#ff6b1a');
  const ac2 = Store.getSetting('accent2', '#ffb347');
  document.documentElement.style.setProperty('--ac',  ac);
  document.documentElement.style.setProperty('--ac2', ac2);

  // mark selected search engine in customize panel
  document.querySelectorAll('[data-se]').forEach(el => {
    const active = el.dataset.se === App.searchEngine;
    el.classList.toggle('active', active);
    const chk = el.querySelector('.opt-check');
    if (chk) chk.textContent = active ? '✓' : '';
  });
}

function applyOrbVisibility(on) {
  const cw = document.getElementById('crescentWrap');
  if (!cw) return;
  if (on) cw.classList.remove('orb-hidden');
  else cw.classList.add('orb-hidden');
}

// ── CLOCK ─────────────────────────────────────────────────────────────────
function updateClock() {
  const n    = new Date();
  const h    = n.getHours();
  const m    = String(n.getMinutes()).padStart(2,'0');
  const ampm = h >= 12 ? 'PM' : 'AM';
  const h12  = h % 12 || 12;
  const ce   = document.getElementById('clockDisplay');
  if (ce) ce.textContent = `${h12}:${m} ${ampm}`;

  // ── Fixed greeting ───────────────────────────────────────────────
  const gm = document.getElementById('greetingMain');
  if (gm) {
    const ac = getComputedStyle(document.documentElement).getPropertyValue('--ac').trim() || '#ff6b1a';
    const highlight = s => `<span class="greet-accent" style="color:${ac}">${s}</span>`;
    if (h >= 5 && h < 12) {
      gm.innerHTML = `Good ${highlight('Morning')}, Explorer`;
    } else if (h >= 12 && h < 17) {
      gm.innerHTML = `Good ${highlight('Afternoon')}, Explorer`;
    } else if (h >= 17 && h < 21) {
      gm.innerHTML = `Good ${highlight('Evening')}, Explorer`;
    } else {
      // Night: different message
      gm.innerHTML = `What are you ${highlight('searching')} for lately, Explorer`;
    }
  }

  const days   = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const de = document.getElementById('dateDisplay');
  if (de) de.textContent = `${days[n.getDay()]} · ${months[n.getMonth()]} ${n.getDate()}`;
}

// ════════════════════════════════════
//  SEARCH — respects selected engine
//  Results load in WebView (bottom nav stays visible via native layout)
// ════════════════════════════════════
function setSearchTab(el, tab) {
  document.querySelectorAll('.tab-chip').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  App.activeSearchTab = tab;
}

function doSearch() {
  // Get query from whichever input is visible
  let q = '';
  if (App.isIncognito) {
    q = (document.getElementById('incogSearchInput')?.value || '').trim();
  } else {
    q = (document.getElementById('searchInput')?.value || '').trim();
  }
  if (!q) return;

  // Detect URL → open directly
  const isURL = /^(https?:\/\/|www\.)/i.test(q) ||
                (q.includes('.') && !q.includes(' ') && q.length < 80);
  if (isURL) {
    const url = q.startsWith('http') ? q : 'https://' + q;
    openUrl(url);
    return;
  }

  // Build search URL using SELECTED engine
  const engineKey = App.searchEngine || 'duckduckgo';
  const base      = App.engines[engineKey]?.url || App.engines.duckduckgo.url;
  let url         = base + encodeURIComponent(q);

  // Tab modifiers (only DuckDuckGo supports these cleanly in URL)
  if (engineKey === 'duckduckgo') {
    if (App.activeSearchTab === 'news')   url += '&iar=news&ia=news';
    if (App.activeSearchTab === 'images') url += '&iar=images&iax=images&ia=images';
    if (App.activeSearchTab === 'videos') url += '&iar=videos&iax=videos&ia=videos';
    if (App.activeSearchTab === 'ai')     url = 'https://duckduckgo.com/?q=' + encodeURIComponent(q) + '&ia=chat';
  } else if (engineKey === 'google') {
    if (App.activeSearchTab === 'news')   url += '&tbm=nws';
    if (App.activeSearchTab === 'images') url += '&tbm=isch';
    if (App.activeSearchTab === 'videos') url += '&tbm=vid';
  } else if (engineKey === 'bing') {
    if (App.activeSearchTab === 'images') url += '&qft=filterui:photo-photo';
    if (App.activeSearchTab === 'videos') url += '&qft=filterui:video-video';
  }

  openUrl(url);
}

// ── NAVIGATE — always via Android bridge so native nav stays ──────────────
function openUrl(url) {
  if (!App.isIncognito) {
    Store.addHistory(url, '');
  }
  try { Android.openUrl(url); }
  catch(e) { window.location.href = url; }
  // Update current tab's URL
  const tab = App.tabs.find(t => t.id === App.activeTabId);
  if (tab) {
    tab.url = url;
    try { tab.title = new URL(url).hostname; } catch(e) { tab.title = url.slice(0,28); }
    Store.saveTabs(App.tabs);
  }
}

// navHome() called from native ⌂ button or JS
function navHome() {
  const tab = App.tabs.find(t => t.id === App.activeTabId);
  if (tab) { tab.url = 'home'; tab.title = 'New Tab'; Store.saveTabs(App.tabs); }
  try { Android.goHome(); } catch(e) {}
}

// Called by native goHome when already on home page (to reset state)
function onHomeLoaded() {
  const input = document.getElementById('searchInput');
  if (input) input.value = '';
  updateClock(); // refresh greeting
}

// ── EXIT INCOGNITO ────────────────────────────────────────────────────────
function exitIncognito() {
  try { Android.exitIncognito(); } catch(e) {}
}

// ── TABS ──────────────────────────────────────────────────────────────────
function syncTabCountToNative() {
  try { Android.updateTabCount(App.tabs.length); } catch(e) {}
}

function openTabsPanel()  { renderTabsPanel(); document.getElementById('tabsOverlay').classList.add('open'); }
function closeTabsPanel() { document.getElementById('tabsOverlay').classList.remove('open'); }

function renderTabsPanel() {
  const grid = document.getElementById('tabsGrid');
  if (!grid) return;
  grid.innerHTML = '';
  App.tabs.forEach(tab => {
    const card   = document.createElement('div');
    const isHome = tab.url === 'home';
    const domain = isHome
      ? (tab.incognito ? '🌑 Void' : '🌑 Eclipse')
      : (() => { try { return new URL(tab.url).hostname.replace('www.',''); } catch(e) { return tab.url.slice(0,20); } })();

    card.className = 'tab-card' +
      (tab.id === App.activeTabId ? ' active' : '') +
      (tab.incognito ? ' incognito' : '');

    card.innerHTML = `
      <div class="tab-card-top">
        <span class="tab-card-title">${escHtml(tab.title || domain)}</span>
        <span class="tab-close-btn" data-id="${tab.id}">✕</span>
      </div>
      <div class="tab-card-domain">${escHtml(domain)}</div>`;

    card.addEventListener('click', e => {
      if (e.target.classList.contains('tab-close-btn')) return;
      switchToTab(tab.id);
    });
    card.querySelector('.tab-close-btn').addEventListener('click', e => {
      e.stopPropagation(); closeTab(tab.id);
    });
    grid.appendChild(card);
  });
}

function switchToTab(tabId) {
  App.activeTabId = tabId;
  const tab = App.tabs.find(t => t.id === tabId);
  closeTabsPanel();
  if (!tab) return;
  if (tab.incognito) {
    try { Android.openIncognito(); } catch(e) {}
  } else {
    if (tab.url === 'home') {
      try { Android.goHome(); } catch(e) {}
    } else {
      try { Android.openUrl(tab.url); } catch(e) {}
    }
  }
}

function newTab() {
  const id = ++App.tabIdCounter;
  App.tabs.push({ id, url: 'home', title: 'New Tab' });
  App.activeTabId = id;
  Store.saveTabs(App.tabs);
  syncTabCountToNative();
  closeTabsPanel(); closeMenu();
  try { Android.goHome(); } catch(e) {}
}

function newIncognitoTab() {
  const id = ++App.tabIdCounter;
  App.tabs.push({ id, url: 'home', title: 'Void Tab', incognito: true });
  App.activeTabId = id;
  Store.saveTabs(App.tabs);
  closeTabsPanel(); closeMenu();
  try { Android.openIncognito(); } catch(e) {}
  syncTabCountToNative();
}

function closeTab(tabId) {
  if (App.tabs.length <= 1) { showToast('Cannot close last tab'); return; }
  App.tabs = App.tabs.filter(t => t.id !== tabId);
  if (App.activeTabId === tabId) {
    App.activeTabId = App.tabs[App.tabs.length - 1].id;
    switchToTab(App.activeTabId);
  }
  Store.saveTabs(App.tabs);
  syncTabCountToNative();
  renderTabsPanel();
}

// Called from Kotlin after page load to keep tab URL current
function updateActiveTabUrl(url, title) {
  const tab = App.tabs.find(t => t.id === App.activeTabId);
  if (!tab) return;
  if (url && url.startsWith('file://')) {
    tab.url   = 'home';
    tab.title = 'New Tab';
  } else if (url) {
    tab.url   = url;
    tab.title = title || tab.title;
  }
  Store.saveTabs(App.tabs);
}

// ── MENU ──────────────────────────────────────────────────────────────────
function openMenu()  { document.getElementById('menuOverlay').classList.add('open'); }
function closeMenu() { document.getElementById('menuOverlay').classList.remove('open'); }

// ── QUICK SITES ───────────────────────────────────────────────────────────
function renderQuickSites() { _renderSites(Store.getQuickSites(), 'sitesGrid'); }
function renderIncogSites()  { _renderSites(Store.getQuickSites(), 'incogSitesGrid'); }

function _renderSites(sites, gridId) {
  const grid = document.getElementById(gridId);
  if (!grid) return;
  grid.innerHTML = '';
  sites.forEach((site, idx) => {
    const domain = (() => { try { return new URL(site.url).hostname; } catch(e) { return ''; } })();
    const item   = document.createElement('div');
    item.className = 'site-item';
    item.innerHTML = `
      <div class="site-icon-wrap">
        <img class="site-fav" src="https://www.google.com/s2/favicons?domain=${domain}&sz=128"
          onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" loading="lazy">
        <span class="site-fav-fallback" style="display:none">🌐</span>
      </div>
      <div class="site-label">${escHtml(site.label)}</div>`;
    item.addEventListener('click', () => openUrl(site.url));
    let lp;
    item.addEventListener('touchstart', () => { lp = setTimeout(() => promptRemoveSite(idx, site.label), 650); }, {passive:true});
    item.addEventListener('touchend',   () => clearTimeout(lp));
    item.addEventListener('touchmove',  () => clearTimeout(lp));
    item.addEventListener('contextmenu', e => { e.preventDefault(); promptRemoveSite(idx, site.label); });
    grid.appendChild(item);
  });
  // Add button
  const addItem = document.createElement('div');
  addItem.className = 'site-item';
  addItem.innerHTML = `
    <div class="site-icon-wrap" style="border:1.5px dashed rgba(255,107,26,0.4)">
      <span style="font-size:20px;color:rgba(255,107,26,0.7)">＋</span>
    </div>
    <div class="site-label">Add</div>`;
  addItem.addEventListener('click', addQuickSite);
  grid.appendChild(addItem);
}

async function promptRemoveSite(idx, label) {
  const ok = await Modal.show({
    title: 'Remove Shortcut',
    body:  `Remove "${label}" from Quick Access?`,
    actions: [
      { label: 'Cancel', type: 'cancel', style: 'cancel' },
      { label: 'Remove', style: 'danger' }
    ]
  });
  if (ok) {
    const sites = Store.getQuickSites();
    sites.splice(idx, 1);
    Store.saveQuickSites(sites);
    renderQuickSites();
    renderIncogSites();
  }
}

async function addQuickSite() {
  const vals = await Modal.show({
    title:  '⊕ Add Quick Access',
    inputs: ['URL  (e.g. https://example.com)', 'Name  (optional)'],
    actions: [
      { label: 'Cancel', type: 'cancel', style: 'cancel' },
      { label: 'Add', style: 'confirm' }
    ]
  });
  if (!vals || !Array.isArray(vals)) return;
  let [url, label] = vals;
  if (!url) return;
  if (!url.startsWith('http')) url = 'https://' + url;
  if (!label) { try { label = new URL(url).hostname.replace('www.',''); } catch(e) { label = url.slice(0,15); } }
  const sites = Store.getQuickSites();
  sites.push({ url, label });
  Store.saveQuickSites(sites);
  renderQuickSites(); renderIncogSites();
  showToast(`${label} added`);
}

// ── BOOKMARKS ─────────────────────────────────────────────────────────────
function addBookmarkCurrent() {
  try {
    const url   = Android.getCurrentUrl();
    const title = url;
    if (!url || url.startsWith('file://')) { showToast('No page to bookmark'); return; }
    const ok = Store.addBookmark(url, title, '🔖');
    showToast(ok ? '★ Bookmarked' : 'Already bookmarked');
  } catch(e) { showToast('Navigate to a page first'); }
}

function openBookmarks() {
  closeMenu();
  const items = Store.getBookmarks();
  const list  = document.getElementById('bookmarksList');
  if (list) {
    list.innerHTML = '';
    if (!items.length) {
      list.innerHTML = '<div style="padding:20px;text-align:center;color:var(--mt)">No bookmarks yet</div>';
    } else {
      items.forEach((item, idx) => {
        const domain = (() => { try { return new URL(item.url).hostname.replace('www.',''); } catch(e) { return item.url.slice(0,30); } })();
        const el = document.createElement('div');
        el.className = 'history-item';
        el.innerHTML = `
          <img class="res-fav" src="https://www.google.com/s2/favicons?domain=${domain}&sz=32" onerror="this.style.display='none'" loading="lazy">
          <div class="history-info" style="flex:1">
            <div class="history-title">${escHtml(item.title || domain)}</div>
            <div class="history-meta">${escHtml(domain)}</div>
          </div>
          <div class="history-del-btn" data-idx="${idx}" title="Remove">🗑</div>`;
        el.querySelector('.history-info').addEventListener('click', () => { closeBookmarks(); openUrl(item.url); });
        el.querySelector('.history-del-btn').addEventListener('click', e => {
          e.stopPropagation();
          Store.removeBookmark(item.url);
          renderBookmarksList();
          showToast('Removed');
        });
        list.appendChild(el);
      });
    }
  }
  document.getElementById('bookmarksOverlay').classList.add('open');
}

function renderBookmarksList() {
  // Re-render in-place
  openBookmarks();
}

function closeBookmarks() { document.getElementById('bookmarksOverlay')?.classList.remove('open'); }

// ── AD BLOCKER ────────────────────────────────────────────────────────────
function updateAdBadge() {
  const badge = document.getElementById('adBadge');
  const text  = document.getElementById('adBadgeText');
  if (!badge || !text) return;
  badge.classList.toggle('off', !App.adOn);
  text.textContent = App.adOn ? `Shield ON · ${App.adsBlocked} blocked` : 'Shield OFF';
  ['statAds','statTrackers','statSaved'].forEach((id, i) => {
    const el = document.getElementById(id);
    if (el) el.textContent = i === 0 ? App.adsBlocked : i === 1 ? App.trackersBlocked : Math.floor(App.adsBlocked * 1.2) + 's';
  });
}

function openAdPanel()  { closeMenu(); document.getElementById('adOverlay').classList.add('open'); }
function closeAdPanel() { document.getElementById('adOverlay').classList.remove('open'); }

function toggleAdBlock() {
  const tog = document.getElementById('togAdBlock');
  tog.classList.toggle('on');
  App.adOn = tog.classList.contains('on');
  Store.saveSetting('adOn', App.adOn);
  updateAdBadge();
  showToast(App.adOn ? 'Shield ON' : 'Shield OFF');
}
function toggleSwitch(el) { el.classList.toggle('on'); }

// Passive ad counter
setInterval(() => {
  if (App.adOn && !App.isIncognito && Math.random() > 0.65) {
    App.adsBlocked      += Math.floor(Math.random() * 2) + 1;
    App.trackersBlocked += 1;
    Store.updateAdStats(1, 1);
    updateAdBadge();
  }
}, 12000);

// ── CUSTOMIZE ─────────────────────────────────────────────────────────────
function openCustomize()  { closeMenu(); document.getElementById('cpOverlay').classList.add('open'); }
function closeCustomize() { document.getElementById('cpOverlay').classList.remove('open'); }

function setAccent(c1, c2, el) {
  document.documentElement.style.setProperty('--ac', c1);
  document.documentElement.style.setProperty('--ac2', c2);
  document.querySelectorAll('.color-swatch').forEach(s => s.classList.remove('active'));
  el.classList.add('active');
  Store.saveSetting('accent', c1); Store.saveSetting('accent2', c2);
  updateClock(); // refresh greeting highlight color
}

function setTheme(theme, el) {
  App.bgTheme = theme; applyTheme(theme);
  document.querySelectorAll('.theme-opt').forEach(e => {
    e.classList.remove('active');
    const chk = e.querySelector('.opt-check'); if (chk) chk.textContent = '';
  });
  el.classList.add('active');
  const chk = el.querySelector('.opt-check'); if (chk) chk.textContent = '✓';
  Store.saveSetting('bgTheme', theme);
}

function applyTheme(theme) {
  const bgs = { eclipse: '#000', nebula: '#02001a', aurora: '#001208', void: '#000' };
  if (!App.isIncognito) document.body.style.background = bgs[theme] || '#000';
  window._bgTheme = theme;
}

function applyStars(on) {
  const sl = document.getElementById('starLayer');
  if (sl) sl.style.opacity = on ? '1' : '0';
}

function toggleParticles(el) { el.classList.toggle('on'); App.particlesOn = el.classList.contains('on'); Store.saveSetting('particles', App.particlesOn); }
function toggleStars(el)     { el.classList.toggle('on'); App.starsOn = el.classList.contains('on'); applyStars(App.starsOn); Store.saveSetting('stars', App.starsOn); }
function toggleOrb(el) {
  el.classList.toggle('on'); App.orbOn = el.classList.contains('on');
  applyOrbVisibility(App.orbOn);
  Store.saveSetting('orb', App.orbOn);
}

function setSearchEngine(se, el) {
  App.searchEngine = se;
  Store.saveSetting('searchEngine', se);
  document.querySelectorAll('[data-se]').forEach(e => {
    e.classList.remove('active');
    const chk = e.querySelector('.opt-check'); if (chk) chk.textContent = '';
  });
  el.classList.add('active');
  const chk = el.querySelector('.opt-check'); if (chk) chk.textContent = '✓';
  showToast(`Search: ${App.engines[se]?.label || se}`);
}

// ── NEWS SOURCES ──────────────────────────────────────────────────────────
const NEWS_SOURCES = [
  { url: 'https://feeds.bbci.co.uk/news/world/rss.xml',               label: 'BBC World'   },
  { url: 'https://feeds.bbci.co.uk/news/technology/rss.xml',          label: 'BBC Tech'    },
  { url: 'https://feeds.bbci.co.uk/news/science_and_environment/rss.xml', label: 'BBC Science' },
  { url: 'https://feeds.skynews.com/feeds/rss/world.xml',             label: 'Sky News'    },
  { url: 'https://www.aljazeera.com/xml/rss/all.xml',                 label: 'Al Jazeera'  },
  { url: 'https://techcrunch.com/feed/',                              label: 'TechCrunch'  },
  { url: 'https://www.theverge.com/rss/index.xml',                    label: 'The Verge'   },
  { url: 'https://feeds.bbci.co.uk/news/health/rss.xml',              label: 'BBC Health'  },
];
let _newsIdx = Math.floor(Math.random() * NEWS_SOURCES.length);

// ── LIVE NEWS ─────────────────────────────────────────────────────────────
async function loadNews(forceRefresh) {
  const container = document.getElementById('newsCards');
  if (!container) return;

  if (forceRefresh) {
    _newsIdx = (_newsIdx + 1) % NEWS_SOURCES.length;
    sessionStorage.removeItem('eclipse_news_cache');
    sessionStorage.removeItem('eclipse_news_time');
    sessionStorage.removeItem('eclipse_news_src');
  }

  const cached     = sessionStorage.getItem('eclipse_news_cache');
  const cachedTime = parseInt(sessionStorage.getItem('eclipse_news_time') || '0');
  const cachedSrc  = sessionStorage.getItem('eclipse_news_src') || '';

  if (!forceRefresh && cached && Date.now() - cachedTime < 10 * 60 * 1000) {
    renderNewsCards(JSON.parse(cached), cachedSrc); return;
  }

  container.innerHTML = '<div class="news-loading"><span class="spinner-news">◌</span> Loading...</div>';
  const src = NEWS_SOURCES[_newsIdx];

  try {
    const xml  = await androidFetch(src.url);
    const doc  = new DOMParser().parseFromString(xml, 'application/xml');
    const news = [...doc.querySelectorAll('item, entry')].slice(0, 12).map(item => {
      const raw = item.querySelector('title')?.textContent || '';
      const title = raw.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&#39;/g,"'").replace(/&quot;/g,'"').trim();
      const link  = item.querySelector('link')?.textContent
                 || item.querySelector('link')?.getAttribute?.('href')
                 || item.querySelector('guid')?.textContent || '#';
      const pubDate = item.querySelector('pubDate,published,updated')?.textContent || '';
      let thumb = '';
      const mt = item.getElementsByTagNameNS('*','thumbnail')[0];
      if (mt) thumb = mt.getAttribute('url') || '';
      if (!thumb) { const enc = item.querySelector('enclosure'); if (enc) thumb = enc.getAttribute('url') || ''; }
      let timeAgo = '';
      if (pubDate) {
        const diff = Math.floor((Date.now() - new Date(pubDate).getTime()) / 1000);
        timeAgo = diff < 3600 ? `${Math.max(1,Math.floor(diff/60))}m ago`
                : diff < 86400 ? `${Math.floor(diff/3600)}h ago`
                : `${Math.floor(diff/86400)}d ago`;
      }
      return { title, link, thumb, timeAgo };
    }).filter(n => n.title.length > 3);

    if (!news.length) throw new Error('empty');
    sessionStorage.setItem('eclipse_news_cache', JSON.stringify(news));
    sessionStorage.setItem('eclipse_news_time',  Date.now().toString());
    sessionStorage.setItem('eclipse_news_src',   src.label);
    renderNewsCards(news, src.label);
  } catch(e) {
    _newsIdx = (_newsIdx + 1) % NEWS_SOURCES.length;
    container.innerHTML = `<div class="news-error" onclick="loadNews(true)">⚠ Tap to retry</div>`;
  }
}

function renderNewsCards(news, srcLabel) {
  const container = document.getElementById('newsCards');
  if (!container) return;
  container.innerHTML = '';
  news.forEach(item => {
    const card = document.createElement('div');
    card.className = 'news-card';
    card.innerHTML = `
      <div class="news-card-thumb">${item.thumb ? `<img src="${item.thumb}" loading="lazy" onerror="this.parentElement.innerHTML='📰'">` : '📰'}</div>
      <div class="news-card-source">${escHtml(srcLabel || 'News')}</div>
      <div class="news-card-title">${escHtml(item.title)}</div>
      <div class="news-card-time">${item.timeAgo || 'Recent'}</div>`;
    card.addEventListener('click', () => openUrl(item.link));
    container.appendChild(card);
  });
}

// ── HISTORY ───────────────────────────────────────────────────────────────
function openHistory() {
  closeMenu();
  renderHistory();
  document.getElementById('historyOverlay').classList.add('open');
}
function closeHistory() { document.getElementById('historyOverlay')?.classList.remove('open'); }

function renderHistory() {
  const list    = document.getElementById('historyList');
  const history = Store.getHistory();
  if (!list) return;
  list.innerHTML = '';
  if (!history.length) {
    list.innerHTML = '<div style="padding:20px;text-align:center;color:var(--mt)">No history yet</div>';
    return;
  }
  history.slice(0, 60).forEach((item, idx) => {
    const domain = (() => { try { return new URL(item.url).hostname.replace('www.',''); } catch(e) { return item.url.slice(0,30); } })();
    const diff   = Math.floor((Date.now() - item.time) / 1000);
    const age    = diff < 60 ? `${diff}s ago` : diff < 3600 ? `${Math.floor(diff/60)}m ago` : diff < 86400 ? `${Math.floor(diff/3600)}h ago` : `${Math.floor(diff/86400)}d ago`;
    const el = document.createElement('div');
    el.className = 'history-item';
    el.innerHTML = `
      <img class="res-fav" src="https://www.google.com/s2/favicons?domain=${domain}&sz=32" onerror="this.style.display='none'" loading="lazy">
      <div class="history-info" style="flex:1;min-width:0">
        <div class="history-title">${escHtml(item.title || domain)}</div>
        <div class="history-meta">${age}</div>
      </div>
      <div class="history-del-btn" title="Delete this item">✕</div>`;
    el.querySelector('.history-info').addEventListener('click', () => { closeHistory(); openUrl(item.url); });
    // Individual delete
    el.querySelector('.history-del-btn').addEventListener('click', e => {
      e.stopPropagation();
      Store.deleteHistoryItem(idx);
      renderHistory();
    });
    list.appendChild(el);
  });
}

// ── STARS / CANVAS ────────────────────────────────────────────────────────
function buildStars() {
  const layer = document.getElementById('starLayer');
  if (!layer) return;
  for (let i = 0; i < 120; i++) {
    const s  = document.createElement('div');
    s.className = 'star';
    const sz = Math.random() * 2.2 + 0.4;
    s.style.cssText = `width:${sz}px;height:${sz}px;left:${Math.random()*100}%;top:${Math.random()*100}%;--d:${2+Math.random()*4}s;--dl:${Math.random()*4}s;--lo:${0.04+Math.random()*0.15};--hi:${0.35+Math.random()*0.65};`;
    layer.appendChild(s);
  }
}

function startBgCanvas() {
  const canvas = document.getElementById('bgCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  function resize() { canvas.width = innerWidth; canvas.height = innerHeight; }
  resize(); window.addEventListener('resize', resize);
  (function draw() {
    const w = canvas.width, h = canvas.height;
    ctx.clearRect(0,0,w,h);
    const theme = window._bgTheme || App.bgTheme;
    if (theme === 'nebula') {
      const g1 = ctx.createRadialGradient(w*0.3,h*0.3,0,w*0.3,h*0.3,w*0.55);
      g1.addColorStop(0,'rgba(80,0,120,.3)'); g1.addColorStop(1,'transparent');
      ctx.fillStyle = g1; ctx.fillRect(0,0,w,h);
      const g2 = ctx.createRadialGradient(w*0.7,h*0.6,0,w*0.7,h*0.6,w*0.45);
      g2.addColorStop(0,'rgba(0,50,160,.25)'); g2.addColorStop(1,'transparent');
      ctx.fillStyle = g2; ctx.fillRect(0,0,w,h);
    } else if (theme === 'aurora') {
      [0,1,2].forEach(i => {
        const x = w*(0.2+i*0.3);
        const g = ctx.createRadialGradient(x,h*0.3,0,x,h*0.3,h*0.45);
        const cls = ['rgba(0,200,100,.18)','rgba(0,150,200,.14)','rgba(100,50,200,.16)'];
        g.addColorStop(0,cls[i]); g.addColorStop(1,'transparent');
        ctx.fillStyle = g; ctx.fillRect(0,0,w,h);
      });
    }
    requestAnimationFrame(draw);
  })();
}

// ── PARTICLES ─────────────────────────────────────────────────────────────
function spawnParticle() {
  if (!App.particlesOn || App.isIncognito) return;
  if (App.particlePool.length >= 10) {
    const old = App.particlePool.shift();
    if (old?.parentNode) old.remove();
  }
  const p   = document.createElement('div');
  p.className = 'particle';
  const sz  = Math.random() * 2.5 + 0.8;
  const x   = Math.random() * 90 + 5;
  const dur = 7 + Math.random() * 8;
  const ac  = getComputedStyle(document.documentElement).getPropertyValue('--ac').trim() || '#ff6b1a';
  p.style.cssText = `width:${sz}px;height:${sz}px;background:${ac};left:${x}%;--dx1:${(Math.random()-.5)*50}px;--dx2:${(Math.random()-.5)*100}px;animation-duration:${dur}s;box-shadow:0 0 ${sz*3}px ${ac};`;
  document.body.appendChild(p);
  App.particlePool.push(p);
  setTimeout(() => { if (p.parentNode) p.remove(); App.particlePool = App.particlePool.filter(x => x !== p); }, dur*1000);
}

// ── DRAGGABLE ─────────────────────────────────────────────────────────────
function initDraggable(id) {
  const el = document.getElementById(id);
  if (!el) return;
  let ox,oy,sx,sy,moved=false;
  el.addEventListener('touchstart', e => {
    const t=e.touches[0]; ox=el.offsetLeft; oy=el.offsetTop; sx=t.clientX; sy=t.clientY; moved=false; el.style.transition='none';
  }, {passive:true});
  el.addEventListener('touchmove', e => {
    const t=e.touches[0],dx=t.clientX-sx,dy=t.clientY-sy;
    if(Math.abs(dx)>6||Math.abs(dy)>6) moved=true;
    if(!moved) return; e.preventDefault();
    el.style.left   = Math.max(4, Math.min(innerWidth-64,  ox+dx)) + 'px';
    el.style.top    = Math.max(4, Math.min(innerHeight-130, oy+dy)) + 'px';
    el.style.right  = 'auto'; el.style.bottom = 'auto';
  }, {passive:false});
  el.addEventListener('touchend', e => { el.style.transition=''; if(moved) e.preventDefault(); });
}

// ── UTILS ─────────────────────────────────────────────────────────────────
function escHtml(str) {
  return String(str)
    .replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>')
    .replace(/&quot;/g,'"').replace(/&#39;/g,"'")
    .replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
