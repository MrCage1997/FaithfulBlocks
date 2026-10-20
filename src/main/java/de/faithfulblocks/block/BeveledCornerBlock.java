package de.faithfulblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pixel-stepped 45-degree outside corner, matching the resource model exactly. */
public final class BeveledCornerBlock extends AbstractCornerBlock {
    private static final VoxelShape NORTH = create(0);
    private static final VoxelShape EAST = create(1);
    private static final VoxelShape SOUTH = create(2);
    private static final VoxelShape WEST = create(3);

    public BeveledCornerBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape create(int turns) {
        VoxelShape result = Shapes.empty();
        for (int z = 0; z < 16; z++) {
            double x0 = 0, z0 = z, x1 = 16 - z, z1 = z + 1;
            for (int turn = 0; turn < turns; turn++) {
                double nextX0 = 16 - z1, nextX1 = 16 - z0;
                z0 = x0;
                z1 = x1;
                x0 = nextX0;
                x1 = nextX1;
            }
            result = Shapes.or(result, Block.box(x0, 0, z0, x1, 16, z1));
        }
        return result;
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

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        // Facing denotes an occupied quarter, not a face normal.
        Direction mirrored = switch (mirror) {
            case LEFT_RIGHT -> switch (facing) {
                case NORTH -> Direction.WEST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                default -> Direction.NORTH;
            };
            case FRONT_BACK -> switch (facing) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.WEST;
                default -> Direction.SOUTH;
            };
            default -> facing;
        };
        return state.setValue(FACING, mirrored);
    }
}
