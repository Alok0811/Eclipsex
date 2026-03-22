// ════════════════════════════════════
// ECLIPSE — music.js  (v4 — HTML5 Audio, SomaFM Radio)
// ════════════════════════════════════

const Music = {
  audio: null,
  isOpen: false,
  isPlaying: false,
  currentIndex: 0,
  loadingTimer: null,

  // SomaFM internet radio stations — reliable, free, no API key
  playlist: [
    { id: 'groovesalad', name: 'Groove Salad',    artist: 'SomaFM · Ambient',   url: 'https://ice1.somafm.com/groovesalad-128-mp3',    art: '🌙', live: true },
    { id: 'lofi',        name: 'Lofi Café',        artist: 'SomaFM · Lofi',      url: 'https://ice1.somafm.com/lofi-128-mp3',           art: '☕', live: true },
    { id: 'dronezone',   name: 'Drone Zone',       artist: 'SomaFM · Deep',      url: 'https://ice1.somafm.com/dronezone-128-mp3',      art: '🎵', live: true },
    { id: 'mellow',      name: 'Mellow Vibes',     artist: 'SomaFM · Chill',     url: 'https://ice1.somafm.com/mellow-128-mp3',         art: '🌿', live: true },
    { id: 'deepspace',   name: 'Deep Space One',   artist: 'SomaFM · Space',     url: 'https://ice1.somafm.com/deepspaceone-128-mp3',   art: '🚀', live: true },
    { id: 'cliqhop',     name: 'Cliqhop IDM',      artist: 'SomaFM · Electronic',url: 'https://ice1.somafm.com/cliqhop-128-mp3',        art: '🎮', live: true },
    { id: 'seventies',   name: '70s Lounge',       artist: 'SomaFM · Retro',     url: 'https://ice1.somafm.com/seventies-128-mp3',      art: '🕺', live: true },
  ],

  init() {
    this.audio = new Audio();
    this.audio.crossOrigin = 'anonymous';
    this.audio.preload = 'none';

    this.audio.addEventListener('play', () => {
      this.isPlaying = true;
      this.updatePlayBtn();
      this._startProgressAnim();
    });
    this.audio.addEventListener('pause', () => {
      this.isPlaying = false;
      this.updatePlayBtn();
    });
    this.audio.addEventListener('ended', () => { this.next(); });
    this.audio.addEventListener('error', (e) => {
      console.log('Audio error, trying next:', e);
      this.isPlaying = false;
      this.updatePlayBtn();
      // Try next station after a brief delay
      setTimeout(() => this.next(), 1500);
    });
    this.audio.addEventListener('stalled', () => {
      // Connection stalled — retry
      if (this.isPlaying) {
        const src = this.audio.src;
        this.audio.src = '';
        setTimeout(() => { this.audio.src = src; this.audio.play().catch(()=>{}); }, 1000);
      }
    });
    this.audio.addEventListener('waiting', () => {
      const te = document.getElementById('mpTotTime');
      if (te) te.textContent = 'Loading...';
    });

    this.buildPlaylistUI();
    this.updateTrackUI();
    this._updateLiveBadge();
  },

  open() {
    this.isOpen = true;
    const p = document.getElementById('musicPanel');
    if (p) p.classList.add('open');
  },
  close() {
    this.isOpen = false;
    const p = document.getElementById('musicPanel');
    if (p) p.classList.remove('open');
  },
  toggle() { this.isOpen ? this.close() : this.open(); },

  play() {
    if (!this.audio) return;
    const track = this.playlist[this.currentIndex];
    if (!track) return;

    // Set source if not already set or changed
    if (!this.audio.src || !this.audio.src.includes(track.id)) {
      this.audio.src = track.url;
    }
    this.audio.play().then(() => {
      this.isPlaying = true;
      this.updatePlayBtn();
    }).catch(e => {
      console.log('Play failed:', e);
      this.isPlaying = false;
      this.updatePlayBtn();
      showToast('⚠ Could not play — check internet');
    });
  },

  pause() {
    if (!this.audio) return;
    this.audio.pause();
    this.isPlaying = false;
    this.updatePlayBtn();
  },

  togglePlay() { this.isPlaying ? this.pause() : this.play(); },

  next() {
    this.currentIndex = (this.currentIndex + 1) % this.playlist.length;
    this.loadAndPlay();
  },

  prev() {
    this.currentIndex = (this.currentIndex - 1 + this.playlist.length) % this.playlist.length;
    this.loadAndPlay();
  },

  selectTrack(index) {
    this.currentIndex = index;
    this.loadAndPlay();
  },

  loadAndPlay() {
    const track = this.playlist[this.currentIndex];
    if (!track || !this.audio) return;
    this.audio.pause();
    this.audio.src = track.url;
    this.audio.load();
    this.audio.play().then(() => {
      this.isPlaying = true;
      this.updatePlayBtn();
    }).catch(e => {
      console.log('Load failed:', e);
      showToast('⚠ Station unavailable, trying next...');
      setTimeout(() => this.next(), 1000);
    });
    this.updateTrackUI();
    this.buildPlaylistUI();
    this._updateLiveBadge();
  },

  updatePlayBtn() {
    const ico = document.getElementById('mpPlayIcon');
    const bubble = document.getElementById('bubbleBtn');
    if (this.isPlaying) {
      if (ico) ico.innerHTML = '<rect x="6" y="4" width="4" height="16" fill="currentColor"/><rect x="14" y="4" width="4" height="16" fill="currentColor"/>';
      if (bubble) bubble.classList.add('playing');
    } else {
      if (ico) ico.innerHTML = '<polygon points="5 3 19 12 5 21 5 3" fill="currentColor"/>';
      if (bubble) bubble.classList.remove('playing');
    }
  },

  updateTrackUI() {
    const track = this.playlist[this.currentIndex];
    if (!track) return;
    const ne = document.getElementById('mpTrack');
    const ae = document.getElementById('mpArtist');
    const arte = document.getElementById('mpAlbumArt');
    if (ne) ne.textContent = track.name;
    if (ae) ae.textContent = track.artist;
    if (arte) arte.textContent = track.art;
  },

  _updateLiveBadge() {
    const track = this.playlist[this.currentIndex];
    const liveRow = document.querySelector('.mp-live-row');
    if (liveRow) liveRow.style.display = track?.live ? 'flex' : 'none';
    const curEl = document.getElementById('mpCurTime');
    const totEl = document.getElementById('mpTotTime');
    if (track?.live) {
      if (curEl) curEl.textContent = '● LIVE';
      if (totEl) totEl.textContent = '∞';
    }
  },

  _startProgressAnim() {
    // For live radio no real progress bar, just animate gently
    const fill = document.getElementById('mpFill');
    if (!fill) return;
    if (this.playlist[this.currentIndex]?.live) {
      // Subtle breathing animation for live
      fill.style.width = '100%';
      fill.style.opacity = '0.4';
      fill.style.animation = 'adPulse 2s ease-in-out infinite';
    } else {
      fill.style.animation = '';
      fill.style.opacity = '1';
      // Normal progress tracking
      const tick = () => {
        if (!this.audio || !this.isPlaying) return;
        const cur = this.audio.currentTime;
        const dur = this.audio.duration;
        if (isFinite(dur) && dur > 0) {
          fill.style.width = (cur / dur * 100) + '%';
          const fmt = s => `${Math.floor(s/60)}:${String(Math.floor(s%60)).padStart(2,'0')}`;
          const ce = document.getElementById('mpCurTime');
          const te = document.getElementById('mpTotTime');
          if (ce) ce.textContent = fmt(cur);
          if (te) te.textContent = fmt(dur);
        }
        setTimeout(tick, 1000);
      };
      tick();
    }
  },

  buildPlaylistUI() {
    const pl = document.getElementById('mpPlaylistEl');
    if (!pl) return;
    pl.innerHTML = '';
    this.playlist.forEach((track, i) => {
      const item = document.createElement('div');
      item.className = 'mp-pl-item' + (i === this.currentIndex ? ' active' : '');
      item.innerHTML = `
        <div class="mp-pl-num">${i === this.currentIndex ? '▶' : i + 1}</div>
        <div class="mp-pl-info">
          <div class="mp-pl-name">${track.name}</div>
          <div class="mp-pl-artist">${track.artist}</div>
        </div>
        <div class="mp-pl-art">${track.art}</div>`;
      item.addEventListener('click', () => this.selectTrack(i));
      pl.appendChild(item);
    });
  }
};
