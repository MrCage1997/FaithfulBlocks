package de.faithfulblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Concave counterpart to the rounded outside corner. It forms a small stepped
 * quarter-ring/wedge used to soften an inward 90 degree architectural corner.
 */
public final class RoundedInnerCornerBlock extends AbstractCornerBlock {
    private static final VoxelShape NORTH = Shapes.or(
            Block.box(0, 0, 0, 10, 16, 2),
            Block.box(0, 0, 2, 6, 16, 4),
            Block.box(0, 0, 4, 4, 16, 6),
            Block.box(0, 0, 6, 2, 16, 8),
            Block.box(0, 0, 8, 1, 16, 12)
    );

    private static final VoxelShape EAST = Shapes.or(
            Block.box(14, 0, 0, 16, 16, 10),
            Block.box(12, 0, 0, 14, 16, 6),
            Block.box(10, 0, 0, 12, 16, 4),
            Block.box(8, 0, 0, 10, 16, 2),
            Block.box(4, 0, 0, 8, 16, 1)
    );

    private static final VoxelShape SOUTH = Shapes.or(
            Block.box(6, 0, 14, 16, 16, 16),
            Block.box(10, 0, 12, 16, 16, 14),
            Block.box(12, 0, 10, 16, 16, 12),
            Block.box(14, 0, 8, 16, 16, 10),
            Block.box(15, 0, 4, 16, 16, 8)
    );

    private static final VoxelShape WEST = Shapes.or(
            Block.box(0, 0, 6, 2, 16, 16),
            Block.box(2, 0, 10, 4, 16, 16),
            Block.box(4, 0, 12, 6, 16, 16),
            Block.box(6, 0, 14, 8, 16, 16),
            Block.box(8, 0, 15, 12, 16, 16)
    );

    public RoundedInnerCornerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }
}
