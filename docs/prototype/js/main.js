/* ============================================
   Main bootstrap & screen renderers
   ============================================ */

// ============ Ripple effect ============
function attachRipple(el) {
  el.addEventListener("click", function (ev) {
    if (el.disabled) return;
    const rect = el.getBoundingClientRect();
    const ripple = document.createElement("span");
    ripple.className = "ripple-effect";
    const size = Math.max(rect.width, rect.height);
    ripple.style.width = ripple.style.height = size + "px";
    ripple.style.left = (ev.clientX - rect.left - size / 2) + "px";
    ripple.style.top = (ev.clientY - rect.top - size / 2) + "px";
    el.appendChild(ripple);
    setTimeout(() => ripple.remove(), 600);
  });
}

function attachRipples(root = document) {
  root.querySelectorAll(".m3-btn, .m3-fab, .m3-icon-btn, .m3-chip, .m3-list-item, .option, .ach-card, .nav-item, .m3-segmented .segment, .m3-card.interactive").forEach(el => {
    if (el._rippleBound) return;
    el._rippleBound = true;
    if (getComputedStyle(el).position === "static") el.style.position = "relative";
    el.style.overflow = "hidden";
    attachRipple(el);
  });
}

// ============ Status bar real time ============
function updateStatusBar() {
  const el = document.querySelector(".status-bar .time");
  if (el) {
    const d = new Date();
    const h = State.get().format24h ? d.getHours() : (((d.getHours() + 11) % 12) + 1);
    el.textContent = `${Clock.pad2(h)}:${Clock.pad2(d.getMinutes())}`;
  }
}

// ============ Clock screen ============
let _mainClockSvg = null;
let _clockTickHandle = null;

function renderClock() {
  const svg = document.getElementById("main-clock");
  if (!svg) return;
  const s = State.get();
  Clock.buildClockSVG(svg, {
    showSecondHand: s.showSecondHand && s.liveTime,
    showMinuteNumbers: s.showMinuteNumbers,
    showHourNumbers: s.showHourNumbers,
    handThickness: s.handThickness,
  });
  svg.classList.toggle("high-contrast", !!s.highContrast);
  Clock.bindDrag(svg, (h, m) => {
    // refresh digital display
    tickClock();
  });
  _mainClockSvg = svg;

  // tap to speak
  svg.addEventListener("click", ev => {
    // ignore if drag in progress
    if (svg.classList.contains("dragging")) return;
    const { hour, minute } = Clock.getCurrentTime();
    Speech.sayTime(hour, minute);
    Speech.vibrate(15);
  });
}

function tickClock() {
  const svg = document.getElementById("main-clock");
  if (!svg) return;
  const { hour, minute, second } = Clock.getCurrentTime();
  Clock.setHands(svg, hour, minute, second, second !== 0);

  // Digital display
  const dEl = document.getElementById("digital-display");
  if (dEl) {
    const s = State.get();
    const formatted = Clock.formatDigital(hour, minute, second, {
      format24h: s.format24h,
      showSeconds: s.liveTime,
    });
    dEl.querySelector(".time-main").textContent = formatted.main;
    const secEl = dEl.querySelector(".seconds");
    secEl.textContent = formatted.seconds ? `.${formatted.seconds}` : "";
    dEl.querySelector(".ampm").textContent = formatted.ampm;
  }

  // Words
  const w = document.getElementById("time-words");
  if (w) w.textContent = Clock.timeWords(hour, minute, State.get().language);

  updateStatusBar();
}

function startClockTick() {
  stopClockTick();
  tickClock();
  _clockTickHandle = setInterval(tickClock, 1000);
}

function stopClockTick() {
  if (_clockTickHandle) { clearInterval(_clockTickHandle); _clockTickHandle = null; }
}

// ============ Quiz screen ============
let _quizClockSvg = null;

function startQuizSession() {
  Quiz.createSession();
  showQuizQuestion();
}

