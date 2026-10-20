package de.faithfulblocks.update;

import de.faithfulblocks.FaithfulBlocks;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateChecker {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile UpdateStatus status = UpdateStatus.idle();

    private UpdateChecker() {
    }

    public static void initializeAsync() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        UpdateConfig config = UpdateConfig.load();
        if (config.mode() == UpdateMode.OFF) {
            FaithfulBlocks.LOGGER.info("FaithfulBlocks update checks are disabled.");
            return;
        }
        if (config.manifestUrl().isBlank()) {
            FaithfulBlocks.LOGGER.info("FaithfulBlocks updater is ready, but no manifest_url is configured yet. See config/faithfulblocks-update.properties.");
            return;
        }

        Thread thread = new Thread(() -> runCheck(config), "FaithfulBlocks-UpdateChecker");
        thread.setDaemon(true);
        thread.start();
    }

    public static UpdateStatus getStatus() {
        return status;
    }

    private static void runCheck(UpdateConfig config) {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            String currentVersion = versionOf(loader, FaithfulBlocks.MOD_ID).orElse("0.0.0");
            status = new UpdateStatus(UpdateStatus.State.CHECKING, currentVersion, "", "Checking for FaithfulBlocks updates...");

            URI manifestUri = validatedHttpsUri(config.manifestUrl());
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(manifestUri)
                    .timeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                    .header("User-Agent", "FaithfulBlocks-Updater/" + currentVersion)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Update manifest returned HTTP " + response.statusCode());
            }

            UpdateManifest manifest = UpdateManifest.parse(response.body());
            if (!manifest.enabled()) {
                String message = "FaithfulBlocks update channel is currently disabled by the publisher.";
                status = new UpdateStatus(UpdateStatus.State.UP_TO_DATE, currentVersion, manifest.version(), message);
                FaithfulBlocks.LOGGER.info(message);
                return;
            }

            String currentMinecraft = loader.getRawGameVersion();
            String currentLoader = versionOf(loader, "fabricloader").orElse("0");
            String currentApi = versionOf(loader, "fabric-api").orElse("0");

            if (!manifest.minecraft().equals(currentMinecraft)) {
                String message = "Newest FaithfulBlocks release targets Minecraft " + manifest.minecraft()
                        + ", current game is " + currentMinecraft + ". No update will be installed.";
                status = new UpdateStatus(UpdateStatus.State.INCOMPATIBLE, currentVersion, manifest.version(), message);
                FaithfulBlocks.LOGGER.info(message);
                return;
            }

            if (!manifest.fabricLoaderMin().isBlank() && VersionUtil.compare(currentLoader, manifest.fabricLoaderMin()) < 0) {
                String message = "FaithfulBlocks " + manifest.version() + " requires Fabric Loader "
                        + manifest.fabricLoaderMin() + " or newer. Current: " + currentLoader + ".";
                status = new UpdateStatus(UpdateStatus.State.INCOMPATIBLE, currentVersion, manifest.version(), message);
                FaithfulBlocks.LOGGER.warn(message);
                return;
            }

            if (!manifest.fabricApiMin().isBlank() && VersionUtil.compare(currentApi, manifest.fabricApiMin()) < 0) {
                String message = "FaithfulBlocks " + manifest.version() + " requires Fabric API "
                        + manifest.fabricApiMin() + " or newer. Current: " + currentApi + ".";
                status = new UpdateStatus(UpdateStatus.State.INCOMPATIBLE, currentVersion, manifest.version(), message);
                FaithfulBlocks.LOGGER.warn(message);
                return;
            }

            if (VersionUtil.compare(manifest.version(), currentVersion) <= 0) {
                String message = "FaithfulBlocks is up to date (" + currentVersion + ").";
                status = new UpdateStatus(UpdateStatus.State.UP_TO_DATE, currentVersion, manifest.version(), message);
                FaithfulBlocks.LOGGER.info(message);
                return;
            }

            String message = "FaithfulBlocks update available: " + currentVersion + " -> " + manifest.version();
            status = new UpdateStatus(UpdateStatus.State.UPDATE_AVAILABLE, currentVersion, manifest.version(), message);
            FaithfulBlocks.LOGGER.warn(message);

            if (config.mode() == UpdateMode.AUTO_DOWNLOAD) {
                Path downloaded = downloadAndVerify(client, config, manifest);
                scheduleInstallAfterExit(downloaded, manifest.version());
                String downloadedMessage = "FaithfulBlocks " + manifest.version()
                        + " downloaded and verified. It will replace the current JAR after Minecraft closes.";
                status = new UpdateStatus(UpdateStatus.State.DOWNLOADED, currentVersion, manifest.version(), downloadedMessage);
                FaithfulBlocks.LOGGER.warn(downloadedMessage);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("FaithfulBlocks update check was interrupted.", exception);
        } catch (Exception exception) {
            fail("FaithfulBlocks update check failed. The game will continue normally.", exception);
        }
    }

    private static Path downloadAndVerify(HttpClient client, UpdateConfig config, UpdateManifest manifest)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        URI downloadUri = validatedHttpsUri(manifest.downloadUrl());
        Path updateDir = FabricLoader.getInstance().getGameDir().resolve("mods").resolve(".faithfulblocks-update");
        Files.createDirectories(updateDir);

        Path temp = updateDir.resolve("faithfulblocks-" + safeVersion(manifest.version()) + ".jar.download");
        Files.deleteIfExists(temp);

        HttpRequest request = HttpRequest.newBuilder(downloadUri)
                .timeout(Duration.ofSeconds(Math.max(config.requestTimeoutSeconds(), 60)))
                .header("User-Agent", "FaithfulBlocks-Updater")
                .GET()
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(temp);
            throw new IOException("Update download returned HTTP " + response.statusCode());
        }

        String actualHash = sha256(temp);
        if (!actualHash.equalsIgnoreCase(manifest.sha256())) {
            Files.deleteIfExists(temp);
            throw new IOException("Downloaded FaithfulBlocks JAR failed SHA-256 verification.");
        }

        Path ready = updateDir.resolve("faithfulblocks-" + safeVersion(manifest.version()) + ".jar");
        Files.move(temp, ready, StandardCopyOption.REPLACE_EXISTING);
        return ready;
    }

    private static void scheduleInstallAfterExit(Path downloadedJar, String newVersion) throws Exception {
        Path currentJar = currentModJar();
        if (currentJar == null || !currentJar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            FaithfulBlocks.LOGGER.warn("Auto-install skipped because FaithfulBlocks is running from a development/classes directory. Download remains at {}.", downloadedJar);
            return;
        }

        Path target = currentJar.resolveSibling("faithfulblocks-" + safeVersion(newVersion) + ".jar");
        long pid = ProcessHandle.current().pid();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path helperDir = downloadedJar.getParent();

        if (os.contains("win")) {
            Path script = helperDir.resolve("apply-faithfulblocks-update.cmd");
            String content = "@echo off\r\n"
                    + "set PID=" + pid + "\r\n"
                    + ":wait\r\n"
                    + "tasklist /FI \"PID eq %PID%\" 2>NUL | find \"%PID%\" >NUL\r\n"
                    + "if not errorlevel 1 (\r\n"
                    + "  timeout /t 1 /nobreak >NUL\r\n"
                    + "  goto wait\r\n"
                    + ")\r\n"
                    + "move /Y \"" + escapeCmd(downloadedJar.toAbsolutePath().toString()) + "\" \""
                    + escapeCmd(target.toAbsolutePath().toString()) + "\" >NUL\r\n"
                    + "del /Q \"" + escapeCmd(currentJar.toAbsolutePath().toString()) + "\" >NUL 2>NUL\r\n"
                    + "del /Q \"%~f0\"\r\n";
            Files.writeString(script, content, StandardCharsets.UTF_8);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> launchQuietly("cmd.exe", "/c", "start", "", "/min", script.toAbsolutePath().toString()), "FaithfulBlocks-Updater-Launcher"));
        } else {
            Path script = helperDir.resolve("apply-faithfulblocks-update.sh");
            String content = "#!/bin/sh\n"
                    + "PID=" + pid + "\n"
                    + "while kill -0 \"$PID\" 2>/dev/null; do sleep 1; done\n"
                    + "mv -f '" + escapeSh(downloadedJar.toAbsolutePath().toString()) + "' '" + escapeSh(target.toAbsolutePath().toString()) + "'\n"
                    + "rm -f '" + escapeSh(currentJar.toAbsolutePath().toString()) + "'\n"
                    + "rm -f -- \"$0\"\n";
            Files.writeString(script, content, StandardCharsets.UTF_8);
            script.toFile().setExecutable(true);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> launchQuietly("sh", script.toAbsolutePath().toString()), "FaithfulBlocks-Updater-Launcher"));
        }
    }

    private static Path currentModJar() {
        try {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(FaithfulBlocks.MOD_ID);
            if (container.isPresent()) {
                ModOrigin origin = container.get().getOrigin();
                if (origin.getKind() == ModOrigin.Kind.PATH) {
                    for (Path candidate : origin.getPaths()) {
                        Path normalized = candidate.toAbsolutePath().normalize();
                        if (Files.isRegularFile(normalized) && normalized.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                            return normalized;
                        }
                    }
                }
            }

            // Fallback for launchers/loaders that do not expose a simple PATH origin.
            URI location = FaithfulBlocks.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Path.of(location).toAbsolutePath().normalize();
            return Files.isRegularFile(path) ? path : null;
        } catch (Exception exception) {
            FaithfulBlocks.LOGGER.warn("Could not determine installed FaithfulBlocks JAR path.", exception);
            return null;
        }
    }

    private static Optional<String> versionOf(FabricLoader loader, String modId) {
        return loader.getModContainer(modId)
                .map(ModContainer::getMetadata)
                .map(metadata -> metadata.getVersion().getFriendlyString());
    }

    private static URI validatedHttpsUri(String value) throws IOException {
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid update URL.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("FaithfulBlocks updater only accepts HTTPS URLs.");
        }
        return uri;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String safeVersion(String version) {
        return version.replaceAll("[^A-Za-z0-9._+-]", "_");
    }

    private static String escapeCmd(String value) {
        return value.replace("%", "%%").replace("\"", "\"\"");
    }

    private static String escapeSh(String value) {
        return value.replace("'", "'\\''");
    }

    private static void launchQuietly(String... command) {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException exception) {
            FaithfulBlocks.LOGGER.error("Could not launch FaithfulBlocks external updater helper.", exception);
        }
    }

    private static void fail(String message, Exception exception) {
        status = new UpdateStatus(UpdateStatus.State.FAILED, "", "", message);
        FaithfulBlocks.LOGGER.warn(message, exception);
    }
}
