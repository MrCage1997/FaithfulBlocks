package de.faithfulblocks.block;

/** A stepped wall cornice that automatically joins around 90-degree corners. */
public final class WallCorniceBlock extends AbstractConnectingTrimBlock {
    private static final double[][] BANDS = {
            {0, 8, 5},
            {8, 12, 7},
            {12, 16, 9}
    };

    public WallCorniceBlock(Properties properties) {
        super(properties, BANDS);
    }
}
