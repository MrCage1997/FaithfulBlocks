package de.faithfulblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Convex rounded outside corner. The model is intentionally stepped so it
 * remains visibly Minecraft-like while reading as a much softer 90 degree edge.
 */
public final class RoundedCornerBlock extends AbstractCornerBlock {
    private static final VoxelShape NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 16, 2),
            Block.box(0, 0, 2, 16, 16, 4),
            Block.box(0, 0, 4, 15, 16, 6),
            Block.box(0, 0, 6, 15, 16, 8),
            Block.box(0, 0, 8, 14, 16, 10),
            Block.box(0, 0, 10, 12, 16, 12),
            Block.box(0, 0, 12, 10, 16, 14),
            Block.box(0, 0, 14, 6, 16, 16)
    );

    private static final VoxelShape EAST = Shapes.or(
            Block.box(14, 0, 0, 16, 16, 16),
            Block.box(12, 0, 0, 14, 16, 16),
            Block.box(10, 0, 0, 12, 16, 15),
            Block.box(8, 0, 0, 10, 16, 15),
            Block.box(6, 0, 0, 8, 16, 14),
            Block.box(4, 0, 0, 6, 16, 12),
            Block.box(2, 0, 0, 4, 16, 10),
            Block.box(0, 0, 0, 2, 16, 6)
    );

    private static final VoxelShape SOUTH = Shapes.or(
            Block.box(0, 0, 14, 16, 16, 16),
            Block.box(0, 0, 12, 16, 16, 14),
            Block.box(1, 0, 10, 16, 16, 12),
            Block.box(1, 0, 8, 16, 16, 10),
            Block.box(2, 0, 6, 16, 16, 8),
            Block.box(4, 0, 4, 16, 16, 6),
            Block.box(6, 0, 2, 16, 16, 4),
            Block.box(10, 0, 0, 16, 16, 2)
    );

    private static final VoxelShape WEST = Shapes.or(
            Block.box(0, 0, 0, 2, 16, 16),
            Block.box(2, 0, 0, 4, 16, 16),
            Block.box(4, 0, 1, 6, 16, 16),
            Block.box(6, 0, 1, 8, 16, 16),
            Block.box(8, 0, 2, 10, 16, 16),
            Block.box(10, 0, 4, 12, 16, 16),
            Block.box(12, 0, 6, 14, 16, 16),
            Block.box(14, 0, 10, 16, 16, 16)
    );

    public RoundedCornerBlock(Properties properties) {
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
