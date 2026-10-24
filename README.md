# Recall

Recall is an offline study app for Android. You build decks of quiz cards and flashcards by hand or import them from CSV or JSON, then study them with spaced repetition. It has no accounts and makes no network calls. All data stays on the device and can be backed up to and restored from a single JSON file.

## Building

Requires Android Studio's bundled JDK (17 or newer) and the Android SDK. On Windows use `.\gradlew.bat`.

| What | Command |
|---|---|
| Build debug APK | `./gradlew :app:assembleDebug` |
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Instrumented tests | `./gradlew :app:connectedDebugAndroidTest` |
| Install and launch | `./gradlew :app:installDebug && adb shell am start -n com.recall/.MainActivity` |
| Lint | `./gradlew :app:lintDebug` |

## Design

Kiln (matte claymorphism), see `docs/design`.
