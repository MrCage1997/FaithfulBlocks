package de.faithfulblocks.block;

/** A wall-base/plinth trim that automatically joins around 90-degree corners. */
public final class WallPedestalBlock extends AbstractConnectingTrimBlock {
    private static final double[][] BANDS = {
            {0, 4, 9},
            {4, 8, 7},
            {8, 16, 5}
    };

    public WallPedestalBlock(Properties properties) {
        super(properties, BANDS);
    }
}
