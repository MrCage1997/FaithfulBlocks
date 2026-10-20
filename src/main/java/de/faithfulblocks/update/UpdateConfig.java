package de.faithfulblocks.update;

import de.faithfulblocks.FaithfulBlocks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record UpdateConfig(UpdateMode mode, String manifestUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
    private static final String FILE_NAME = "faithfulblocks-update.properties";
    public static final String DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/MrCage1997/FaithfulBlocks/main/update.properties";

    public static UpdateConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException exception) {
                FaithfulBlocks.LOGGER.warn("Could not read FaithfulBlocks update configuration at {}.", path, exception);
            }
        } else {
            properties.setProperty("mode", "notify");
            properties.setProperty("manifest_url", DEFAULT_MANIFEST_URL);
            properties.setProperty("connect_timeout_seconds", "5");
            properties.setProperty("request_timeout_seconds", "15");
            saveDefaults(path, properties);
        }

        UpdateMode mode = UpdateMode.parse(properties.getProperty("mode", "notify"));
        String manifestUrl = properties.getProperty("manifest_url", DEFAULT_MANIFEST_URL).trim();
        if (manifestUrl.isBlank()) {
            // Migrate the blank URL written by the 1.6.0-rc5 prototype.
            manifestUrl = DEFAULT_MANIFEST_URL;
            properties.setProperty("manifest_url", manifestUrl);
            saveDefaults(path, properties);
        }
        int connectTimeout = parsePositiveInt(properties.getProperty("connect_timeout_seconds"), 5);
        int requestTimeout = parsePositiveInt(properties.getProperty("request_timeout_seconds"), 15);
        return new UpdateConfig(mode, manifestUrl, connectTimeout, requestTimeout);
    }

    private static void saveDefaults(Path path, Properties properties) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out,
                        "FaithfulBlocks updater\n" +
                        "mode = off | notify | auto_download\n" +
                        "manifest_url = HTTPS URL to update.properties hosted on GitHub/Modrinth/etc.");
            }
        } catch (IOException exception) {
            FaithfulBlocks.LOGGER.warn("Could not create default FaithfulBlocks update configuration at {}.", path, exception);
        }
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
