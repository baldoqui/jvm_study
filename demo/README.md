# Todo Desktop

A simple to-do list desktop app for Linux and Windows, built with Kotlin and Compose Multiplatform (Material 3).

## Features

- Add, complete and delete tasks
- Filter by all / active / completed, clear completed tasks
- Tasks persisted locally as JSON
  - Linux: `$XDG_DATA_HOME/TodoDesktop/tasks.json` (default `~/.local/share/TodoDesktop/tasks.json`)
  - Windows: `%APPDATA%\TodoDesktop\tasks.json`
- Light and dark theme following the OS setting

## Requirements

- JDK 17 or newer (the installer tasks need a full JDK that includes `jpackage`)

## Commands

```bash
./gradlew run                          # run the app
./gradlew test                         # run unit tests
./gradlew packageDistributionForCurrentOS  # build installers for the host OS
```

`jpackage` only builds installers for the OS it runs on: run the packaging task on Linux to get
`.deb`/`.rpm`/AppImage and on Windows (`gradlew.bat`) to get `.msi`/`.exe`. Output goes to
`build/compose/binaries/main/`.

