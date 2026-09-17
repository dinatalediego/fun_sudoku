# Fun Sudoku — Android Native

A polished, offline-first Sudoku game for Android, built in **Kotlin + Jetpack Compose**.

## Product principles

- Native Android UX, fast startup and zero web wrapper.
- Fully playable offline.
- Procedural Sudoku generation with uniqueness checking.
- No ads and no account required for the core game.
- Architecture keeps the Sudoku engine independent from the UI so cloud sync, rankings and learning features can be added later without rewriting the game.

## Included in v1

- 9×9 classic Sudoku.
- Fácil, Medio, Difícil and Experto difficulty levels.
- Local procedural generation.
- Unique-solution validation during clue removal.
- Notes/candidate mode.
- Mistake highlighting.
- Smart highlighting of selected row, column, 3×3 box and matching values.
- Hints.
- Pause/resume.
- Game timer.
- Automatic local game persistence.
- Wins and best-time statistics by difficulty.
- Completion screen.
- Material 3 / Jetpack Compose interface.
- Unit tests for the Sudoku engine.
- GitHub Actions build that produces a debug APK artifact.

## Architecture

```text
app/src/main/java/com/dinatale/funsudoku/
├── MainActivity.kt       # Compose UI and interaction layer
├── GameModel.kt          # Immutable game state models
├── GameViewModel.kt      # Gameplay orchestration + persistence
└── SudokuEngine.kt       # Generator, solver and candidates
```

The core loop is intentionally local:

```text
Generate → Play → Persist → Complete → Record stats → New puzzle
```

No backend is necessary to play.

## Build locally

Open the repository in a recent Android Studio release, let Gradle sync, then run the `app` configuration on an Android emulator or physical phone (Android 8.0/API 26+).

## APK from GitHub

Every push to `main` runs **Android CI**, executes unit tests, builds `app-debug.apk`, and uploads it as the `fun-sudoku-debug-apk` workflow artifact. This gives the project a repeatable path to an installable Android build even before a Play Store release pipeline is configured.

## Product roadmap

The next product layers are deliberately separated from v1:

1. Daily Challenge with deterministic seed.
2. Undo/redo history and richer statistics.
3. Sudoku Academy: explain Hidden Singles, Naked Pairs, X-Wing, etc.
4. Adaptive difficulty based on solved techniques and error patterns.
5. Optional identity + cloud sync.
6. Friends, leagues and asynchronous challenges.
7. Signed release / Google Play publishing pipeline.

## Package

`com.dinatale.funsudoku`

## License

All rights reserved unless a license is added explicitly.
