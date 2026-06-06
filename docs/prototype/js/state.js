/* ============================================
   App state & i18n
   ============================================ */

const STORAGE_KEY = "clockwise:state:v1";

// Default state
const DEFAULT_STATE = {
  language: "en",            // 'en' | 'zh'
  theme: "light",            // 'light' | 'dark'
  level: 1,
  stars: 0,
  totalStarsEver: 0,
  streak: 0,
  bestStreak: 0,
  quizzesCompleted: 0,
  quizzesCorrect: 0,
  format24h: false,
  showSecondHand: true,
  showMinuteNumbers: true,
  showHourNumbers: true,
  highContrast: false,
  handThickness: 1,          // 0.5..1.5 multiplier
  voiceEnabled: true,
  liveTime: true,            // realtime clock vs manual
  manualMinutes: 0,           // 0..1439 (when liveTime=false)
  parentUnlockAll: false,
  achievements: {
    first_correct: false,
    streak_5: false,
    streak_10: false,
    ten_quizzes: false,
    perfect_quiz: false,
  },
};

// Level thresholds (stars needed to reach level N)
const LEVEL_THRESHOLDS = [0, 10, 25, 50, 100];

const I18N = {
  en: {
    app_name: "ClockWise",
    nav_clock: "Clock",
    nav_quiz: "Quiz",
    nav_ach: "Stars",
    nav_settings: "Settings",
    home_title: "Hello!",
    home_subtitle: "Let's learn time today",
    clock_label: "What time is it?",
    quiz_fab: "Practice",
    quiz_title: "Time Quiz",
    quiz_q_read: "What time is shown?",
    quiz_q_calc: "What time will it be in {n} {unit}?",
    quiz_q_set: "Set the clock to:",
    quiz_q_minutes: "How many minutes past {hour}?",
    quiz_correct: "Great job!",
    quiz_wrong: "Try again!",
    quiz_progress: "Question {n} of {total}",
    quiz_streak: "Streak",
    quiz_check_answer: "Check",
    quiz_finish_title: "All done!",
    quiz_score: "{score} of {total} correct",
    quiz_again: "Play Again",
    quiz_home: "Back to Clock",
    settings_title: "Settings",
    settings_display: "Display",
    settings_clock_face: "Clock Face",
    settings_lang: "Language & Voice",
    settings_parent: "Parent Controls",
    settings_about: "About",
    settings_24h: "24-hour format",
    settings_24h_desc: "Use 24-hour digital time",
    settings_seconds: "Show second hand",
    settings_minutes_num: "Show minute numbers",
    settings_hours_num: "Show hour numbers",
    settings_contrast: "High contrast clock",
    settings_thickness: "Hand thickness",
    settings_voice: "Voice readout",
    settings_voice_desc: "Tap clock to hear the time",
    settings_dark: "Dark theme",
    settings_parent_unlock: "Unlock all features",
    settings_parent_unlock_desc: "Skip level requirements",
    settings_reset: "Reset progress",
    settings_lang_label: "App language",
    settings_close: "Done",
    achievements_title: "Achievements",
    total_stars: "Total Stars",
    ach_first_correct: "First Steps",
    ach_first_correct_desc: "Answer your first question correctly",
    ach_streak_5: "On Fire",
    ach_streak_5_desc: "Get a streak of 5 correct answers",
    ach_streak_10: "Lightning",
    ach_streak_10_desc: "Get a streak of 10 correct answers",
    ach_ten_quizzes: "Dedicated",
    ach_ten_quizzes_desc: "Complete 10 full quizzes",
    ach_perfect_quiz: "Perfectionist",
    ach_perfect_quiz_desc: "Get 10/10 on a single quiz",
    earned: "Earned",
    locked_hint: "Keep practicing to unlock!",
    levelup_title: "Level Up!",
    levelup_sub: "You've reached Level {level}",
    unlock_quiz: "Quiz mode unlocked",
    unlock_ach: "Achievements unlocked",
    best_label: "best",
    unlock_settings: "Advanced settings unlocked",
    unlock_modes: "All quiz modes unlocked",
    unlock_continue: "Continue",
    snackbar_quiz_locked: "Earn more stars to unlock Quiz!",
    snackbar_quiz_unlocked: "Quiz mode is now available!",
    snackbar_correct: "Correct! +1 star",
    snackbar_wrong: "Not quite — try again",
    snackbar_reset: "Progress has been reset",
    snackbar_parent_on: "All features unlocked",
    snackbar_parent_off: "Feature locks restored",
    set_clock_check: "Check answer",
    set_clock_hint: "Drag the hands to set the time",
    real_time: "Live time",
    manual_time: "Practice",
    drag_hint: "Tap clock to hear time · drag hands to set",
    options: "Options",
    minutes: "minute",
    minutes_plural: "minutes",
    hours: "hour",
    hours_plural: "hours",
    oclock: "{h} o'clock",
    half_past: "half past {h}",
    quarter_past: "quarter past {h}",
    quarter_to: "quarter to {h}",
    n_past: "{n} past {h}",
    n_to: "{n} to {h}",
    say_time: "It is {time}",
    level: "Level",
    progress_to_next: "{cur} / {max} stars to Level {next}",
    max_level: "Maximum level reached!",
    quiz_loading: "Loading quiz…",
    quiz_round: "Round {n}",
  },
  zh: {
    app_name: "时光",
    nav_clock: "时钟",
    nav_quiz: "练习",
    nav_ach: "成就",
    nav_settings: "设置",
    home_title: "你好！",
    home_subtitle: "今天一起学习时间吧",
    clock_label: "现在几点了？",
    quiz_fab: "开始练习",
    quiz_title: "时间挑战",
    quiz_q_read: "现在是几点？",
    quiz_q_calc: "{n}{unit}之后是几点？",
    quiz_q_set: "请把时钟调到：",
    quiz_q_minutes: "现在是{hour}点过几分？",
    quiz_correct: "答对啦！",
    quiz_wrong: "再试一次！",
    quiz_progress: "第 {n} 题 / 共 {total} 题",
    quiz_streak: "连胜",
    quiz_check_answer: "确认",
    quiz_finish_title: "完成啦！",
    quiz_score: "答对了 {score} / {total} 题",
    quiz_again: "再来一次",
    quiz_home: "返回时钟",
    settings_title: "设置",
    settings_display: "显示",
    settings_clock_face: "钟面",
    settings_lang: "语言与声音",
    settings_parent: "家长设置",
    settings_about: "关于",
    settings_24h: "24 小时制",
    settings_24h_desc: "数字时间使用 24 小时格式",
    settings_seconds: "显示秒针",
    settings_minutes_num: "显示分钟数字",
    settings_hours_num: "显示小时数字",
    settings_contrast: "高对比度钟面",
    settings_thickness: "指针粗细",
    settings_voice: "语音播报",
    settings_voice_desc: "点击钟面听时间",
    settings_dark: "深色模式",
    settings_parent_unlock: "解锁全部功能",
    settings_parent_unlock_desc: "忽略等级限制",
    settings_reset: "重置进度",
    settings_lang_label: "界面语言",
    settings_close: "完成",
    achievements_title: "成就",
    total_stars: "累计星星",
    ach_first_correct: "初出茅庐",
    ach_first_correct_desc: "答对第一道题",
    ach_streak_5: "渐入佳境",
    ach_streak_5_desc: "连续答对 5 题",
    ach_streak_10: "闪电连击",
    ach_streak_10_desc: "连续答对 10 题",
    ach_ten_quizzes: "勤奋练习",
    ach_ten_quizzes_desc: "完成 10 局练习",
    ach_perfect_quiz: "完美主义",
    ach_perfect_quiz_desc: "一局答对全部 10 题",
    earned: "已获得",
    locked_hint: "继续练习就能解锁！",
    levelup_title: "升级啦！",
    levelup_sub: "你已经升到 Lv.{level}",
    unlock_quiz: "解锁练习模式",
    unlock_ach: "解锁成就页",
    best_label: "最高",
    unlock_settings: "解锁高级设置",
    unlock_modes: "解锁全部题型",
    unlock_continue: "继续",
    snackbar_quiz_locked: "再获得一些星星就能解锁练习啦！",
    snackbar_quiz_unlocked: "练习模式已解锁！",
    snackbar_correct: "答对啦！+1⭐",
    snackbar_wrong: "差一点哦，再试试",
    snackbar_reset: "进度已重置",
    snackbar_parent_on: "已解锁所有功能",
    snackbar_parent_off: "已恢复等级限制",
    set_clock_check: "对答案",
    set_clock_hint: "拖动指针把时钟调到目标时间",
    real_time: "实时",
    manual_time: "练习",
    drag_hint: "点击钟面听时间 · 拖动指针练习",
    options: "选项",
    minutes: "分钟",
    minutes_plural: "分钟",
    hours: "小时",
    hours_plural: "小时",
    oclock: "{h}点整",
    half_past: "{h}点半",
    quarter_past: "{h}点一刻",
    quarter_to: "差一刻{h}点",
    n_past: "{h}点过{n}分",
    n_to: "差{n}分{h}点",
    say_time: "现在是{time}",
    level: "等级",
    progress_to_next: "再获得 {cur} / {max} ⭐ 升到 Lv.{next}",
    max_level: "已达最高等级！",
    quiz_loading: "练习准备中…",
    quiz_round: "第 {n} 局",
  },
};

