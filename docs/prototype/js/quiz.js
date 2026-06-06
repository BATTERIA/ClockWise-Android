/* ============================================
   Quiz module — questions, scoring, results
   ============================================ */

const Quiz = (() => {
  const QUESTIONS_PER_ROUND = 10;

  let session = null;

  function createSession() {
    const level = currentLevel();
    const questions = [];
    for (let i = 0; i < QUESTIONS_PER_ROUND; i++) {
      questions.push(generateQuestion(level, i));
    }
    session = {
      questions,
      index: 0,
      correct: 0,
      streakStart: State.get().streak,
      answers: [],
      starsThisRound: 0,
    };
    return session;
  }

  function generateQuestion(level, idx) {
    // Question types available by level
    const types = ["read"];
    if (isFeatureUnlocked("set_clock_quiz")) types.push("set");
    if (isFeatureUnlocked("calc_quiz")) types.push("calc");
    if (isFeatureUnlocked("minute_quiz")) types.push("minute");

    const type = types[Math.floor(Math.random() * types.length)];

    // Random time — controlled difficulty by level
    let minute;
    if (level === 1) {
      // Just hours (o'clock)
      minute = 0;
    } else if (level === 2) {
      // hours and half-hours
      minute = [0, 30][Math.floor(Math.random() * 2)];
    } else if (level === 3) {
      // hours, halves, quarters
      minute = [0, 15, 30, 45][Math.floor(Math.random() * 4)];
    } else {
      // any 5-minute increment
      minute = Math.floor(Math.random() * 12) * 5;
    }

    const hour = Math.floor(Math.random() * 12) + 1; // 1-12
    const baseHour = hour === 12 ? 0 : hour;

    switch (type) {
      case "read":
        return makeReadQ(baseHour, minute);
      case "calc":
        return makeCalcQ(baseHour, minute);
      case "minute":
        return makeMinuteQ(baseHour, minute);
      case "set":
        return makeSetQ(baseHour, minute);
      default:
        return makeReadQ(baseHour, minute);
    }
  }

  function makeReadQ(hour, minute) {
    const correct = formatAnswer(hour, minute);
    const options = generateDistractors(hour, minute, 3).map(([h, m]) => formatAnswer(h, m));
    options.push(correct);
    shuffle(options);
    return {
      type: "read",
      hour, minute,
      prompt: t("quiz_q_read"),
      options,
      answer: correct,
    };
  }

  function makeCalcQ(hour, minute) {
    // Add N hours
    const n = Math.floor(Math.random() * 4) + 1; // 1..4
    const newH = (hour + n) % 12;
    const correct = formatAnswer(newH, minute);
    const options = generateDistractors(newH, minute, 3).map(([h, m]) => formatAnswer(h, m));
    options.push(correct);
    shuffle(options);
    return {
      type: "calc",
      hour, minute,
      prompt: t("quiz_q_calc", { n, unit: n === 1 ? t("hours") : t("hours_plural") }),
      options,
      answer: correct,
    };
  }

  function makeMinuteQ(hour, minute) {
    const h12 = hour === 0 ? 12 : hour;
    const correct = String(minute);
    const others = new Set();
    while (others.size < 3) {
      const m = Math.floor(Math.random() * 12) * 5;
      if (m !== minute) others.add(String(m));
    }
    const options = [correct, ...others];
    shuffle(options);
    return {
      type: "minute",
      hour, minute,
      prompt: t("quiz_q_minutes", { hour: h12 }),
      options,
      answer: correct,
    };
  }

  function makeSetQ(hour, minute) {
    return {
      type: "set",
      hour, minute,
      prompt: t("quiz_q_set"),
      options: null,
      answer: { hour, minute },
      targetText: formatAnswer(hour, minute),
    };
  }

  function formatAnswer(hour, minute) {
    const f24 = State.get().format24h;
    let h = hour;
    let ampm = "";
    if (!f24) {
      ampm = h >= 12 ? "PM" : "AM";
      h = ((h + 11) % 12) + 1;
    }
    return `${Clock.pad2(h)}:${Clock.pad2(minute)}${ampm ? " " + ampm : ""}`;
  }

  function generateDistractors(hour, minute, n) {
    const result = [];
    const seen = new Set([`${hour}-${minute}`]);
    const candidates = [
      [(hour + 1) % 12, minute],
      [(hour + 11) % 12, minute],
      [hour, (minute + 15) % 60],
      [hour, (minute + 30) % 60],
      [(hour + 2) % 12, minute],
      [hour, (minute + 45) % 60],
      [(hour + 6) % 12, minute],
      [(hour + 1) % 12, (minute + 30) % 60],
    ];
    shuffle(candidates);
    for (const c of candidates) {
      const key = `${c[0]}-${c[1]}`;
      if (!seen.has(key)) {
        seen.add(key);
        result.push(c);
        if (result.length >= n) return result;
      }
    }
    while (result.length < n) result.push([(hour + result.length + 1) % 12, minute]);
    return result;
  }

  function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  function getCurrent() { return session && session.questions[session.index]; }
  function getSession() { return session; }
  function getProgress() {
    return session ? { current: session.index + 1, total: session.questions.length } : null;
  }

  function answer(choice) {
    const q = getCurrent();
    if (!q) return null;
    let correct;
    if (q.type === "set") {
      const a = choice;
      correct = a && a.hour === q.answer.hour && a.minute === q.answer.minute;
    } else {
      correct = choice === q.answer;
    }
    session.answers.push({ q, choice, correct });
    if (correct) {
      session.correct++;
      State.set({ streak: State.get().streak + 1 }, "streak");
      const cur = State.get();
      if (cur.streak > (cur.bestStreak || 0)) State.set({ bestStreak: cur.streak }, "streak");
    } else {
      State.set({ streak: 0 }, "streak");
    }
    return correct;
  }

  function nextQuestion() {
    if (!session) return false;
    if (session.index + 1 >= session.questions.length) return false;
    session.index++;
    return true;
  }

  function finish() {
    if (!session) return null;
    const totalCorrect = session.correct;
    const earnedStars = totalCorrect; // 1 star per correct
    session.starsThisRound = earnedStars;

    // Update achievements + stats
    State.set({
      quizzesCompleted: State.get().quizzesCompleted + 1,
      quizzesCorrect: State.get().quizzesCorrect + totalCorrect,
    });

    let unlockedAchievements = [];
    const ach = State.get().achievements;
    if (totalCorrect >= 1 && !ach.first_correct) {
      State.setAchievement("first_correct");
      unlockedAchievements.push("first_correct");
    }
    if ((State.get().bestStreak || 0) >= 5 && !ach.streak_5) {
      State.setAchievement("streak_5");
      unlockedAchievements.push("streak_5");
    }
    if ((State.get().bestStreak || 0) >= 10 && !ach.streak_10) {
      State.setAchievement("streak_10");
      unlockedAchievements.push("streak_10");
    }
    if (State.get().quizzesCompleted >= 10 && !ach.ten_quizzes) {
      State.setAchievement("ten_quizzes");
      unlockedAchievements.push("ten_quizzes");
    }
    if (totalCorrect === QUESTIONS_PER_ROUND && !ach.perfect_quiz) {
      State.setAchievement("perfect_quiz");
      unlockedAchievements.push("perfect_quiz");
    }

    // Bonus stars: 2 extra for perfect, 1 extra for ≥80%
    let bonus = 0;
    if (totalCorrect === QUESTIONS_PER_ROUND) bonus = 2;
    else if (totalCorrect >= 8) bonus = 1;

    const finalStars = earnedStars + bonus;
    const levelInfo = State.addStars(finalStars);

    return {
      total: QUESTIONS_PER_ROUND,
      correct: totalCorrect,
      stars: finalStars,
      bonus,
      levelInfo,
      unlockedAchievements,
    };
  }

  function clear() { session = null; }

  return {
    createSession,
    getCurrent,
    getSession,
    getProgress,
    answer,
    nextQuestion,
    finish,
    clear,
    QUESTIONS_PER_ROUND,
    formatAnswer,
  };
})();

window.Quiz = Quiz;
