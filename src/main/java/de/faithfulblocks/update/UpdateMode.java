package de.faithfulblocks.update;

public enum UpdateMode {
    OFF,
    NOTIFY,
    AUTO_DOWNLOAD;

    public static UpdateMode parse(String value) {
        if (value == null) {
            return NOTIFY;
        }

        return switch (value.trim().toLowerCase()) {
            case "off", "false", "disabled" -> OFF;
            case "auto", "auto_download", "automatic" -> AUTO_DOWNLOAD;
            default -> NOTIFY;
        };
    }
}