function showQuizQuestion() {
  const q = Quiz.getCurrent();
  if (!q) return;
  const wrap = document.getElementById("quiz-card-wrap");
  if (!wrap) return;

  // Build/clear
  wrap.innerHTML = "";

  // Progress
  const progress = Quiz.getProgress();
  const progressEl = document.getElementById("quiz-progress-info");
  if (progressEl) {
    progressEl.innerHTML = `
      <div class="row">
        <span>${t("quiz_progress", { n: progress.current, total: progress.total })}</span>
        <span class="streak-pill"><span class="material-symbols-rounded">local_fire_department</span>${State.get().streak}</span>
      </div>
      <div class="m3-linear-progress"><div class="bar" style="width:${(progress.current / progress.total) * 100}%"></div></div>
    `;
  }

  if (q.type === "set") {
    renderSetClockQuestion(wrap, q);
  } else {
    renderMcQuestion(wrap, q);
  }

  // Read question aloud
  if (q.type !== "set") {
    Speech.speak(q.prompt);
  } else {
    Speech.speak(`${q.prompt} ${q.targetText}`);
  }
}

function renderMcQuestion(wrap, q) {
  const showClock = q.type === "read" || q.type === "minute";

  const card = document.createElement("div");
  card.className = "quiz-card";
  card.innerHTML = `
    <span class="qtype-chip">${qTypeLabel(q.type)}</span>
    <div class="question">${escapeHtml(q.prompt)}</div>
    ${showClock ? '<svg class="analog-clock" id="quiz-clock" viewBox="0 0 240 240"></svg>' : ""}
    ${q.type === "calc" ? `<svg class="analog-clock" id="quiz-clock" viewBox="0 0 240 240"></svg>` : ""}
  `;
  wrap.appendChild(card);

  if (showClock || q.type === "calc") {
    const svg = card.querySelector("#quiz-clock");
    const s = State.get();
    Clock.buildClockSVG(svg, {
      showSecondHand: false,
      showMinuteNumbers: s.showMinuteNumbers,
      showHourNumbers: s.showHourNumbers,
      handThickness: s.handThickness,
    });
    svg.classList.toggle("high-contrast", !!s.highContrast);
    Clock.setHands(svg, q.hour, q.minute, 0, false);
  }

  // Options 2x2
  const grid = document.createElement("div");
  grid.className = "quiz-options";
  q.options.forEach(opt => {
    const btn = document.createElement("button");
    btn.className = "option ripple-container";
    btn.textContent = opt;
    btn.addEventListener("click", () => handleAnswer(btn, opt));
    grid.appendChild(btn);
  });
  wrap.appendChild(grid);

  attachRipples(wrap);
}

function renderSetClockQuestion(wrap, q) {
  const card = document.createElement("div");
  card.className = "quiz-card";
  card.innerHTML = `
    <span class="qtype-chip">${qTypeLabel(q.type)}</span>
    <div class="question">${escapeHtml(q.prompt)}</div>
    <div class="quiz-target">${q.targetText}</div>
    <svg class="analog-clock" id="quiz-set-clock" viewBox="0 0 240 240" style="cursor:pointer"></svg>
    <div class="drag-hint" style="position:relative;bottom:auto;transform:none;font-size:12px;">${t("set_clock_hint")}</div>
    <div class="set-clock-actions">
      <button class="m3-btn filled" id="set-check"><span class="material-symbols-rounded">check</span>${t("set_clock_check")}</button>
    </div>
  `;
  wrap.appendChild(card);

  const svg = card.querySelector("#quiz-set-clock");
  // make this draggable, but no live time
  Clock.buildClockSVG(svg, {
    showSecondHand: false,
    showMinuteNumbers: true,
    showHourNumbers: true,
    handThickness: State.get().handThickness,
  });
  svg.classList.toggle("high-contrast", State.get().highContrast);
  svg.style.pointerEvents = "auto";

  // Start at a random distant time (not the target)
  let userHour = (q.answer.hour + 3) % 12;
  let userMin = (q.answer.minute + 25) % 60;
  Clock.setHands(svg, userHour, userMin, 0, false);

  // Local drag (doesn't affect main state)
  let activeHand = null;
  function getAngle(ev) {
    const rect = svg.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    let deg = Math.atan2(ev.clientY - cy, ev.clientX - cx) * 180 / Math.PI + 90;
    if (deg < 0) deg += 360;
    return deg;
  }
  svg.addEventListener("pointerdown", ev => {
    const h = ev.target.closest("[data-hand]");
    if (!h) return;
    activeHand = h.dataset.hand;
    svg.setPointerCapture(ev.pointerId);
    svg.classList.add("dragging");
    ev.preventDefault();
  });
  svg.addEventListener("pointermove", ev => {
    if (!activeHand) return;
    const deg = getAngle(ev);
    if (activeHand === "minute") userMin = Math.round(deg / 6) % 60;
    if (activeHand === "hour") userHour = Math.floor(deg / 30) % 12;
    Clock.setHands(svg, userHour, userMin, 0, false);
  });
  svg.addEventListener("pointerup", () => { activeHand = null; svg.classList.remove("dragging"); });
  svg.addEventListener("pointercancel", () => { activeHand = null; svg.classList.remove("dragging"); });

  card.querySelector("#set-check").addEventListener("click", () => {
    const choice = { hour: userHour, minute: userMin };
    handleAnswer(card.querySelector("#set-check"), choice);
  });
  attachRipples(wrap);
}

