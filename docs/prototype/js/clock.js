/* ============================================
   Clock module — analog clock rendering, drag, formatters
   ============================================ */

const Clock = (() => {
  // Build SVG analog clock
  function buildClockSVG(svg, opts = {}) {
    const cx = 120, cy = 120, R = 100;
    const ns = "http://www.w3.org/2000/svg";

    while (svg.firstChild) svg.removeChild(svg.firstChild);
    svg.setAttribute("viewBox", "0 0 240 240");

    // Outer decorative ring
    const ringOuter = document.createElementNS(ns, "circle");
    ringOuter.setAttribute("cx", cx);
    ringOuter.setAttribute("cy", cy);
    ringOuter.setAttribute("r", R + 12);
    ringOuter.setAttribute("class", "ring");
    svg.appendChild(ringOuter);

    // Face
    const face = document.createElementNS(ns, "circle");
    face.setAttribute("cx", cx);
    face.setAttribute("cy", cy);
    face.setAttribute("r", R);
    face.setAttribute("class", "face");
    svg.appendChild(face);

    const showMinNum = opts.showMinuteNumbers !== false;
    const showHourNum = opts.showHourNumbers !== false;

    // Minute ticks + minute numbers (every 5)
    for (let i = 0; i < 60; i++) {
      const a = (i * 6 - 90) * Math.PI / 180;
      const inner = i % 5 === 0 ? R - 10 : R - 5;
      const outer = R;
      const x1 = cx + Math.cos(a) * inner;
      const y1 = cy + Math.sin(a) * inner;
      const x2 = cx + Math.cos(a) * outer;
      const y2 = cy + Math.sin(a) * outer;
      const line = document.createElementNS(ns, "line");
      line.setAttribute("x1", x1.toFixed(2));
      line.setAttribute("y1", y1.toFixed(2));
      line.setAttribute("x2", x2.toFixed(2));
      line.setAttribute("y2", y2.toFixed(2));
      line.setAttribute("class", i % 5 === 0 ? "hour-tick" : "minute-tick");
      svg.appendChild(line);

      // minute numbers (5,10,...) inside ring
      if (showMinNum && i > 0 && i % 5 === 0) {
        const numR = R - 22;
        const nx = cx + Math.cos(a) * numR;
        const ny = cy + Math.sin(a) * numR;
        const text = document.createElementNS(ns, "text");
        text.setAttribute("x", nx.toFixed(2));
        text.setAttribute("y", ny.toFixed(2));
        text.setAttribute("class", "minute-num");
        text.textContent = String(i);
        svg.appendChild(text);
      }
    }

    // Hour numbers (1-12)
    if (showHourNum) {
      for (let h = 1; h <= 12; h++) {
        const a = (h * 30 - 90) * Math.PI / 180;
        const numR = showMinNum ? R - 40 : R - 22;
        const x = cx + Math.cos(a) * numR;
        const y = cy + Math.sin(a) * numR;
        const text = document.createElementNS(ns, "text");
        text.setAttribute("x", x.toFixed(2));
        text.setAttribute("y", y.toFixed(2));
        text.setAttribute("class", "hour-num");
        text.textContent = String(h);
        svg.appendChild(text);
      }
    }

    // Hands (groups for easy rotation)
    const hourG = document.createElementNS(ns, "g");
    hourG.setAttribute("data-hand", "hour");
    hourG.setAttribute("transform", `rotate(0 ${cx} ${cy})`);

    const hourLine = document.createElementNS(ns, "line");
    hourLine.setAttribute("x1", cx);
    hourLine.setAttribute("y1", cy + 8);
    hourLine.setAttribute("x2", cx);
    hourLine.setAttribute("y2", cy - 55);
    hourLine.setAttribute("class", "hour-hand");
    hourG.appendChild(hourLine);

    // invisible grab handle for hour (wider)
    const hourGrab = document.createElementNS(ns, "line");
    hourGrab.setAttribute("x1", cx);
    hourGrab.setAttribute("y1", cy + 8);
    hourGrab.setAttribute("x2", cx);
    hourGrab.setAttribute("y2", cy - 55);
    hourGrab.setAttribute("stroke", "transparent");
    hourGrab.setAttribute("stroke-width", "24");
    hourGrab.setAttribute("class", "hand-handle hour-handle");
    hourGrab.style.cursor = "grab";
    hourG.appendChild(hourGrab);
    svg.appendChild(hourG);

    const minuteG = document.createElementNS(ns, "g");
    minuteG.setAttribute("data-hand", "minute");
    minuteG.setAttribute("transform", `rotate(0 ${cx} ${cy})`);

    const minLine = document.createElementNS(ns, "line");
    minLine.setAttribute("x1", cx);
    minLine.setAttribute("y1", cy + 12);
    minLine.setAttribute("x2", cx);
    minLine.setAttribute("y2", cy - 78);
    minLine.setAttribute("class", "minute-hand");
    minuteG.appendChild(minLine);

    const minGrab = document.createElementNS(ns, "line");
    minGrab.setAttribute("x1", cx);
    minGrab.setAttribute("y1", cy + 12);
    minGrab.setAttribute("x2", cx);
    minGrab.setAttribute("y2", cy - 78);
    minGrab.setAttribute("stroke", "transparent");
    minGrab.setAttribute("stroke-width", "24");
    minGrab.setAttribute("class", "hand-handle minute-handle");
    minGrab.style.cursor = "grab";
    minuteG.appendChild(minGrab);
    svg.appendChild(minuteG);

    // Second hand (optional)
    if (opts.showSecondHand !== false) {
      const secG = document.createElementNS(ns, "g");
      secG.setAttribute("data-hand", "second");
      secG.setAttribute("transform", `rotate(0 ${cx} ${cy})`);
      const secLine = document.createElementNS(ns, "line");
      secLine.setAttribute("x1", cx);
      secLine.setAttribute("y1", cy + 16);
      secLine.setAttribute("x2", cx);
      secLine.setAttribute("y2", cy - 88);
      secLine.setAttribute("class", "second-hand");
      secG.appendChild(secLine);
      svg.appendChild(secG);
    }

    // Center dot
    const dot = document.createElementNS(ns, "circle");
    dot.setAttribute("cx", cx);
    dot.setAttribute("cy", cy);
    dot.setAttribute("r", 6);
    dot.setAttribute("class", "center-dot");
    svg.appendChild(dot);

    const cap = document.createElementNS(ns, "circle");
    cap.setAttribute("cx", cx);
    cap.setAttribute("cy", cy);
    cap.setAttribute("r", 3);
    cap.setAttribute("class", "center-cap");
    svg.appendChild(cap);

    // Apply hand thickness multiplier via inline scale
    if (opts.handThickness && opts.handThickness !== 1) {
      const m = opts.handThickness;
      hourLine.setAttribute("stroke-width", (6 * m).toFixed(2));
      minLine.setAttribute("stroke-width", (4 * m).toFixed(2));
      const sec = svg.querySelector(".second-hand");
      if (sec) sec.setAttribute("stroke-width", (2 * m).toFixed(2));
    }
  }

  // Set rotation of a hand group based on time
  function setHands(svg, hour, minute, second, animateSecond = true) {
    if (!svg) return;
    // hour: 360deg per 12h; offset by minute (smooth)
    const hourDeg = ((hour % 12) + minute / 60) * 30;
    const minuteDeg = (minute + second / 60) * 6;
    const secondDeg = second * 6;

    const hourG = svg.querySelector('[data-hand="hour"]');
    const minG = svg.querySelector('[data-hand="minute"]');
    const secG = svg.querySelector('[data-hand="second"]');

    if (hourG) hourG.setAttribute("transform", `rotate(${hourDeg.toFixed(2)} 120 120)`);
    if (minG) minG.setAttribute("transform", `rotate(${minuteDeg.toFixed(2)} 120 120)`);
    if (secG) {
      const sec = secG.querySelector(".second-hand");
      // Skip animation at 0->360 wrap-around to avoid backward sweep
      if (sec && !animateSecond) sec.classList.add("no-transition");
      secG.setAttribute("transform", `rotate(${secondDeg.toFixed(2)} 120 120)`);
      if (sec && !animateSecond) {
        // re-enable for next tick
        requestAnimationFrame(() => requestAnimationFrame(() => sec.classList.remove("no-transition")));
      }
    }
  }

  // === Formatters ===

  function timeWords(hour, minute, lang) {
    const h12 = ((hour + 11) % 12) + 1;
    const nextH12 = (h12 % 12) + 1;
    if (minute === 0) return t("oclock", { h: h12 });
    if (minute === 15) return t("quarter_past", { h: h12 });
    if (minute === 30) return t("half_past", { h: h12 });
    if (minute === 45) return t("quarter_to", { h: nextH12 });
    if (minute <= 30) return t("n_past", { n: minute, h: h12 });
    return t("n_to", { n: 60 - minute, h: nextH12 });
  }

  function formatDigital(hour, minute, second, opts = {}) {
    const f24 = opts.format24h;
    const showSec = opts.showSeconds !== false;
    let h = hour;
    let ampm = "";
    if (!f24) {
      ampm = h >= 12 ? "PM" : "AM";
      h = ((h + 11) % 12) + 1;
    }
    return {
      main: `${pad2(h)}:${pad2(minute)}`,
      seconds: showSec ? pad2(second) : "",
      ampm,
    };
  }

  function pad2(n) { return String(n).padStart(2, "0"); }

  function getCurrentTime() {
    const s = State.get();
    if (s.liveTime) {
      const d = new Date();
      return { hour: d.getHours(), minute: d.getMinutes(), second: d.getSeconds() };
    } else {
      const mins = s.manualMinutes;
      return { hour: Math.floor(mins / 60), minute: mins % 60, second: 0 };
    }
  }

  // Drag handling
  function bindDrag(svg, onChange) {
    let activeHand = null;
    let pointerId = null;

    function getAngle(ev) {
      const rect = svg.getBoundingClientRect();
      const cx = rect.left + rect.width / 2;
      const cy = rect.top + rect.height / 2;
      const dx = ev.clientX - cx;
      const dy = ev.clientY - cy;
      // angle from 12 o'clock, clockwise, 0..360
      let deg = Math.atan2(dy, dx) * 180 / Math.PI + 90;
      if (deg < 0) deg += 360;
      return deg;
    }

    function startDrag(ev) {
      const target = ev.target;
      const handG = target.closest("[data-hand]");
      if (!handG) return;
      const hand = handG.dataset.hand;
      if (hand !== "hour" && hand !== "minute") return;
      activeHand = hand;
      pointerId = ev.pointerId;
      svg.setPointerCapture(pointerId);
      svg.classList.add("dragging", "dragging-hand");
      // mark active for visual
      svg.querySelectorAll(".hand-handle").forEach(h => h.classList.remove("active"));
      target.classList.add("active");
      ev.preventDefault();
      ev.stopPropagation();
    }

    function moveDrag(ev) {
      if (!activeHand) return;
      const deg = getAngle(ev);
      const cur = getCurrentTime();
      let { hour, minute } = cur;
      if (activeHand === "minute") {
        minute = Math.round(deg / 6) % 60;
      } else if (activeHand === "hour") {
        // hour angle is hour*30 + minute*0.5; reverse
        const hVal = deg / 30; // 0..12 float
        const newH12 = Math.floor(hVal) % 12;
        // preserve current minute, just snap to whole hour
        hour = (hour >= 12 ? 12 : 0) + newH12;
        // minute remains unchanged
      }
      // switch to manual mode
      if (State.get().liveTime) {
        State.set({ liveTime: false }, "live-off");
      }
      State.set({ manualMinutes: (hour * 60 + minute) % 1440 }, "manual-time");
      if (onChange) onChange(hour, minute);
    }

    function endDrag(ev) {
      if (!activeHand) return;
      activeHand = null;
      svg.classList.remove("dragging", "dragging-hand");
      svg.querySelectorAll(".hand-handle").forEach(h => h.classList.remove("active"));
      try { svg.releasePointerCapture(pointerId); } catch {}
      pointerId = null;
    }

    svg.addEventListener("pointerdown", startDrag);
    svg.addEventListener("pointermove", moveDrag);
    svg.addEventListener("pointerup", endDrag);
    svg.addEventListener("pointercancel", endDrag);
  }

  return { buildClockSVG, setHands, formatDigital, timeWords, getCurrentTime, bindDrag, pad2 };
})();

window.Clock = Clock;
