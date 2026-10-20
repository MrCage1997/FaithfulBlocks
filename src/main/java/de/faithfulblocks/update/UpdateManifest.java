package de.faithfulblocks.update;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Properties;

public record UpdateManifest(
        boolean enabled,
        String version,
        String minecraft,
        String fabricLoaderMin,
        String fabricApiMin,
        String downloadUrl,
        String sha256
) {
    public static UpdateManifest parse(String body) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(body));

        return new UpdateManifest(
                Boolean.parseBoolean(properties.getProperty("enabled", "true").trim()),
                required(properties, "version"),
                required(properties, "minecraft"),
                properties.getProperty("fabric_loader_min", "").trim(),
                properties.getProperty("fabric_api_min", "").trim(),
                required(properties, "download_url"),
                normalizeHash(required(properties, "sha256"))
        );
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IOException("Missing update manifest property: " + key);
        }
        return value;
    }

    private static String normalizeHash(String value) throws IOException {
        String normalized = value.replace(" ", "").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IOException("Invalid SHA-256 in update manifest.");
        }
        return normalized;
    }
}