function qTypeLabel(type) {
  switch (type) {
    case "read": return "READ CLOCK";
    case "calc": return "TIME CALC";
    case "minute": return "MINUTES";
    case "set": return "SET CLOCK";
    default: return type.toUpperCase();
  }
}

function handleAnswer(btn, choice) {
  // Lock everything
  document.querySelectorAll("#quiz-card-wrap .option").forEach(o => o.classList.add("locked"));
  document.querySelectorAll("#quiz-card-wrap #set-check").forEach(b => b.disabled = true);

  const q = Quiz.getCurrent();
  const correct = Quiz.answer(choice);

  if (q.type !== "set") {
    // highlight buttons
    document.querySelectorAll("#quiz-card-wrap .option").forEach(o => {
      if (o.textContent === q.answer) o.classList.add("correct");
      else if (o === btn && !correct) o.classList.add("wrong");
    });
  } else {
    if (correct) btn.classList.add("correct");
    else btn.classList.add("wrong");
  }

  if (correct) {
    Speech.vibrate(20);
    Speech.speak(t("quiz_correct"));
  } else {
    Speech.vibrate([60, 30, 60]);
    Speech.speak(t("quiz_wrong"));
  }

  // Auto-advance
  setTimeout(() => {
    if (Quiz.nextQuestion()) {
      showQuizQuestion();
    } else {
      finishQuizSession();
    }
  }, 1500);
}

function finishQuizSession() {
  const result = Quiz.finish();
  Quiz.clear();
  renderQuizResults(result);

  if (result.levelInfo && result.levelInfo.leveledUp) {
    // Show after results animate in
    setTimeout(() => showLevelUp(result.levelInfo.newLevel), 1200);
  }
  // Refresh nav unlocks
  Settings.updateNavLocks();
}

function renderQuizResults(result) {
  const wrap = document.getElementById("quiz-card-wrap");
  if (!wrap) return;
  wrap.innerHTML = "";

  const pct = Math.round((result.correct / result.total) * 100);
  const stars = Math.min(3, Math.ceil((result.correct / result.total) * 3));

  const view = document.createElement("div");
  view.className = "quiz-results";
  view.innerHTML = `
    <div class="results-circle" style="--score-deg:${(pct / 100) * 360}deg">
      <div class="pct">${pct}%</div>
    </div>
    <div class="md-headline-medium" style="font-weight:700">${t("quiz_finish_title")}</div>
    <div class="md-body-large" style="color:var(--md-sys-color-on-surface-variant)">${t("quiz_score", { score: result.correct, total: result.total })}</div>
    <div class="results-stars-row" id="results-stars">
      ${[1,2,3].map(i => `<span class="material-symbols-rounded star ${i <= stars ? "earned" : ""}" style="animation-delay:${i*200}ms">star</span>`).join("")}
    </div>
    <div class="md-title-medium" style="color:var(--md-sys-color-secondary);font-weight:700">+${result.stars} ⭐${result.bonus ? `  <span style="font-size:13px;color:var(--md-sys-color-on-surface-variant)">(+${result.bonus} bonus)</span>` : ""}</div>
    <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center;margin-top:12px">
      <button class="m3-btn outlined" data-quiz-action="home"><span class="material-symbols-rounded">home</span>${t("quiz_home")}</button>
      <button class="m3-btn filled" data-quiz-action="again"><span class="material-symbols-rounded">refresh</span>${t("quiz_again")}</button>
    </div>
  `;
  wrap.appendChild(view);

  view.querySelector("[data-quiz-action='home']").addEventListener("click", () => Router.navigate("clock"));
  view.querySelector("[data-quiz-action='again']").addEventListener("click", () => startQuizSession());

  // Stars to level — flying animation
  setTimeout(() => animateStarsToProgress(result.stars), 800);
  attachRipples(wrap);
}

