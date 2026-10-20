# FaithfulBlocks

FaithfulBlocks is a Fabric mod for **Minecraft 26.3** that adds vanilla-friendly architectural building blocks for detailed medieval, historical and monumental construction.

Current development version: **1.6.0-rc6**

## Highlights

- 46 supported Vanilla building materials
- rounded and beveled inner/outer corners
- narrow and large columns with horizontal placement
- pedestals, pilasters, wall trims and wall caps
- arches with stair-like placement and automatic corner behavior
- stepped/sloped architectural blocks
- window frames that accept Vanilla glass panes
- custom Architecture Table with reverse conversion back to the original Vanilla material
- resource-pack-friendly models that reference Vanilla textures directly
- optional built-in update checker with SHA-256 verification

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API 0.161.0+26.3 or newer
- Java 25

## Build

On Windows, run:

```text
BUILD-WINDOWS.bat
```

Or use Gradle directly:

```text
./gradlew build
```

The normal mod JAR is created in:

```text
build/libs/
```

## Update system

FaithfulBlocks checks the public update manifest at:

```text
https://raw.githubusercontent.com/MrCage1997/FaithfulBlocks/main/update.properties
```

The updater can be configured in:

```text
.minecraft/config/faithfulblocks-update.properties
```

Available modes are `off`, `notify`, and `auto_download`.

Automatic downloads are only accepted when the Minecraft version matches and the downloaded JAR passes the SHA-256 check from the manifest.

## CI and releases

Pull requests and pushes to `main` are built automatically by GitHub Actions.

Publishing a GitHub Release with a tag that exactly matches the version in `gradle.properties`, for example:

```text
v1.6.0-rc6
```

automatically builds the release JAR, uploads it to the GitHub Release, calculates its SHA-256 hash, and updates `update.properties`.

See [RELEASING.md](RELEASING.md) for the release procedure.
