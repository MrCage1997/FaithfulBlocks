# Releasing FaithfulBlocks

GitHub Actions handles the release build and update manifest automatically.

## 1. Prepare the version

Update the version in `gradle.properties`:

```properties
version=1.6.0-rc7
```

Commit and merge all changes into `main`.

## 2. Confirm CI

Wait for the normal GitHub Actions build on `main` to finish successfully.

## 3. Publish a GitHub Release

Open **Releases -> Draft a new release**.

Use a tag that exactly matches the version with a leading `v`:

```text
v1.6.0-rc7
```

Choose `main` as the target and publish the release.

## 4. What happens automatically

The release workflow will:

1. check that the release tag matches `gradle.properties`
2. build FaithfulBlocks with Java 25
3. upload `faithfulblocks-<version>.jar` to the GitHub Release
4. calculate the JAR SHA-256 checksum
5. update `update.properties` on `main`
6. enable that release as the current update target

No checksum needs to be entered manually.

## Important

Do not publish a release before the matching version has been merged into `main`.

Do not reuse an old version tag for a different JAR.

For a Minecraft version change, update the Minecraft/Fabric versions in `gradle.properties` before publishing the release. The in-game updater will not automatically cross Minecraft versions.