function animateStarsToProgress(count) {
  // For visual richness on results screen, briefly spawn stars
  const stage = document.getElementById("screen-stack");
  if (!stage) return;
  for (let i = 0; i < Math.min(count, 5); i++) {
    setTimeout(() => {
      const star = document.createElement("div");
      star.className = "confetti-star";
      star.style.left = (50 + (Math.random() - 0.5) * 60) + "%";
      star.style.top = "-30px";
      star.textContent = "★";
      stage.appendChild(star);
      setTimeout(() => star.remove(), 3000);
    }, i * 120);
  }
}

// ============ Achievements screen ============
function renderAchievements() {
  const list = document.getElementById("ach-list");
  if (!list) return;
  const s = State.get();

  // Hero
  const hero = document.getElementById("stars-hero");
  if (hero) {
    hero.innerHTML = `
      <div class="stars-count">
        <span class="material-symbols-rounded">star</span>
        ${s.totalStarsEver}
      </div>
      <div class="label">${t("total_stars")}</div>
    `;
  }

  // Achievements
  const achList = [
    { key: "first_correct", icon: "school", label: t("ach_first_correct"), desc: t("ach_first_correct_desc"), progress: () => Math.min(1, s.quizzesCorrect), max: 1 },
    { key: "streak_5", icon: "local_fire_department", label: t("ach_streak_5"), desc: t("ach_streak_5_desc"), progress: () => Math.min(5, s.bestStreak), max: 5 },
    { key: "streak_10", icon: "bolt", label: t("ach_streak_10"), desc: t("ach_streak_10_desc"), progress: () => Math.min(10, s.bestStreak), max: 10 },
    { key: "ten_quizzes", icon: "workspace_premium", label: t("ach_ten_quizzes"), desc: t("ach_ten_quizzes_desc"), progress: () => Math.min(10, s.quizzesCompleted), max: 10 },
    { key: "perfect_quiz", icon: "military_tech", label: t("ach_perfect_quiz"), desc: t("ach_perfect_quiz_desc"), progress: () => s.achievements.perfect_quiz ? 1 : 0, max: 1 },
  ];

  list.innerHTML = "";
  achList.forEach(a => {
    const earned = !!s.achievements[a.key];
    const card = document.createElement("button");
    card.className = `ach-card ripple-container ${earned ? "earned" : "locked"}`;
    const prog = a.progress();
    const pct = Math.min(100, (prog / a.max) * 100);
    card.innerHTML = `
      <span class="material-symbols-rounded ach-icon">${a.icon}</span>
      <div class="ach-info">
        <div class="title">${a.label}</div>
        <div class="desc">${a.desc}</div>
        ${earned
          ? `<div class="trailing-badge" style="margin-top:4px"><span class="material-symbols-rounded">check_circle</span>${t("earned")}</div>`
          : `<div class="m3-linear-progress pbar"><div class="bar" style="width:${pct}%"></div></div>
             <div style="font-size:11px;color:var(--md-sys-color-on-surface-variant);margin-top:4px">${prog}/${a.max}</div>`}
      </div>
    `;
    if (!earned) {
      card.addEventListener("click", () => {
        showSnackbar(t("locked_hint"));
      });
    }
    list.appendChild(card);
  });
  attachRipples(list);
}

