/* ============================================
   Web Speech API integration
   ============================================ */

const Speech = (() => {
  const supported = "speechSynthesis" in window;

  function speak(text, langOverride) {
    if (!supported) return;
    if (!State.get().voiceEnabled) return;
    try {
      window.speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(text);
      u.lang = langOverride || (State.get().language === "zh" ? "zh-CN" : "en-US");
      u.rate = State.get().language === "zh" ? 0.95 : 0.95;
      u.pitch = 1.05;
      u.volume = 1;
      window.speechSynthesis.speak(u);
    } catch (e) {
      console.warn("speech failed", e);
    }
  }

  function sayTime(hour, minute) {
    const words = Clock.timeWords(hour, minute, State.get().language);
    const phrase = t("say_time", { time: words });
    speak(phrase);
  }

  function vibrate(pattern) {
    if (!navigator.vibrate) return;
    try { navigator.vibrate(pattern); } catch {}
  }

  function cancel() { if (supported) window.speechSynthesis.cancel(); }

  return { speak, sayTime, vibrate, cancel, supported };
})();

window.Speech = Speech;
