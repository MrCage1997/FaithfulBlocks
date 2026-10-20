# FaithfulBlocks

Vanilla-friendly architectural building blocks for **Minecraft 26.3** using **Fabric**.

FaithfulBlocks expands Minecraft's building system with architectural forms for medieval, historical and monumental builds while retaining the Vanilla look and Resource-Pack compatibility.

## Current development version

- Minecraft: 26.3
- Fabric Loader: 0.19.5
- Fabric API: 0.161.0+26.3
- Java target: 25
- Current test line: 1.6.0 RC

## Highlights

- 46 supported Vanilla building materials
- rounded and beveled corners
- narrow and large columns
- pedestals and wall profiles
- arches with automatic corner behaviour
- stepped/sloped architectural blocks
- facade and wall-detail elements
- connected window frames with Vanilla glass panes
- Architecture Table for creating and reverting architectural forms
- Resource-Pack-friendly Vanilla texture references
- optional secure update checker with Minecraft/Fabric compatibility checks and SHA-256 verification

## Updates

The public update manifest is:

`https://raw.githubusercontent.com/MrCage1997/FaithfulBlocks/main/update.properties`

The update channel remains disabled until a corresponding GitHub Release JAR and verified SHA-256 are published.

## Building

On Windows:

```bat
BUILD-WINDOWS.bat
```

Or with Gradle:

```bash
./gradlew clean build
```

The resulting JAR is created in `build/libs/`.

## License

Code is published under the MIT License unless a file explicitly states otherwise.