// ============ Level-up overlay ============
function showLevelUp(newLevel) {
  const overlay = document.getElementById("levelup-overlay");
  if (!overlay) return;

  const unlocks = getUnlockedAt(newLevel);
  overlay.innerHTML = `
    <div class="burst">
      ${Array.from({ length: 12 }).map((_, i) => `<div class="ray" style="--rot:${i * 30}deg;animation-delay:${i * 0.1}s"></div>`).join("")}
      <div class="badge-big">${newLevel}</div>
    </div>
    <div class="title">${t("levelup_title")}</div>
    <div class="sub">${t("levelup_sub", { level: newLevel })}</div>
    <div class="unlocks">
      ${unlocks.map(u => `<div class="unlock-item"><span class="material-symbols-rounded filled">${u.icon}</span>${u.label}</div>`).join("")}
    </div>
    <button class="m3-btn elevated" style="color:var(--md-sys-color-primary)" id="levelup-continue">${t("unlock_continue")}</button>
  `;
  overlay.classList.add("show");

  // Confetti
  spawnConfetti(overlay);

  overlay.querySelector("#levelup-continue").addEventListener("click", () => {
    overlay.classList.remove("show");
    Speech.vibrate([40, 20, 40]);
  });
  Speech.speak(t("levelup_title") + " " + t("levelup_sub", { level: newLevel }));
  Speech.vibrate([60, 40, 60, 40, 100]);
  attachRipples(overlay);
}

function getUnlockedAt(level) {
  const unlocks = [];
  if (level === 2) {
    unlocks.push({ icon: "quiz", label: t("unlock_quiz") });
    unlocks.push({ icon: "emoji_events", label: t("unlock_ach") });
  } else if (level === 3) {
    unlocks.push({ icon: "tune", label: t("unlock_settings") });
    unlocks.push({ icon: "pan_tool", label: "Set-clock quizzes" });
    unlocks.push({ icon: "calculate", label: "Time calculation" });
  } else if (level === 4) {
    unlocks.push({ icon: "auto_awesome", label: t("unlock_modes") });
    unlocks.push({ icon: "schedule", label: "Minute-level questions" });
  } else if (level === 5) {
    unlocks.push({ icon: "workspace_premium", label: "Master level achieved!" });
  }
  return unlocks;
}

function spawnConfetti(parent) {
  for (let i = 0; i < 20; i++) {
    setTimeout(() => {
      const s = document.createElement("div");
      s.className = "confetti-star";
      s.style.left = Math.random() * 100 + "%";
      s.style.top = "-30px";
      s.style.fontSize = (24 + Math.random() * 24) + "px";
      s.style.animationDuration = (2 + Math.random() * 1.5) + "s";
      s.style.animationDelay = (Math.random() * 0.5) + "s";
      s.textContent = "★";
      parent.appendChild(s);
      setTimeout(() => s.remove(), 3500);
    }, i * 100);
  }
}

// ============ Level card on clock screen ============
function renderLevelCard() {
  const el = document.getElementById("level-card");
  if (!el) return;
  const s = State.get();
  const lvl = currentLevel();
  const p = progressToNextLevel();

  const progressText = p.atMax
    ? t("max_level")
    : t("progress_to_next", { cur: p.cur, max: p.max, next: p.next });

  el.innerHTML = `
    <div class="level-header">
      <div class="level-badge">${lvl}</div>
      <div class="level-info">
        <div class="level-title">${t("level")} ${lvl}</div>
        <div class="level-sub">${s.stars} ⭐ · ${s.bestStreak}🔥 ${t("best_label")}</div>
      </div>
    </div>
    <div class="m3-linear-progress"><div class="bar" style="width:${p.atMax ? 100 : Math.min(100, p.percent).toFixed(1)}%"></div></div>
    <div style="font-size:12px;color:var(--md-sys-color-on-surface-variant)">${progressText}</div>
  `;
}

function renderQuickStats() {
  const wrap = document.getElementById("quick-stats");
  if (!wrap) return;
  const s = State.get();
  wrap.innerHTML = `
    <div class="stat-pill">
      <span class="material-symbols-rounded stat-icon star">star</span>
      <div class="stat-value">${s.stars}</div>
      <div class="stat-label">${t("nav_ach")}</div>
    </div>
    <div class="stat-pill">
      <span class="material-symbols-rounded stat-icon streak">local_fire_department</span>
      <div class="stat-value">${s.streak}</div>
      <div class="stat-label">${t("quiz_streak")}</div>
    </div>
    <div class="stat-pill">
      <span class="material-symbols-rounded stat-icon level">emoji_events</span>
      <div class="stat-value">L${currentLevel()}</div>
      <div class="stat-label">${t("level")}</div>
    </div>
  `;
}

