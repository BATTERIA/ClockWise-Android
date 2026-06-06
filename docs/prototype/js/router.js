/* ============================================
   Screen router
   ============================================ */

const Router = (() => {
  const ROUTES = ["clock", "quiz", "achievements", "settings"];
  let current = "clock";
  const listeners = new Set();

  function getScreen(name) {
    return document.querySelector(`.screen[data-screen="${name}"]`);
  }

  function navigate(name, opts = {}) {
    if (!ROUTES.includes(name)) name = "clock";

    // gate quiz behind unlock
    if (name === "quiz" && !window.isFeatureUnlocked("quiz")) {
      showSnackbar(t("snackbar_quiz_locked"));
      return false;
    }

    if (name === current && !opts.force) return false;

    const prev = current;
    current = name;

    // animate
    const prevEl = getScreen(prev);
    const nextEl = getScreen(name);

    if (prevEl) {
      prevEl.classList.remove("active");
      prevEl.classList.add("exit-left");
      setTimeout(() => prevEl.classList.remove("exit-left"), 350);
    }
    if (nextEl) {
      // brief delay to allow CSS transition
      requestAnimationFrame(() => {
        nextEl.classList.add("active");
      });
    }

    // hash
    if (!opts.silent) {
      history.replaceState({ screen: name }, "", "#" + name);
    }

    // update bottom nav
    updateBottomNav();

    listeners.forEach(fn => { try { fn(name, prev); } catch (e) {} });
    return true;
  }

  function updateBottomNav() {
    document.querySelectorAll(".m3-bottom-nav .nav-item").forEach(item => {
      item.classList.toggle("active", item.dataset.target === current);
    });
  }

  function setupNav() {
    document.querySelectorAll(".m3-bottom-nav .nav-item").forEach(item => {
      item.addEventListener("click", () => {
        const target = item.dataset.target;
        if (target === "settings") {
          openSettings();
        } else {
          navigate(target);
        }
      });
    });

    // hash on load
    const hash = location.hash.replace("#", "");
    if (ROUTES.includes(hash)) navigate(hash, { silent: true });
    else navigate("clock", { silent: true });

    // browser back/forward
    window.addEventListener("popstate", e => {
      const target = (e.state && e.state.screen) || location.hash.replace("#", "") || "clock";
      navigate(target, { silent: true, force: true });
    });
  }

  function openSettings() {
    const sheet = document.getElementById("settings-sheet");
    const scrim = document.getElementById("scrim");
    if (sheet) sheet.classList.add("open");
    if (scrim) scrim.classList.add("open");
    history.pushState({ sheet: "settings" }, "", "#settings");
  }

  function closeSettings() {
    const sheet = document.getElementById("settings-sheet");
    const scrim = document.getElementById("scrim");
    if (sheet) sheet.classList.remove("open");
    if (scrim) scrim.classList.remove("open");
    // restore hash for current screen
    history.replaceState({ screen: current }, "", "#" + current);
  }

  function onChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }

  return { navigate, onChange, setupNav, openSettings, closeSettings, current: () => current, updateBottomNav };
})();

// Snackbar helper
let _snackbarTimer = null;
function showSnackbar(text, action) {
  const sb = document.getElementById("snackbar");
  if (!sb) return;
  sb.querySelector(".message").textContent = text;
  const actionEl = sb.querySelector(".action");
  if (action) {
    actionEl.textContent = action.label || "OK";
    actionEl.style.display = "";
    actionEl.onclick = () => { action.onClick && action.onClick(); sb.classList.remove("show"); };
  } else {
    actionEl.style.display = "none";
  }
  sb.classList.add("show");
  clearTimeout(_snackbarTimer);
  _snackbarTimer = setTimeout(() => sb.classList.remove("show"), 3200);
}

window.Router = Router;
window.showSnackbar = showSnackbar;
