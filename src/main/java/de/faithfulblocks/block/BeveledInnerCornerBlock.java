package de.faithfulblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** L-shaped quarter-thick wall return with a pixel-stepped bevel inside the corner. */
public final class BeveledInnerCornerBlock extends AbstractCornerBlock {
    private static final double[][] BOXES = {
            { 0, 0, 0, 16, 16, 4 },
            { 0, 0, 4, 8, 16, 5 },
            { 0, 0, 5, 7, 16, 6 },
            { 0, 0, 6, 6, 16, 7 },
            { 0, 0, 7, 5, 16, 8 },
            { 0, 0, 8, 4, 16, 16 }
    };

    private static final VoxelShape NORTH = create(0);
    private static final VoxelShape EAST = create(1);
    private static final VoxelShape SOUTH = create(2);
    private static final VoxelShape WEST = create(3);

    public BeveledInnerCornerBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape create(int turns) {
        VoxelShape result = Shapes.empty();
        for (double[] box : BOXES) {
            double x0 = box[0], z0 = box[2], x1 = box[3], z1 = box[5];
            for (int turn = 0; turn < turns; turn++) {
                double nextX0 = 16 - z1, nextX1 = 16 - z0;
                z0 = x0;
                z1 = x1;
                x0 = nextX0;
                x1 = nextX1;
            }
            result = Shapes.or(result, Block.box(x0, box[1], z0, x1, box[4], z1));
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
