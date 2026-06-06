# ClockWise — Interactive M3 Prototype

A fully interactive, browser-based prototype of the **ClockWise** Android app — built with vanilla HTML/CSS/JS, no frameworks, no build step. It looks and behaves like a real Material Design 3 Android app and lets you explore every feature.

> 🌐 **Live demo:** https://batteria.github.io/ClockWise-Android/prototype/

---

## What's inside

| Screen | What you can do |
|---|---|
| **Clock** (home) | Live analog clock, drag hands to set custom time, switch live/manual mode, tap to hear time spoken, watch level progress |
| **Quiz** | 3 question types (read clock, time-calc, set clock), 4-option grid, streak chip, animated correct/wrong feedback, haptic vibration, 10-question session with results screen + stars |
| **Achievements** | 5 achievement cards with progress bars, total stars hero card, tap locked cards for unlock hint |
| **Settings** (bottom sheet) | Language toggle (EN/中), 12h/24h, hand thickness slider, hand visibility, high-contrast clock, voice toggle, theme, parent unlock-all |
| **Level-up overlay** | Full-screen celebration with star burst when crossing thresholds (10/25/50 stars) |

## Interactive features

- ✅ Live analog clock (updates every second)
- ✅ Drag hour/minute hands to set time
- ✅ All settings persist in `localStorage`
- ✅ Language hot-swap (English ↔ 中文) — every string is translated
- ✅ 12h/24h, hand visibility, thickness, high-contrast — all live
- ✅ Web Speech API for "tap clock to hear time"
- ✅ Vibration API for quiz feedback (mobile only)
- ✅ Quiz state machine with 3 question types
- ✅ Progressive feature unlocking (L1 → L4)
- ✅ Dark mode (toggle in top-right on desktop view)
- ✅ URL hash routing (`#clock`, `#quiz`, `#achievements`, `#settings`)
- ✅ Browser back/forward works
- ✅ Bottom navigation with M3 active pill
- ✅ Smooth screen transitions
- ✅ Ripple on every interactive element
- ✅ Snackbar, dialog, bottom sheet (real M3 components)
- ✅ Responsive: desktop shows phone frame, mobile fills screen

## Run locally

No build needed — it's static files. Just serve the directory:

```bash
cd docs/prototype
python3 -m http.server 8765
# then open http://localhost:8765/
```

Or any other static server (npx serve, caddy, nginx, etc.).

## Explore via URL flags

You can preview specific app states by appending a `?preview=…` parameter:

| URL | What it shows |
|---|---|
| `?preview=settings` | Open settings bottom sheet |
| `?preview=quiz-ready` | Jump directly into a quiz session |
| `?preview=quiz-results` | Show the results screen after a quiz |
| `?preview=ach` | Open achievements with sample progress (32 stars) |
| `?preview=ach-zh` | Same, but Chinese |
| `?preview=levelup` | Trigger the level-up celebration overlay |
| `?preview=dark` | Force dark theme |

These short-circuit some state for screenshotting/review.

## File structure

```
docs/prototype/
├── README.md            ← you are here
├── index.html           ← single page, all screens nested
├── css/
│   ├── m3-tokens.css    ← Material 3 design tokens (light + dark)
│   ├── m3-components.css ← M3 component library (button, card, FAB, switch, slider, sheet, snackbar, dialog…)
│   └── app.css          ← screen-specific layouts
├── js/
│   ├── state.js         ← reactive store + i18n strings + localStorage
│   ├── router.js        ← screen navigation + hash routing
│   ├── clock.js         ← SVG analog clock + draggable hands
│   ├── quiz.js          ← quiz state machine + question generators
│   ├── settings.js      ← settings sheet logic
│   ├── speech.js        ← Web Speech API wrapper
│   └── main.js          ← bootstrap, achievements, level-up overlay
└── assets/              ← icons, images
```

## Tech notes

- **Material Symbols Rounded** for all icons (loaded from Google Fonts CDN)
- **Roboto + Roboto Flex** for typography
- **No frameworks** — pure vanilla, fully reviewable
- **No build step** — open `index.html` and it works
- **State persists** in `localStorage` under `clockwise:state` — clear it to reset
- **Routing** uses `location.hash`; supports browser back/forward
- **Dark mode** toggle (`data-theme="dark"` on `<html>`) — preference persists

## Resetting state

To wipe all stars/level/settings:

```js
localStorage.removeItem('clockwise:state');
location.reload();
```

Or in DevTools → Application → Local Storage → delete the key.

## Limitations

This is a static prototype — it does **not** include:

- Real backend / sync (settings are local-only)
- Push notifications
- Actual parent PIN code enforcement (the unlock toggle is just a flag)
- Sound effects (only Web Speech for time announcements)
- Production-grade accessibility audit (basic keyboard nav works, but no full ARIA review yet)

The prototype is designed to be visually and behaviourally faithful enough for design review, demos, and stakeholder walk-throughs.

---

Built as a working spec for the native Android implementation. Feedback → open an issue on the main repo.
