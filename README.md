# JSON Builder

[العربية](README.ar.md)

JSON Builder is an Android application for creating, editing, previewing, and managing JSON data through a visual and interactive interface.

The application is designed primarily for developers who need to prepare JSON structures, test data models, create API request bodies, and manage reusable JSON projects without manually writing every part of the JSON document.

## Project Origin

This application was created entirely with Google's artificial intelligence using **Gemini 4 Hydrogen** through **Google AI Studio**.

This `README.md` file was written by **Manus**.

> The application itself currently does not provide an Arabic user interface. Arabic support in this repository applies to the project documentation only.

## Features

- Create and edit JSON objects visually.
- Add, update, and remove JSON properties.
- Work with string, number, boolean, null, object, and array values.
- Build nested JSON structures.
- Preview the generated JSON in real time.
- Format JSON for improved readability.
- Validate the generated JSON structure.
- Create and manage multiple JSON projects.
- Store projects locally on the device.
- Copy generated JSON for use in other development tools.
- Use a modern Android interface built with Jetpack Compose.

## Potential Use Cases

### API Development

Create request bodies for `POST`, `PUT`, and `PATCH` endpoints before testing them with tools such as Postman or Insomnia.

### Mock Data Preparation

Prepare sample JSON data for testing application screens, API integrations, and different data states before connecting the application to a production backend.

### Android and Mobile Development

Create reusable data models and test objects while developing Android, iOS, or cross-platform applications.

### Web and Backend Development

Prepare JSON payloads, configuration objects, and example responses for web services and backend applications.

### Learning and Training

Use the visual builder to understand JSON objects, arrays, primitive values, and nested structures.

### Configuration Management

Create and edit JSON-based configuration files used by applications, services, scripts, and development tools.

### Reusable Data Templates

Keep multiple JSON projects for common structures such as users, products, orders, settings, and API responses.

## Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Android Gradle Plugin
- Gradle
- Room Database
- Kotlin Coroutines
- ViewModel
- StateFlow
- Android Architecture Components
- JUnit
- Robolectric

## Project Structure

```text
app/
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    ├── data/
                    │   ├── local/
                    │   │   ├── AppDatabase.kt
                    │   │   ├── JsonProjectDao.kt
                    │   │   └── JsonProjectEntity.kt
                    │   └── repository/
                    │       └── ProjectRepository.kt
                    ├── json/
                    │   └── JsonEngine.kt
                    ├── model/
                    │   └── JsonModels.kt
                    ├── ui/
                    │   ├── components/
                    │   ├── theme/
                    │   └── JsonBuilderApp.kt
                    ├── viewmodel/
                    │   └── JsonBuilderViewModel.kt
                    └── MainActivity.kt
```

## Architecture Overview

### Models

The model layer represents JSON elements, including objects, arrays, and primitive values.

### JSON Engine

The JSON engine converts the visual data structure into formatted JSON text and provides JSON-related validation and processing.

### Local Data Layer

The application uses Room Database to store JSON projects locally on the device.

### Repository

The repository layer separates data sources from the rest of the application and provides a clean interface for accessing stored projects.

### ViewModel

The ViewModel manages screen state, editing operations, project management, and communication with the data layer.

### User Interface

The user interface is built with Jetpack Compose and divided into reusable components to make future maintenance and development easier.

## Requirements

To open and build the project, you need:

- A recent version of Android Studio.
- A compatible JDK version for the Android Gradle Plugin used by the project.
- Android SDK.
- Android SDK Platform 36.
- An internet connection for downloading Gradle and project dependencies during the first build.

## Running the Project

1. Open Android Studio.
2. Select **Open**.
3. Choose the root directory of this project.
4. Wait for Gradle synchronization to finish.
5. Run the application on an Android emulator or a connected Android device.

To build a debug APK from the project root, run:

```bash
./gradlew assembleDebug
```

The generated APK will be available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Testing

Run unit tests with:

```bash
./gradlew test
```

Run connected Android tests with:

```bash
./gradlew connectedAndroidTest
```

The connected test command requires a running emulator or a connected Android device.

## Future Improvements

Possible future improvements include:

- Importing JSON files from device storage.
- Exporting projects as JSON files.
- Sharing JSON with other applications.
- JSON Schema support.
- An advanced raw text editor.
- JSON comparison and diff tools.
- Undo and redo support.
- Cloud synchronization.
- REST API integration.
- User authentication.
- Collaborative editing.
- Converting JSON into Kotlin, TypeScript, or Swift models.
- Importing data from CSV or YAML.
- Additional documentation and automated release builds.

## License

No open-source license has been selected for this project yet.

If you plan to publish the project publicly, consider adding an appropriate license, such as:

- MIT License
- Apache License 2.0
- GNU General Public License v3.0

## Credits

- The application was created entirely with Google's artificial intelligence using **Gemini 4 Hydrogen**.
- The application was created through **Google AI Studio**.
- This README was written by **Manus**.

## Documentation Languages

The main documentation language is English. An Arabic translation is available here:

- [Arabic README](README.ar.md)
