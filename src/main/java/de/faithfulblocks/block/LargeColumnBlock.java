package de.faithfulblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A large chamfered column/beam that can be placed on X, Y or Z.
 *
 * Straight large columns meet exactly at their block boundary. When one end
 * touches a perpendicular large column, a short central bridge closes only
 * the one-model-unit gap to the neighbour's outer profile. The bridge does not
 * overlap neighbour geometry, preventing the Z-fighting seen in earlier
 * narrow-column prototypes.
 */
public final class LargeColumnBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty NEGATIVE_CONNECTED = BooleanProperty.create("negative_connected");
    public static final BooleanProperty POSITIVE_CONNECTED = BooleanProperty.create("positive_connected");

    private static final VoxelShape[][] SHAPES = createShapes();

    public LargeColumnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(NEGATIVE_CONNECTED, false)
                .setValue(POSITIVE_CONNECTED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockState state = defaultBlockState().setValue(AXIS, axis);
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        Direction.Axis axis = state.getValue(AXIS);
        if (directionToNeighbour.getAxis() != axis) {
            return state;
        }

        boolean connected = isPerpendicularLargeColumn(neighbourState, axis);
        if (directionToNeighbour == negativeDirection(axis)) {
            return state.setValue(NEGATIVE_CONNECTED, connected);
        }
        return state.setValue(POSITIVE_CONNECTED, connected);
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean negative = isPerpendicularLargeColumn(
                level.getBlockState(pos.relative(negativeDirection(axis))), axis);
        boolean positive = isPerpendicularLargeColumn(
                level.getBlockState(pos.relative(positiveDirection(axis))), axis);
        return state
                .setValue(NEGATIVE_CONNECTED, negative)
                .setValue(POSITIVE_CONNECTED, positive);
    }

    private static boolean isPerpendicularLargeColumn(BlockState state, Direction.Axis ownAxis) {
        return state.getBlock() instanceof LargeColumnBlock
                && state.getValue(AXIS) != ownAxis;
    }

    private static Direction negativeDirection(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.WEST;
            case Y -> Direction.DOWN;
            case Z -> Direction.NORTH;
        };
    }

    private static Direction positiveDirection(Direction.Axis axis) {
        return negativeDirection(axis).getOpposite();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, NEGATIVE_CONNECTED, POSITIVE_CONNECTED);
    }

    private static int shapeIndex(BlockState state) {
        int index = 0;
        if (state.getValue(NEGATIVE_CONNECTED)) index |= 1;
        if (state.getValue(POSITIVE_CONNECTED)) index |= 2;
        return index;
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[axisIndex(state.getValue(AXIS))][shapeIndex(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[axisIndex(state.getValue(AXIS))][shapeIndex(state)];
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] result = new VoxelShape[3][4];
        Direction.Axis[] axes = { Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z };
        for (int axisIndex = 0; axisIndex < axes.length; axisIndex++) {
            for (int flags = 0; flags < 4; flags++) {
                boolean negative = (flags & 1) != 0;
                boolean positive = (flags & 2) != 0;
                result[axisIndex][flags] = createShape(axes[axisIndex], negative, positive);
            }
        }
        return result;
    }

    private static VoxelShape createShape(Direction.Axis axis, boolean negative, boolean positive) {
        VoxelShape base = switch (axis) {
            case X -> Shapes.or(
                    Block.box(0, 3, 1, 16, 13, 15),
                    Block.box(0, 1, 3, 16, 15, 13)
            );
            case Y -> Shapes.or(
                    Block.box(3, 0, 1, 13, 16, 15),
                    Block.box(1, 0, 3, 15, 16, 13)
            );
            case Z -> Shapes.or(
                    Block.box(3, 1, 0, 13, 15, 16),
                    Block.box(1, 3, 0, 15, 13, 16)
            );
        };

        // A perpendicular large column starts one model unit inside its block.
        // Bridge precisely that one unit with the central 10x10 profile.
        if (negative) {
            base = Shapes.or(base, switch (axis) {
                case X -> Block.box(-1, 3, 3, 0, 13, 13);
                case Y -> Block.box(3, -1, 3, 13, 0, 13);
                case Z -> Block.box(3, 3, -1, 13, 13, 0);
            });
        }
        if (positive) {
            base = Shapes.or(base, switch (axis) {
                case X -> Block.box(16, 3, 3, 17, 13, 13);
                case Y -> Block.box(3, 16, 3, 13, 17, 13);
                case Z -> Block.box(3, 3, 16, 13, 13, 17);
            });
        }
        return base;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return transformDirections(state, rotation::rotate);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return transformDirections(state, mirror::mirror);
    }

    private BlockState transformDirections(BlockState state, java.util.function.Function<Direction, Direction> transform) {
        Direction.Axis oldAxis = state.getValue(AXIS);
        Direction oldNegative = negativeDirection(oldAxis);
        Direction oldPositive = positiveDirection(oldAxis);

        Direction transformedNegative = transform.apply(oldNegative);
        Direction transformedPositive = transform.apply(oldPositive);
        Direction.Axis newAxis = transformedPositive.getAxis();
        Direction canonicalNegative = negativeDirection(newAxis);

        boolean oldNegativeConnected = state.getValue(NEGATIVE_CONNECTED);
        boolean oldPositiveConnected = state.getValue(POSITIVE_CONNECTED);

        boolean newNegativeConnected;
        boolean newPositiveConnected;
        if (transformedNegative == canonicalNegative) {
            newNegativeConnected = oldNegativeConnected;
            newPositiveConnected = oldPositiveConnected;
        } else {
            newNegativeConnected = oldPositiveConnected;
            newPositiveConnected = oldNegativeConnected;
        }

        return state
                .setValue(AXIS, newAxis)
                .setValue(NEGATIVE_CONNECTED, newNegativeConnected)
                .setValue(POSITIVE_CONNECTED, newPositiveConnected);
    }
}
