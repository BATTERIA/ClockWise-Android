(() => {
  'use strict';

  // ---- i18n ----
  const STR = {
    en: { title: 'ClockWise', am: 'AM', pm: 'PM' },
    zh: { title: '学时钟', am: '上午', pm: '下午' },
  };

  // ---- State (persisted) ----
  const state = {
    format: localStorage.getItem('cw.format') || '12', // '12' | '24'
    lang: localStorage.getItem('cw.lang') || 'en',     // 'en' | 'zh'
  };

  // ---- DOM ----
  const el = {
    title: document.getElementById('appTitle'),
    digitalTime: document.getElementById('digitalTime'),
    digitalPeriod: document.getElementById('digitalPeriod'),
    hour: document.getElementById('hourHand'),
    minute: document.getElementById('minuteHand'),
    second: document.getElementById('secondHand'),
    ticks: document.getElementById('ticks'),
    numbers: document.getElementById('numbers'),
    segments: document.querySelectorAll('.segment'),
  };

  // ---- Build clock face (ticks + numbers) ----
  const CX = 100, CY = 100;
  const TICK_OUTER = 92;
  const NUMBER_R = 76;

  function buildFace() {
    const svgNS = 'http://www.w3.org/2000/svg';
    const ticksFrag = document.createDocumentFragment();
    for (let i = 0; i < 60; i++) {
      const angle = (i * 6 - 90) * (Math.PI / 180);
      const isMajor = i % 5 === 0;
      const inner = isMajor ? 84 : 88;
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

  // ---- Clock tick ----
  let lastSecond = -1;

  function tick() {
    const now = new Date();
    const h = now.getHours();
    const m = now.getMinutes();
    const s = now.getSeconds();
    const ms = now.getMilliseconds();

    // Smooth angles
    const secondAngle = (s + ms / 1000) * 6;
    const minuteAngle = (m + s / 60) * 6;
    const hourAngle = ((h % 12) + m / 60) * 30;

    el.hour.style.transform   = `rotate(${hourAngle}deg)`;
    el.minute.style.transform = `rotate(${minuteAngle}deg)`;
    el.second.style.transform = `rotate(${secondAngle}deg)`;

    // Update digital only when second changes (cheap)
    if (s !== lastSecond) {
      lastSecond = s;
      renderDigital(h, m, s);
    }

    requestAnimationFrame(tick);
  }

  function renderDigital(h, m, s) {
    const pad = (n) => String(n).padStart(2, '0');
    if (state.format === '24') {
      el.digitalTime.textContent = `${pad(h)}:${pad(m)}`;
      el.digitalPeriod.textContent = '';
    } else {
      const isPm = h >= 12;
      const h12 = h % 12 === 0 ? 12 : h % 12;
      el.digitalTime.textContent = `${h12}:${pad(m)}`;
      const period = isPm ? STR[state.lang].pm : STR[state.lang].am;
      el.digitalPeriod.textContent = period;
    }
  }

  // ---- Language ----
  function applyLang() {
    el.title.textContent = STR[state.lang].title;
    // Refresh digital so AM/PM matches
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

  // ---- Init ----
  buildFace();
  wireSegments();
  refreshSegments();
  applyLang();
  requestAnimationFrame(tick);
})();
