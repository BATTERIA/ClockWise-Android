# 🕐 ClockWise Android

**教小朋友认识时钟 | Learn to read clocks**

A bilingual (Chinese + English) clock-learning app for children ages 4-10, built with modern Android technologies.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Animation:** Compose Animation + Lottie
- **Storage:** DataStore / Room
- **Voice:** Android TTS

## Project Structure

```
app/src/main/java/com/batteria/clockwise/
├── di/              — Hilt modules
├── presentation/
│   ├── theme/       — Material3 theming
│   ├── navigation/  — Nav graph
│   ├── clock/       — Clock learning screen
│   ├── quiz/        — Practice quizzes
│   └── settings/    — App settings
└── MainActivity.kt
```

## Documentation

- 📄 [Product Proposal](docs/proposal.html)
- 🎨 [M3 Design Mockups](docs/mockups/index.html) — high-fidelity HTML previews of all main screens
  - [Clock Screen](docs/mockups/clock-screen.html)
  - [Quiz Screen](docs/mockups/quiz-screen.html)
  - [Settings Screen](docs/mockups/settings-screen.html)
  - [Achievements](docs/mockups/achievements-screen.html)

## License

Private — © 2026 BATTERIA
