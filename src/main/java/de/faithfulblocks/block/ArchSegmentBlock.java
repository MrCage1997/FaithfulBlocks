package de.faithfulblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One half of a two-block-wide stepped arch. Place LEFT and RIGHT next to each
 * other with matching facing values to form the complete arch.
 */
public final class ArchSegmentBlock extends Block {
    public enum Side { LEFT, RIGHT }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Side side;

    private static final VoxelShape LEFT_NORTH = makeX(false);
    private static final VoxelShape LEFT_EAST = makeZ(false);
    private static final VoxelShape LEFT_SOUTH = makeX(true);
    private static final VoxelShape LEFT_WEST = makeZ(true);

    private static final VoxelShape RIGHT_NORTH = makeX(true);
    private static final VoxelShape RIGHT_EAST = makeZ(true);
    private static final VoxelShape RIGHT_SOUTH = makeX(false);
    private static final VoxelShape RIGHT_WEST = makeZ(false);

    public ArchSegmentBlock(Properties properties, Side side) {
        super(properties);
        this.side = side;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape makeX(boolean highSide) {
        return Shapes.or(
                xBand(0, 5, 4, highSide),
                xBand(5, 9, 6, highSide),
                xBand(9, 12, 9, highSide),
                xBand(12, 14, 12, highSide),
                Block.box(0, 14, 0, 16, 16, 16)
        );
    }

    private static VoxelShape makeZ(boolean highSide) {
        return Shapes.or(
                zBand(0, 5, 4, highSide),
                zBand(5, 9, 6, highSide),
                zBand(9, 12, 9, highSide),
                zBand(12, 14, 12, highSide),
                Block.box(0, 14, 0, 16, 16, 16)
        );
    }

    private static VoxelShape xBand(double y0, double y1, double width, boolean highSide) {
        return highSide
                ? Block.box(16 - width, y0, 0, 16, y1, 16)
                : Block.box(0, y0, 0, width, y1, 16);
    }

    private static VoxelShape zBand(double y0, double y1, double width, boolean highSide) {
        return highSide
                ? Block.box(0, y0, 16 - width, 16, y1, 16)
                : Block.box(0, y0, 0, 16, y1, width);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private VoxelShape shapeFor(Direction facing) {
        if (side == Side.LEFT) {
            return switch (facing) {
                case EAST -> LEFT_EAST;
                case SOUTH -> LEFT_SOUTH;
                case WEST -> LEFT_WEST;
                default -> LEFT_NORTH;
            };
        }

        return switch (facing) {
            case EAST -> RIGHT_EAST;
            case SOUTH -> RIGHT_SOUTH;
            case WEST -> RIGHT_WEST;
            default -> RIGHT_NORTH;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