// ============ Mode chips (live vs manual) ============
function renderModeChips() {
  const wrap = document.getElementById("mode-chips");
  if (!wrap) return;
  const s = State.get();
  wrap.innerHTML = `
    <div class="m3-segmented" data-mode-toggle>
      <button class="segment ${s.liveTime ? "selected" : ""}" data-value="live">
        <span class="material-symbols-rounded">check</span>
        <span class="material-symbols-rounded" style="opacity:1;width:auto">schedule</span>
        ${t("real_time")}
      </button>
      <button class="segment ${!s.liveTime ? "selected" : ""}" data-value="manual">
        <span class="material-symbols-rounded">check</span>
        <span class="material-symbols-rounded" style="opacity:1;width:auto">pan_tool</span>
        ${t("manual_time")}
      </button>
    </div>
  `;
  wrap.querySelectorAll(".segment").forEach(seg => {
    seg.addEventListener("click", () => {
      const live = seg.dataset.value === "live";
      State.set({ liveTime: live }, "mode");
      renderModeChips();
      renderClock();
      tickClock();
    });
  });
}

// ============ Top bar lang toggle ============
function renderTopBar() {
  const langChip = document.getElementById("lang-chip");
  if (langChip) {
    const s = State.get();
    langChip.textContent = s.language === "en" ? "EN" : "中";
  }
}

// ============ i18n updates ============
function refreshLabels() {
  document.querySelectorAll("[data-i18n]").forEach(el => {
    el.textContent = t(el.dataset.i18n);
  });
  renderTopBar();
  renderQuickStats();
  renderLevelCard();
  renderModeChips();
  // refresh quiz fab label
  const fab = document.querySelector("#clock-fab .label");
  if (fab) fab.textContent = t("quiz_fab");
  // re-render current quiz question for language
  if (Quiz.getCurrent()) showQuizQuestion();
  if (Router.current() === "achievements") renderAchievements();
  tickClock();
}

// ============ Helpers ============
function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

// ============ Bootstrap ============
function fitDevice() {
  const frame = document.querySelector(".device-frame");
  if (!frame) return;
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  // Mobile: full screen, no scale
  if (vw <= 480) {
    frame.style.transform = "";
    return;
  }
  // Desktop: scale to fit
  const scaleY = (vh - 60) / 915;
  const scaleX = (vw - 80) / 412;
  const scale = Math.min(1, scaleY, scaleX);
  frame.style.transform = `scale(${scale})`;
}

