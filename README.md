# Sticky Note - Android Floating Notes

A simple and intuitive Android application that allows users to create floating sticky notes that stay on top of other applications. Perfect for quick reminders, snippets of information, or keeping track of tasks while using other apps.

## Features

- **Floating UI**: Notes float over other applications using Android's overlay permission.
- **Persistence**: Notes are automatically saved to a local Room database, so your data is preserved even if the service is restarted.
- **Drag-and-Drop**: Easily move the sticky note anywhere on your screen.
- **Auto-Transparency**: The note becomes semi-transparent when not being interacted with or focused, minimizing visual distraction.
- **Dynamic Resizing**: The note expands when you tap to edit and shrinks back to a compact view when you're done.

## Tech Stack

- **Kotlin**: Primary programming language.
- **Jetpack Compose**: For building a modern, reactive UI, even within a Service.
- **Room Persistence Library**: For local data storage.
- **Android Services**: Utilizes a `LifecycleService` to manage the floating overlay lifecycle.

## Getting Started

### Prerequisites

- Android Device or Emulator running Android 7.0 (API 24) or higher.
- Overlay permission enabled (the app will prompt you to grant this on first launch).

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/stickynote.git
   ```
2. Open the project in Android Studio.
3. Build and Run the app on your device/emulator.

### Usage

1. Launch the app from your app drawer.
2. Grant the **Overlay Permission** when prompted.
3. Click **"Start Floating Note"** to show the sticky note.
4. Drag the note to position it.
5. Tap the text area to edit the note.
6. Click the **"X"** icon to close the note and stop the service.

## Project Structure

- `app/src/main/java/com/example/stickynotes/`
  - `MainActivity.kt`: Entry point, handles permission requests and service control.
  - `FloatingNoteService.kt`: Core logic for the floating overlay and UI using Jetpack Compose.
  - `data/`: Room database setup (Entities, DAO, and Database).
  - `ui/theme/`: Compose theme definitions.

## License

This project is licensed under the MIT License.
