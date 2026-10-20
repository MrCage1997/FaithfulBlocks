package de.faithfulblocks.architecture;

public enum ArchitectureShape {
    ROUNDED_CORNER(0, "menu.faithfulblocks.shape.rounded_corner"),
    NARROW_COLUMN(1, "menu.faithfulblocks.shape.narrow_column"),
    ROUNDED_INNER_CORNER(2, "menu.faithfulblocks.shape.rounded_inner_corner"),
    PEDESTAL(3, "menu.faithfulblocks.shape.pedestal"),
    CORNICE(4, "menu.faithfulblocks.shape.cornice"),
    ARCH(5, "menu.faithfulblocks.shape.arch"),
    WALL_CAP(6, "menu.faithfulblocks.shape.wall_cap"),
    WALL_END(7, "menu.faithfulblocks.shape.wall_end"),
    PILASTER(8, "menu.faithfulblocks.shape.pilaster"),
    LARGE_COLUMN(9, "menu.faithfulblocks.shape.large_column"),
    WALL_PEDESTAL(10, "menu.faithfulblocks.shape.wall_pedestal"),
    WALL_CORNICE(11, "menu.faithfulblocks.shape.wall_cornice"),
    LARGE_COLUMN_PEDESTAL(12, "menu.faithfulblocks.shape.large_column_pedestal"),
    BEVELED_CORNER(13, "menu.faithfulblocks.shape.beveled_corner"),
    FACADE_OFFSET(14, "menu.faithfulblocks.shape.facade_offset"),
    BEVELED_INNER_CORNER(15, "menu.faithfulblocks.shape.beveled_inner_corner"),
    WINDOW_FRAME(16, "menu.faithfulblocks.shape.window_frame"),
    STEPPED_SLOPE(17, "menu.faithfulblocks.shape.stepped_slope"),
    RETURN_ORIGINAL(18, "menu.faithfulblocks.shape.return_original");

    private final int buttonId;
    private final String translationKey;

    ArchitectureShape(int buttonId, String translationKey) { this.buttonId = buttonId; this.translationKey = translationKey; }
    public int buttonId() { return buttonId; }
    public String translationKey() { return translationKey; }
    public static ArchitectureShape byButtonId(int buttonId) {
        for (ArchitectureShape shape : values()) if (shape.buttonId == buttonId) return shape;
        return null;
    }
}
