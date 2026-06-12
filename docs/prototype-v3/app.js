(() => {
  'use strict';

  // ---- i18n ----
  const STR = {
    en: { am: 'AM', pm: 'PM', caption: 'Bluey-themed · golden-ratio layout · multi-orientation' },
    zh: { am: '上午', pm: '下午', caption: 'Bluey 配色 · 黄金比例布局 · 多方向适配' },
  };

  // ---- State (persisted) ----
  const state = {
    format: localStorage.getItem('cw.format') || '12',          // '12' | '24'
    lang:   localStorage.getItem('cw.lang')   || 'en',          // 'en' | 'zh'
    device: localStorage.getItem('cw.device') || 'phone-portrait',
  };

  // ---- DOM ----
  const el = {
    device:        document.querySelector('.device'),
    content:       document.querySelector('.content'),
    digitalTime:   document.getElementById('digitalTime'),
    digitalPeriod: document.getElementById('digitalPeriod'),
    hour:          document.getElementById('hourHand'),
    minute:        document.getElementById('minuteHand'),
    second:        document.getElementById('secondHand'),
    ticks:         document.getElementById('ticks'),
    numbers:       document.getElementById('numbers'),
    segments:      document.querySelectorAll('.segment'),
    devicePicker:  document.querySelectorAll('.dp-btn'),
    caption:       document.getElementById('caption'),
  };

  // ---- Clock face geometry (viewBox 220x220, center 110,110) ----
  const CX = 110, CY = 110;
  const TICK_OUTER = 100;

  function buildFace() {
    const svgNS = 'http://www.w3.org/2000/svg';

    // Ticks (60)
    const ticksFrag = document.createDocumentFragment();
    for (let i = 0; i < 60; i++) {
      const angle = (i * 6 - 90) * (Math.PI / 180);
      const isMajor = i % 5 === 0;
      const inner = isMajor ? 90 : 95;
      const x1 = CX + Math.cos(angle) * inner;
      const y1 = CY + Math.sin(angle) * inner;
      const x2 = CX + Math.cos(angle) * TICK_OUTER;
      const y2 = CY + Math.sin(angle) * TICK_OUTER;
      const line = document.createElementNS(svgNS, 'line');
      line.setAttribute('x1', x1.toFixed(2));
      line.setAttribute('y1', y1.toFixed(2));
      line.setAttribute('x2', x2.toFixed(2));
      line.setAttribute('y2', y2.toFixed(2));
      line.setAttribute('class', isMajor ? 'tick-major' : 'tick-minor');
      ticksFrag.appendChild(line);
    }
    el.ticks.appendChild(ticksFrag);

    // Numbers 1–12
    const NUMBER_R = 80;
    const numFrag = document.createDocumentFragment();
    for (let n = 1; n <= 12; n++) {
      const angle = (n * 30 - 90) * (Math.PI / 180);
      const x = CX + Math.cos(angle) * NUMBER_R;
      const y = CY + Math.sin(angle) * NUMBER_R;
      const text = document.createElementNS(svgNS, 'text');
      text.setAttribute('x', x.toFixed(2));
      text.setAttribute('y', y.toFixed(2));
      text.textContent = String(n);
      numFrag.appendChild(text);
    }
    el.numbers.appendChild(numFrag);
  }

  // ---- Tick loop ----
  let lastSecond = -1;
  function tick() {
    const now = new Date();
    const h = now.getHours(), m = now.getMinutes(), s = now.getSeconds();
    const ms = now.getMilliseconds();

    const secondAngle = (s + ms / 1000) * 6;
    const minuteAngle = (m + s / 60) * 6;
    const hourAngle = ((h % 12) + m / 60) * 30;

    el.hour.style.transform   = `rotate(${hourAngle}deg)`;
    el.minute.style.transform = `rotate(${minuteAngle}deg)`;
    el.second.style.transform = `rotate(${secondAngle}deg)`;

    if (s !== lastSecond) {
      lastSecond = s;
      renderDigital(h, m, s);
    }
    requestAnimationFrame(tick);
  }

  function renderDigital(h, m /*, s */) {
    const pad = (n) => String(n).padStart(2, '0');
    if (state.format === '24') {
      // 24h: always padded, no period
      el.digitalTime.textContent = `${pad(h)}:${pad(m)}`;
      el.digitalPeriod.textContent = '';
    } else {
      const isPm = h >= 12;
      const h12 = h % 12 === 0 ? 12 : h % 12;
      // Pad to 2 digits in 12h too: e.g. 06:30
      el.digitalTime.textContent = `${pad(h12)}:${pad(m)}`;
      el.digitalPeriod.textContent = isPm ? STR[state.lang].pm : STR[state.lang].am;
    }
  }

  // ---- Language ----
  function applyLang() {
    if (el.caption) el.caption.textContent = STR[state.lang].caption;
    const now = new Date();
    renderDigital(now.getHours(), now.getMinutes(), now.getSeconds());
  }

  // ---- Segmented buttons ----
  function refreshSegments() {
    el.segments.forEach((btn) => {
      const group = btn.dataset.group;
      const value = btn.dataset.value;
      const selected = state[group] === value;
      btn.classList.toggle('selected', selected);
      btn.setAttribute('aria-pressed', selected ? 'true' : 'false');
    });
  }

  function wireSegments() {
    el.segments.forEach((btn) => {
      btn.addEventListener('click', () => {
        const group = btn.dataset.group;
        const value = btn.dataset.value;
        if (state[group] === value) return;
        state[group] = value;
        localStorage.setItem(`cw.${group}`, value);
        refreshSegments();
        if (group === 'lang') applyLang();
        if (group === 'format') {
          const now = new Date();
          renderDigital(now.getHours(), now.getMinutes(), now.getSeconds());
        }
      });
    });
  }

  // ---- Device picker (phone-portrait | phone-landscape | tablet-portrait | tablet-landscape) ----
  const DEVICES = ['phone-portrait', 'phone-landscape', 'tablet-portrait', 'tablet-landscape'];
  function applyDevice() {
    DEVICES.forEach(d => el.device.classList.remove(d));
    el.device.classList.add(state.device);
    el.devicePicker.forEach(btn => {
      btn.classList.toggle('selected', btn.dataset.device === state.device);
    });
    // Re-position clock to keep golden ratio in portrait modes
    requestAnimationFrame(positionClock);
  }
  function wireDevicePicker() {
    el.devicePicker.forEach(btn => {
      btn.addEventListener('click', () => {
        const d = btn.dataset.device;
        if (state.device === d) return;
        state.device = d;
        localStorage.setItem('cw.device', d);
        applyDevice();
      });
    });
  }

  // ---- Golden-ratio clock positioning (portrait only) ----
  // Goal: in portrait modes, clock CENTER should sit at ~38.2% of the SCREEN height
  // (measured from the screen top, including status bar). We achieve this by
  // translating the clock-region within the flex column.
  const PHI = 0.382;
  function positionClock() {
    const isPortrait = state.device === 'phone-portrait' || state.device === 'tablet-portrait';
    const clockRegion = document.querySelector('.clock-region');
    const screen = document.querySelector('.screen');
    if (!clockRegion || !screen) return;

    if (!isPortrait) {
      clockRegion.style.marginTop = '';
      return;
    }
    // After natural layout, measure where clock center currently is relative to screen top.
    clockRegion.style.marginTop = '';
    const sRect = screen.getBoundingClientRect();
    const cRect = clockRegion.getBoundingClientRect();
    const currentCenter = (cRect.top + cRect.height / 2) - sRect.top;
    const targetCenter = sRect.height * PHI;
    const delta = targetCenter - currentCenter;
    if (Math.abs(delta) > 1) {
      clockRegion.style.marginTop = `${delta.toFixed(1)}px`;
    }
  }

  window.addEventListener('resize', () => {
    requestAnimationFrame(positionClock);
  });

  // ---- Init ----
  buildFace();
  wireSegments();
  refreshSegments();
  wireDevicePicker();
  applyDevice();
  applyLang();
  requestAnimationFrame(tick);
  // initial positioning after fonts have a chance to load
  setTimeout(positionClock, 80);
  setTimeout(positionClock, 400);
})();
