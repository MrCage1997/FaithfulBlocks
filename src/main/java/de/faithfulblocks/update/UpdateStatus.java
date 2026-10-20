package de.faithfulblocks.update;

public record UpdateStatus(State state, String currentVersion, String availableVersion, String message) {
    public enum State {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        DOWNLOADED,
        INCOMPATIBLE,
        FAILED
    }

    public static UpdateStatus idle() {
        return new UpdateStatus(State.IDLE, "", "", "");
    }
}