// In-memory state
let _state = loadState();

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { ...DEFAULT_STATE, achievements: { ...DEFAULT_STATE.achievements } };
    const parsed = JSON.parse(raw);
    return {
      ...DEFAULT_STATE,
      ...parsed,
      achievements: { ...DEFAULT_STATE.achievements, ...(parsed.achievements || {}) },
    };
  } catch {
    return { ...DEFAULT_STATE, achievements: { ...DEFAULT_STATE.achievements } };
  }
}

function saveState() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(_state)); }
  catch (e) { /* ignore quota errors */ }
}

const Listeners = new Set();
function subscribe(fn) { Listeners.add(fn); return () => Listeners.delete(fn); }
function notify(event = "change") {
  Listeners.forEach(fn => { try { fn(_state, event); } catch (e) { console.error(e); } });
}

// public state API
const State = {
  get() { return _state; },

  set(patch, event = "change") {
    _state = { ..._state, ...patch };
    saveState();
    notify(event);
  },

  setAchievement(key, val = true) {
    if (_state.achievements[key] === val) return false;
    _state = {
      ..._state,
      achievements: { ..._state.achievements, [key]: val },
    };
    saveState();
    notify("achievement");
    return true;
  },

  addStars(n) {
    const prevLevel = currentLevel();
    const totalNext = _state.stars + n;
    _state.stars = Math.max(0, totalNext);
    _state.totalStarsEver = Math.max(_state.totalStarsEver, _state.stars);
    saveState();
    const newLevel = currentLevel();
    notify("stars");
    if (newLevel > prevLevel) {
      _state.level = newLevel;
      saveState();
      notify("levelup");
      return { leveledUp: true, newLevel, prevLevel };
    }
    return { leveledUp: false };
  },

  reset() {
    _state = { ...DEFAULT_STATE, language: _state.language, theme: _state.theme, achievements: { ...DEFAULT_STATE.achievements } };
    saveState();
    notify("reset");
  },

  subscribe,
};

