/* ============================================
   Settings module — control bindings
   ============================================ */

const Settings = (() => {
  function init() {
    const sheet = document.getElementById("settings-sheet");
    if (!sheet) return;

    // Close on done
    sheet.querySelector("[data-close]")?.addEventListener("click", () => Router.closeSettings());

    // Click scrim to close
    document.getElementById("scrim")?.addEventListener("click", () => Router.closeSettings());

    // Bind every control with data-setting
    bindSwitches(sheet);
    bindSliders(sheet);
    bindSegmented(sheet);
    bindReset(sheet);

    refreshUnlocks();
  }

  function bindSwitches(scope) {
    scope.querySelectorAll(".m3-switch input[data-setting]").forEach(input => {
      const key = input.dataset.setting;
      input.checked = !!State.get()[key];
      input.addEventListener("change", () => {
        const val = input.checked;
        const patch = { [key]: val };
        State.set(patch, key);

        // Special side effects
        if (key === "theme") {
          // theme is a string elsewhere; switch maps to dark/light
          State.set({ theme: val ? "dark" : "light" }, "theme");
          document.documentElement.dataset.theme = val ? "dark" : "light";
        } else if (key === "parentUnlockAll") {
          showSnackbar(val ? t("snackbar_parent_on") : t("snackbar_parent_off"));
          Router.updateBottomNav();
          // refresh disabled state of quiz nav button
          updateNavLocks();
        }
      });
    });

    // theme switch special: read from state.theme
    const themeSwitch = scope.querySelector("[data-setting='__theme']");
    if (themeSwitch) {
      themeSwitch.checked = State.get().theme === "dark";
      themeSwitch.addEventListener("change", () => {
        State.set({ theme: themeSwitch.checked ? "dark" : "light" }, "theme");
        document.documentElement.dataset.theme = themeSwitch.checked ? "dark" : "light";
      });
    }
  }

  function bindSliders(scope) {
    scope.querySelectorAll(".m3-slider[data-setting]").forEach(slider => {
      const key = slider.dataset.setting;
      const min = parseFloat(slider.dataset.min || "0");
      const max = parseFloat(slider.dataset.max || "1");
      const step = parseFloat(slider.dataset.step || "0.05");
      const current = State.get()[key];
      const percent = ((current - min) / (max - min)) * 100;
      slider.style.setProperty("--slider-progress", percent + "%");

      let dragging = false;

      function update(ev) {
        const rect = slider.getBoundingClientRect();
        const x = (ev.clientX ?? (ev.touches && ev.touches[0]?.clientX)) - rect.left;
        let ratio = Math.min(1, Math.max(0, x / rect.width));
        let value = min + ratio * (max - min);
        // snap to step
        value = Math.round(value / step) * step;
        value = Math.min(max, Math.max(min, value));
        const newPct = ((value - min) / (max - min)) * 100;
        slider.style.setProperty("--slider-progress", newPct + "%");
        State.set({ [key]: value }, key);
      }

      slider.addEventListener("pointerdown", ev => {
        dragging = true;
        slider.setPointerCapture(ev.pointerId);
        update(ev);
      });
      slider.addEventListener("pointermove", ev => { if (dragging) update(ev); });
      slider.addEventListener("pointerup", ev => {
        dragging = false;
        try { slider.releasePointerCapture(ev.pointerId); } catch {}
      });
      slider.addEventListener("pointercancel", ev => { dragging = false; });
    });
  }

  function bindSegmented(scope) {
    scope.querySelectorAll(".m3-segmented[data-setting]").forEach(group => {
      const key = group.dataset.setting;
      const current = State.get()[key];
      group.querySelectorAll(".segment").forEach(seg => {
        seg.classList.toggle("selected", seg.dataset.value === String(current));
        seg.addEventListener("click", () => {
          group.querySelectorAll(".segment").forEach(s => s.classList.remove("selected"));
          seg.classList.add("selected");
          State.set({ [key]: seg.dataset.value }, key);
          if (key === "language") {
            window.dispatchEvent(new CustomEvent("lang-change"));
          }
        });
      });
    });
  }

  function bindReset(scope) {
    const btn = scope.querySelector("[data-action='reset']");
    if (!btn) return;
    btn.addEventListener("click", () => {
      if (confirm("Reset all progress? This cannot be undone.")) {
        State.reset();
        showSnackbar(t("snackbar_reset"));
        Router.updateBottomNav();
        updateNavLocks();
      }
    });
  }

  function refreshUnlocks() {
    const sheet = document.getElementById("settings-sheet");
    if (!sheet) return;
    sheet.querySelectorAll("[data-requires]").forEach(el => {
      const feature = el.dataset.requires;
      const ok = isFeatureUnlocked(feature);
      el.classList.toggle("disabled", !ok);
      el.querySelectorAll("input, .m3-slider, .segment").forEach(i => {
        if ("disabled" in i) i.disabled = !ok;
        i.style.pointerEvents = ok ? "" : "none";
        i.style.opacity = ok ? "" : "0.5";
      });
    });
  }

  function updateNavLocks() {
    document.querySelectorAll(".m3-bottom-nav .nav-item").forEach(item => {
      const tgt = item.dataset.target;
      if (tgt === "quiz") {
        item.classList.toggle("disabled", !isFeatureUnlocked("quiz"));
      }
    });
  }

  return { init, refreshUnlocks, updateNavLocks };
})();

window.Settings = Settings;
