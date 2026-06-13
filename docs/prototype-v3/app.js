(() => {
  'use strict';

  // ---- i18n ----
  const STR = {
    en: {
      am: 'AM',
      pm: 'PM',
      caption: 'Bluey-themed · golden-ratio layout · multi-orientation',
      showSecondsOn:  'Show s',
      showSecondsOff: 'Hide s',
      modeAuto:       'Auto',
      modeManual:     'Manual',
    },
    zh: {
      am: '上午',
      pm: '下午',
      caption: 'Bluey 配色 · 黄金比例布局 · 多方向适配',
      showSecondsOn:  '显示秒',
      showSecondsOff: '隐藏秒',
      modeAuto:       '自动',
      modeManual:     '手动',
    },
  };

  // ---- State ----
  // Persist user prefs except mode (mode is transient — Auto on each page load).
  const state = {
    format:      localStorage.getItem('cw.format') || '12',
    lang:        localStorage.getItem('cw.lang')   || 'en',
    device:      localStorage.getItem('cw.device') || 'phone-portrait',
    showSeconds: localStorage.getItem('cw.showSeconds') === '1' ? '1' : '0',
    mode:        'auto',
    // manualTotalSeconds in [0, 43200): 0 = 12:00:00 → 43199 = 11:59:59 (12h cycle).
    manualTotalSeconds: 0,
  };

  const CYCLE = 43200; // seconds in a 12-hour cycle

  // ---- DOM ----
  const el = {
    device:         document.querySelector('.device'),
    content:        document.querySelector('.content'),
    screen:         document.querySelector('.screen'),
    clockSvg:       document.querySelector('.clock'),
    digitalTime:    document.getElementById('digitalTime'),
    digitalPeriod:  document.getElementById('digitalPeriod'),
    hour:           document.getElementById('hourHand'),
    minute:         document.getElementById('minuteHand'),
    second:         document.getElementById('secondHand'),
    hourHit:        document.getElementById('hourHit'),
    minuteHit:      document.getElementById('minuteHit'),
    secondHit:      document.getElementById('secondHit'),
    // v3.6.5: manualBadge removed from the SVG — entry kept absent so JS doesn't reference it.
    ticks:          document.getElementById('ticks'),
    numbers:        document.getElementById('numbers'),
    segments:       document.querySelectorAll('.segment'),
    devicePicker:   document.querySelectorAll('.dp-btn'),
    caption:        document.getElementById('caption'),
    labelSecOn:     document.getElementById('labelSecOn'),
    labelSecOff:    document.getElementById('labelSecOff'),
    labelModeAuto:  document.getElementById('labelModeAuto'),
    labelModeManual:document.getElementById('labelModeManual'),
    speakBtn:       document.getElementById('speakBtn'),
  };

  // ---- Clock face geometry (viewBox 220x220, center 110,110) ----
  const CX = 110, CY = 110;
  const TICK_OUTER = 100;

  function buildFace() {
    const svgNS = 'http://www.w3.org/2000/svg';

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

  // ---- Render hands + digital from a "snapshot" {h,m,s,ms} or from manualTotalSeconds ----

  function renderFromHMS(h, m, s, ms) {
    const secondAngle = (s + ms / 1000) * 6;
    const minuteAngle = (m + s / 60) * 6;
    const hourAngle = ((h % 12) + m / 60) * 30;

    el.hour.style.transform   = `rotate(${hourAngle}deg)`;
    el.minute.style.transform = `rotate(${minuteAngle}deg)`;
    el.second.style.transform = `rotate(${secondAngle}deg)`;
    if (el.hourHit)   el.hourHit.style.transform   = `rotate(${hourAngle}deg)`;
    if (el.minuteHit) el.minuteHit.style.transform = `rotate(${minuteAngle}deg)`;
    if (el.secondHit) el.secondHit.style.transform = `rotate(${secondAngle}deg)`;

    renderDigital(h, m, s);
  }

  function renderFromManual() {
    const total = state.manualTotalSeconds;
    // hour hand: smooth across the 12-hour cycle
    const hourAngle   = (total / CYCLE) * 360;
    const minuteAngle = ((total % 3600) / 3600) * 360;
    const secondAngle = ((total % 60)   / 60)   * 360;

    el.hour.style.transform   = `rotate(${hourAngle}deg)`;
    el.minute.style.transform = `rotate(${minuteAngle}deg)`;
    el.second.style.transform = `rotate(${secondAngle}deg)`;
    if (el.hourHit)   el.hourHit.style.transform   = `rotate(${hourAngle}deg)`;
    if (el.minuteHit) el.minuteHit.style.transform = `rotate(${minuteAngle}deg)`;
    if (el.secondHit) el.secondHit.style.transform = `rotate(${secondAngle}deg)`;

    // Convert manualTotalSeconds back to h,m,s for the digital display.
    // We don't know AM/PM in manual mode, so just always show the 12h hour as 12/1..11,
    // and treat as "AM" (or empty in 24h) — actually for 24h we map the 12-hour cycle to
    // hour-of-day = h12 (no PM info). This keeps the digital readout faithful to the analog.
    let total24 = Math.floor(total) % CYCLE;
    if (total24 < 0) total24 += CYCLE;
    const h12 = Math.floor(total24 / 3600); // 0..11 (we treat 0 as 12 in 12h display)
    const m   = Math.floor((total24 % 3600) / 60);
    const s   = total24 % 60;
    renderDigital(h12, m, s, /* manualMode */ true);
  }

  function pad(n) { return String(n).padStart(2, '0'); }

  function renderDigital(h, m, s, manualMode) {
    const showS = state.showSeconds === '1';
    // Always keep textContent set (even when hidden) so the .hidden class
    // can animate without layout jumps.
    if (state.format === '24') {
      // In manual mode, h is 0..11 from manualTotalSeconds (no AM/PM available).
      // Show actual 0..11 (no 12-fudge) so 24h is visibly different from 12h.
      const hh = manualMode ? h : h;
      el.digitalTime.textContent = showS
        ? `${pad(hh)}:${pad(m)}:${pad(s)}`
        : `${pad(hh)}:${pad(m)}`;
      // Period: keep last text but hide via class for animation.
      if (!el.digitalPeriod.textContent) {
        el.digitalPeriod.textContent = STR[state.lang].am;
      }
      el.digitalPeriod.classList.add('hidden');
    } else {
      if (manualMode) {
        // 12h manual: show 12,1..11. AM/PM not knowable from the analog dial,
        // but we still show the AM badge so the 12h↔ 24h toggle is visibly
        // different (and the user can mentally interpret it as "AM-side of the dial").
        const h12 = h === 0 ? 12 : h;
        el.digitalTime.textContent = showS
          ? `${pad(h12)}:${pad(m)}:${pad(s)}`
          : `${pad(h12)}:${pad(m)}`;
        el.digitalPeriod.textContent = STR[state.lang].am;
        el.digitalPeriod.classList.remove('hidden');
      } else {
        const isPm = h >= 12;
        const h12 = h % 12 === 0 ? 12 : h % 12;
        el.digitalTime.textContent = showS
          ? `${pad(h12)}:${pad(m)}:${pad(s)}`
          : `${pad(h12)}:${pad(m)}`;
        el.digitalPeriod.textContent = isPm ? STR[state.lang].pm : STR[state.lang].am;
        el.digitalPeriod.classList.remove('hidden');
      }
    }
  }

  // ---- Auto tick loop ----
  let rafId = null;
  let lastSecond = -1;
  function tickAuto() {
    if (state.mode !== 'auto') { rafId = null; return; }
    const now = new Date();
    const h = now.getHours(), m = now.getMinutes(), s = now.getSeconds();
    const ms = now.getMilliseconds();
    renderFromHMS(h, m, s, ms);
    lastSecond = s;
    rafId = requestAnimationFrame(tickAuto);
  }
  function startAutoLoop() {
    if (rafId == null) {
      rafId = requestAnimationFrame(tickAuto);
    }
  }
  function stopAutoLoop() {
    if (rafId != null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
  }

  function rerenderDigitalNow() {
    if (state.mode === 'manual') {
      renderFromManual();
      return;
    }
    const now = new Date();
    lastSecond = -1;
    renderDigital(now.getHours(), now.getMinutes(), now.getSeconds());
  }

  // ---- Language ----
  function applyLang() {
    if (el.caption) el.caption.textContent = STR[state.lang].caption;
    if (el.labelSecOn)      el.labelSecOn.textContent      = STR[state.lang].showSecondsOn;
    if (el.labelSecOff)     el.labelSecOff.textContent     = STR[state.lang].showSecondsOff;
    if (el.labelModeAuto)   el.labelModeAuto.textContent   = STR[state.lang].modeAuto;
    if (el.labelModeManual) el.labelModeManual.textContent = STR[state.lang].modeManual;
    rerenderDigitalNow();
  }

  // ---- Segmented buttons (all four rows) ----
  function refreshSegments() {
    el.segments.forEach((btn) => {
      const group = btn.dataset.group;
      const value = btn.dataset.value;
      const selected = String(state[group]) === value;
      btn.classList.toggle('selected', selected);
      btn.setAttribute('aria-pressed', selected ? 'true' : 'false');
    });
  }

  function applyShowSeconds() {
    const hide = state.showSeconds !== '1';
    document.body.classList.toggle('hide-seconds', hide);
    // also flag on the device so styles cascade if needed
    if (el.device) el.device.classList.toggle('hide-seconds', hide);
  }

  function applyMode() {
    const isManual = state.mode === 'manual';
    document.body.classList.toggle('manual-mode', isManual);
    if (el.device) el.device.classList.toggle('manual-mode', isManual);
    // v3.6.5: removed the manualBadge show/hide — the segmented Mode toggle already conveys this.

    if (isManual) {
      // Seed manualTotalSeconds from current real time.
      const now = new Date();
      const h = now.getHours() % 12;
      const m = now.getMinutes();
      const s = now.getSeconds();
      state.manualTotalSeconds = h * 3600 + m * 60 + s;
      stopAutoLoop();
      renderFromManual();
    } else {
      startAutoLoop();
    }
  }

  function wireSegments() {
    el.segments.forEach((btn) => {
      btn.addEventListener('click', () => {
        const group = btn.dataset.group;
        const value = btn.dataset.value;
        if (String(state[group]) === value) return;
        state[group] = value;
        if (group !== 'mode') {
          localStorage.setItem(`cw.${group}`, value);
        }
        refreshSegments();
        if (group === 'lang') applyLang();
        if (group === 'format') rerenderDigitalNow();
        if (group === 'showSeconds') { applyShowSeconds(); rerenderDigitalNow(); }
        if (group === 'mode') applyMode();
      });
    });
  }

  // ---- Manual drag handling ----
  // Track previous pointer angle for delta-based update (handles wrap automatically).
  const drag = {
    active: false,
    hand: null,       // 'hour' | 'minute' | 'second'
    lastAngle: 0,
    pointerId: null,
  };

  // Compute angle in degrees (0=12 o'clock, clockwise positive) from pointer event.
  function angleFromPointer(ev) {
    const rect = el.clockSvg.getBoundingClientRect();
    // Center in CSS pixels (SVG viewBox center is 110,110 of a 220 viewBox)
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const dx = ev.clientX - cx;
    const dy = ev.clientY - cy;
    // atan2 gives angle from +X axis. We want 0 at 12 o'clock = -Y axis. So:
    let deg = Math.atan2(dy, dx) * 180 / Math.PI + 90;
    // Normalize to [0, 360)
    deg = ((deg % 360) + 360) % 360;
    return deg;
  }

  // Wrap a delta into (-180, 180] so small motions never look like big jumps.
  function shortestDelta(prev, curr) {
    let d = curr - prev;
    while (d > 180) d -= 360;
    while (d <= -180) d += 360;
    return d;
  }

  function wrapTotal(total) {
    let t = total % CYCLE;
    if (t < 0) t += CYCLE;
    return t;
  }

  function startDrag(handName, ev) {
    if (state.mode !== 'manual') return;
    drag.active = true;
    drag.hand = handName;
    drag.lastAngle = angleFromPointer(ev);
    drag.pointerId = ev.pointerId;
    document.body.classList.add('dragging');
    if (el.device) el.device.classList.add('dragging');
    // Add .dragging to the visible hand so CSS can thicken its stroke.
    const visibleHand = handName === 'hour' ? el.hour
                      : handName === 'minute' ? el.minute
                      : el.second;
    if (visibleHand) visibleHand.classList.add('dragging');
    try { ev.target.setPointerCapture(ev.pointerId); } catch (_) { /* ignore */ }
    ev.preventDefault();
  }

  function onDragMove(ev) {
    if (!drag.active) return;
    const newAngle = angleFromPointer(ev);
    const delta = shortestDelta(drag.lastAngle, newAngle);
    drag.lastAngle = newAngle;

    // Convert degree delta → seconds delta based on which hand is being dragged.
    // Hour hand:   360° = 12h = 43200s   → 1° = 120s
    // Minute hand: 360° = 1h  = 3600s    → 1° = 10s
    // Second hand: 360° = 1min = 60s     → 1° = 1/6 s
    let secondsDelta = 0;
    if (drag.hand === 'hour')        secondsDelta = delta * 120;
    else if (drag.hand === 'minute') secondsDelta = delta * 10;
    else if (drag.hand === 'second') secondsDelta = delta / 6;

    state.manualTotalSeconds = wrapTotal(state.manualTotalSeconds + secondsDelta);
    renderFromManual();
  }

  function endDrag(ev) {
    if (!drag.active) return;
    // Remove .dragging from the hand we were dragging.
    const visibleHand = drag.hand === 'hour' ? el.hour
                      : drag.hand === 'minute' ? el.minute
                      : drag.hand === 'second' ? el.second
                      : null;
    if (visibleHand) visibleHand.classList.remove('dragging');
    drag.active = false;
    drag.hand = null;
    document.body.classList.remove('dragging');
    if (el.device) el.device.classList.remove('dragging');
    if (ev && ev.target && drag.pointerId != null) {
      try { ev.target.releasePointerCapture(drag.pointerId); } catch (_) { /* ignore */ }
    }
    drag.pointerId = null;
  }

  function wireDrag() {
    // Wire each hand (both the visible line and the wider invisible hit area).
    const pairs = [
      ['hour',   el.hour,   el.hourHit],
      ['minute', el.minute, el.minuteHit],
      ['second', el.second, el.secondHit],
    ];
    pairs.forEach(([name, visible, hit]) => {
      [visible, hit].forEach(node => {
        if (!node) return;
        node.addEventListener('pointerdown', (ev) => startDrag(name, ev));
      });
    });
    document.addEventListener('pointermove', onDragMove);
    document.addEventListener('pointerup', endDrag);
    document.addEventListener('pointercancel', endDrag);
  }

  // ---- v3.6.5: TTS speaker button ----
  // Builds a natural EN/ZH sentence from the currently displayed digital time
  // and feeds it to window.speechSynthesis. Mirrors the Android TimeSpeech helper.
  const ONES = ['zero','one','two','three','four','five','six','seven','eight','nine',
                'ten','eleven','twelve','thirteen','fourteen','fifteen','sixteen','seventeen','eighteen','nineteen'];
  function twoDigitWords(n) {
    if (n < 20) return ONES[n];
    const tens = ['twenty','thirty','forty','fifty'][Math.floor(n / 10) - 2];
    const rest = n % 10;
    return rest === 0 ? tens : tens + '-' + ONES[rest];
  }
  function buildSentenceEn(h, m, s, withS, fmt, isPm) {
    let hourWord;
    if (fmt === '24')      hourWord = String(h);
    else if (h === 0)      hourWord = '12';
    else                   hourWord = String(h);
    let out = "It's " + hourWord;
    if (m === 0 && !withS) {
      out += " o'clock";
    } else {
      out += ' ' + twoDigitWords(m);
      if (withS) out += ' and ' + twoDigitWords(s) + ' seconds';
    }
    if (fmt === '12') out += isPm ? ' PM' : ' AM';
    return out;
  }
  function buildSentenceZh(h, m, s, withS, fmt, isPm) {
    const prefix = fmt === '12' ? (isPm ? '下午' : '上午') : '';
    let hourWord;
    if (fmt === '24')         hourWord = h + '点';
    else                      hourWord = (h === 0 ? 12 : h) + '点';
    let rest;
    if (m === 0 && !withS)    rest = '整';
    else if (!withS)          rest = m + '分';
    else                      rest = m + '分' + s + '秒';
    return '现在是' + prefix + hourWord + rest;
  }
  function speakCurrentTime() {
    if (!('speechSynthesis' in window)) return;
    // Read what the digital readout actually shows (works in both auto and manual).
    const txt = (el.digitalTime && el.digitalTime.textContent) || '';
    // Parse "HH:MM" or "HH:MM:SS". Always take ints.
    const parts = txt.split(':').map(p => parseInt(p, 10) || 0);
    const h = parts[0] || 0;
    const m = parts[1] || 0;
    const s = parts.length > 2 ? parts[2] : 0;
    const withS = state.showSeconds === '1';
    const fmt   = state.format; // '12' | '24'
    // isPm: only meaningful in auto+12h. In manual mode we don't know PM.
    let isPm = false;
    if (state.mode === 'auto' && fmt === '12') {
      isPm = (new Date()).getHours() >= 12;
    }
    const sentence = state.lang === 'zh'
      ? buildSentenceZh(h, m, s, withS, fmt, isPm)
      : buildSentenceEn(h, m, s, withS, fmt, isPm);
    const u = new SpeechSynthesisUtterance(sentence);
    u.lang = state.lang === 'zh' ? 'zh-CN' : 'en-US';
    try { window.speechSynthesis.cancel(); } catch (_) { /* ignore */ }
    window.speechSynthesis.speak(u);
  }
  function wireSpeak() {
    if (el.speakBtn) {
      el.speakBtn.addEventListener('click', speakCurrentTime);
    }
  }

  // ---- Device picker ----
  const DEVICES = ['phone-portrait', 'phone-landscape', 'tablet-portrait', 'tablet-landscape'];
  function applyDevice() {
    DEVICES.forEach(d => el.device.classList.remove(d));
    el.device.classList.add(state.device);
    el.devicePicker.forEach(btn => {
      btn.classList.toggle('selected', btn.dataset.device === state.device);
    });
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
  wireDrag();
  wireSpeak();
  applyDevice();
  applyLang();
  applyShowSeconds();
  applyMode();           // sets mode=auto by default → starts the loop
  setTimeout(positionClock, 80);
  setTimeout(positionClock, 400);
})();