function bootstrap() {
  // theme
  document.documentElement.dataset.theme = State.get().theme || "light";

  // Scale device frame to fit viewport
  fitDevice();
  window.addEventListener("resize", fitDevice);

  // Settings init
  Settings.init();

  // Router init
  Router.setupNav();

  // FAB click → quiz
  document.getElementById("clock-fab")?.addEventListener("click", () => {
    if (!isFeatureUnlocked("quiz")) {
      showSnackbar(t("snackbar_quiz_locked"));
      return;
    }
    Router.navigate("quiz");
  });

  // Open settings via top bar settings icon
  document.querySelectorAll("[data-open-settings]").forEach(b => b.addEventListener("click", () => Router.openSettings()));

  // Language quick toggle in top bar
  document.getElementById("lang-chip")?.addEventListener("click", () => {
    const newLang = State.get().language === "en" ? "zh" : "en";
    State.set({ language: newLang }, "language");
  });

  // Render initial UI
  renderClock();
  startClockTick();
  renderLevelCard();
  renderQuickStats();
  renderModeChips();
  renderTopBar();
  renderAchievements();
  refreshLabels();

  // State subscriptions
  State.subscribe((s, event) => {
    // Re-render parts based on event
    if (["change", "language"].includes(event)) refreshLabels();
    if (["theme"].includes(event)) document.documentElement.dataset.theme = s.theme;
    if (event === "language") { refreshLabels(); renderClock(); tickClock(); }
    if (["change", "format24h", "showSecondHand", "showHourNumbers", "showMinuteNumbers", "highContrast", "handThickness"].includes(event)) {
      renderClock();
      tickClock();
    }
    if (["stars", "streak", "change", "achievement", "reset", "levelup"].includes(event)) {
      renderQuickStats();
      renderLevelCard();
      renderAchievements();
      Settings.refreshUnlocks();
      Settings.updateNavLocks();

      // hide FAB if quiz still locked
      const fab = document.getElementById("clock-fab");
      if (fab) fab.classList.toggle("hidden", false); // always show, but click shows snackbar
    }
    if (event === "live-off" || event === "mode" || event === "manual-time") {
      renderModeChips();
    }
  });

  // When stars hit a level threshold, levelup event already fires
  State.subscribe((s, event) => {
    if (event === "levelup") {
      // small delay so render finishes
      const lvl = currentLevel();
      setTimeout(() => showLevelUp(lvl), 200);
      // snackbar for quiz unlock specifically
      if (lvl === 2) {
        setTimeout(() => showSnackbar(t("snackbar_quiz_unlocked")), 4500);
      }
    }
  });

  // Router screen change
  Router.onChange((name) => {
    if (name === "clock") {
      // continue ticking
      startClockTick();
      renderClock();
    } else {
      // keep tick going (for status bar etc.) but always tick to refresh main clock if rendered
    }
    if (name === "quiz") {
      // begin new quiz session if none active
      if (!Quiz.getCurrent()) startQuizSession();
    } else {
      Quiz.clear();
    }
    if (name === "achievements") {
      renderAchievements();
    }
  });

  // Ripples globally
  attachRipples(document);

  // Status bar tick
  setInterval(updateStatusBar, 30000);
  updateStatusBar();

  // Initial nav locks
  Settings.updateNavLocks();

  // Theme toggle button (outside the device)
  document.getElementById("global-theme-toggle")?.addEventListener("click", () => {
    const isDark = State.get().theme === "dark";
    State.set({ theme: isDark ? "light" : "dark" }, "theme");
    document.documentElement.dataset.theme = State.get().theme;
  });

  // Demo helper: add ?demo to instantly bump stars
  if (location.search.includes("demo")) {
    setTimeout(() => State.addStars(10), 400);
  }

  // Test/preview shortcuts via URL params (useful for screenshot testing)
  const params = new URLSearchParams(location.search);
  const preview = params.get("preview");
  if (preview === "settings") {
    setTimeout(() => Router.openSettings(), 500);
  } else if (preview === "quiz-ready") {
    State.set({ parentUnlockAll: true });
    setTimeout(() => Router.navigate("quiz"), 400);
  } else if (preview === "quiz-results") {
    State.set({ parentUnlockAll: true });
    setTimeout(() => {
      Router.navigate("quiz");
      // jump to results: complete the session
      setTimeout(() => {
        const sess = Quiz.getSession();
        if (sess) {
          sess.correct = 8;
          sess.index = sess.questions.length - 1;
          finishQuizSession();
        }
      }, 300);
    }, 400);
  } else if (preview === "ach") {
    State.set({ parentUnlockAll: true, totalStarsEver: 32, quizzesCompleted: 4, bestStreak: 7 });
    State.setAchievement("first_correct");
    State.setAchievement("streak_5");
    setTimeout(() => Router.navigate("achievements"), 400);
  } else if (preview === "levelup") {
    setTimeout(() => showLevelUp(2), 400);
  } else if (preview === "dark") {
    State.set({ theme: "dark" }, "theme");
    document.documentElement.dataset.theme = "dark";
  } else if (preview === "dark-ach") {
    State.set({ theme: "dark", parentUnlockAll: true, totalStarsEver: 47, bestStreak: 12, quizzesCompleted: 11 });
    document.documentElement.dataset.theme = "dark";
    State.setAchievement("first_correct");
    State.setAchievement("streak_5");
    State.setAchievement("streak_10");
    State.setAchievement("ten_quizzes");
    setTimeout(() => Router.navigate("achievements"), 400);
  } else if (preview === "zh") {
    State.set({ language: "zh" }, "language");
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", bootstrap);
} else {
  bootstrap();
}