// Helpers
function currentLevel() {
  let lvl = 1;
  for (let i = 0; i < LEVEL_THRESHOLDS.length; i++) {
    if (_state.stars >= LEVEL_THRESHOLDS[i]) lvl = i + 1;
  }
  return lvl;
}

function progressToNextLevel() {
  const lvl = currentLevel();
  if (lvl >= LEVEL_THRESHOLDS.length) return { atMax: true, cur: 0, max: 0, next: lvl };
  const curBase = LEVEL_THRESHOLDS[lvl - 1];
  const nextBase = LEVEL_THRESHOLDS[lvl];
  return {
    atMax: false,
    cur: _state.stars - curBase,
    max: nextBase - curBase,
    next: lvl + 1,
    percent: ((_state.stars - curBase) / (nextBase - curBase)) * 100,
  };
}

function isFeatureUnlocked(feature) {
  if (_state.parentUnlockAll) return true;
  const lvl = currentLevel();
  switch (feature) {
    case "quiz": return lvl >= 2;
    case "advanced_settings": return lvl >= 3;
    case "set_clock_quiz": return lvl >= 3;
    case "calc_quiz": return lvl >= 3;
    case "minute_quiz": return lvl >= 4;
    case "stats": return lvl >= 2;
    case "high_contrast": return lvl >= 3;
    case "hand_thickness": return lvl >= 4;
    case "dark_theme": return true;
    default: return true;
  }
}

// i18n
function t(key, vars = {}) {
  const dict = I18N[_state.language] || I18N.en;
  let str = dict[key] ?? I18N.en[key] ?? key;
  for (const [k, v] of Object.entries(vars)) {
    str = str.replaceAll(`{${k}}`, v);
  }
  return str;
}

// expose
window.State = State;
window.t = t;
window.I18N = I18N;
window.currentLevel = currentLevel;
window.progressToNextLevel = progressToNextLevel;
window.isFeatureUnlocked = isFeatureUnlocked;
window.LEVEL_THRESHOLDS = LEVEL_THRESHOLDS;
