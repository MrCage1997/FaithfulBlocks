package de.faithfulblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Modular vertical window frame.
 *
 * New placements behave like panes: they are always vertical, inherit the plane
 * of adjacent frames when extending an existing window and automatically narrow
 * shared borders so neighbouring frames read as one continuous construction.
 * Legacy UP/DOWN states are retained only so old test worlds keep loading.
 */
public final class WindowFrameBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<FrameGlazing> GLAZING = EnumProperty.create("glazing", FrameGlazing.class);
    public static final BooleanProperty NEGATIVE_CONNECTED = BooleanProperty.create("negative_connected");
    public static final BooleanProperty POSITIVE_CONNECTED = BooleanProperty.create("positive_connected");
    public static final BooleanProperty DOWN_CONNECTED = BooleanProperty.create("down_connected");
    public static final BooleanProperty UP_CONNECTED = BooleanProperty.create("up_connected");

    // Legacy pre-1.6 shape, kept for old UP/DOWN block states only.
    private static final double[][] LEGACY_BOXES = {
            { 0, 0, 0, 16, 4, 2 },
            { 0, 0, 14, 16, 4, 16 },
            { 0, 0, 2, 2, 4, 14 },
            { 14, 0, 2, 16, 4, 14 }
    };
    private static final VoxelShape LEGACY_UP = DirectionalShapeUtils.create(Direction.UP, LEGACY_BOXES);
    private static final VoxelShape LEGACY_DOWN = DirectionalShapeUtils.create(Direction.DOWN, LEGACY_BOXES);

    // Two pane planes (normal X or normal Z), each with 16 connection combinations.
    private static final VoxelShape[][] FRAME_SHAPES = createFrameShapes();
    private static final VoxelShape GLASS_X = Block.box(7.5, 2, 2, 8.5, 14, 14);
    private static final VoxelShape GLASS_Z = Block.box(2, 2, 7.5, 14, 14, 8.5);

    // Old glazing geometry used only for legacy UP/DOWN states.
    private static final double[][] LEGACY_GLASS_BOXES = { { 2, 1.5, 2, 14, 2.5, 14 } };
    private static final Map<Direction, VoxelShape> LEGACY_GLAZED_SHAPES = createLegacyGlazedShapes();

    public WindowFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(GLAZING, FrameGlazing.NONE)
                .setValue(NEGATIVE_CONNECTED, false)
                .setValue(POSITIVE_CONNECTED, false)
                .setValue(DOWN_CONNECTED, false)
                .setValue(UP_CONNECTED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = choosePlacementFacing(context);
        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(GLAZING, FrameGlazing.NONE);
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    /** Always choose a vertical plane; floor/ceiling clicks use player direction. */
    private Direction choosePlacementFacing(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction preferred = clicked.getAxis() == Direction.Axis.Y
                ? context.getHorizontalDirection()
                : clicked;

        Direction inherited = findNeighbourFacing(context.getLevel(), context.getClickedPos(), preferred);
        return inherited != null ? inherited : preferred;
    }

    /**
     * Extending a window above/below or along its plane inherits the existing
     * frame orientation. Frames directly in front/behind are deliberately ignored.
     */
    private static Direction findNeighbourFacing(BlockGetter level, BlockPos pos, Direction preferred) {
        Direction fallback = null;
        for (Direction offset : new Direction[]{
                Direction.DOWN, Direction.UP,
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        }) {
            BlockState neighbour = level.getBlockState(pos.relative(offset));
            if (!(neighbour.getBlock() instanceof WindowFrameBlock)) {
                continue;
            }
            Direction neighbourFacing = neighbour.getValue(FACING);
            if (neighbourFacing.getAxis() == Direction.Axis.Y) {
                continue;
            }
            // Only frames next to each other in the pane plane should snap together.
            if (offset.getAxis() == neighbourFacing.getAxis()) {
                continue;
            }
            if (neighbourFacing.getAxis() == preferred.getAxis()) {
                return neighbourFacing;
            }
            if (fallback == null) {
                fallback = neighbourFacing;
            }
        }
        return fallback;
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
        if (state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            return state;
        }
        return withConnections(state, level, pos);
    }

    private static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis() == Direction.Axis.Y) {
            return state
                    .setValue(NEGATIVE_CONNECTED, false)
                    .setValue(POSITIVE_CONNECTED, false)
                    .setValue(DOWN_CONNECTED, false)
                    .setValue(UP_CONNECTED, false);
        }

        Direction negative = negativeInPlane(facing);
        Direction positive = negative.getOpposite();
        return state
                .setValue(NEGATIVE_CONNECTED, connects(level.getBlockState(pos.relative(negative)), facing))
                .setValue(POSITIVE_CONNECTED, connects(level.getBlockState(pos.relative(positive)), facing))
                .setValue(DOWN_CONNECTED, connects(level.getBlockState(pos.below()), facing))
                .setValue(UP_CONNECTED, connects(level.getBlockState(pos.above()), facing));
    }

    private static boolean connects(BlockState neighbour, Direction facing) {
        if (!(neighbour.getBlock() instanceof WindowFrameBlock)) {
            return false;
        }
        Direction other = neighbour.getValue(FACING);
        return other.getAxis() != Direction.Axis.Y && other.getAxis() == facing.getAxis();
    }

    private static Direction negativeInPlane(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GLAZING, NEGATIVE_CONNECTED, POSITIVE_CONNECTED, DOWN_CONNECTED, UP_CONNECTED);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        FrameGlazing glazing = FrameGlazing.fromStack(stack);
        if (glazing == FrameGlazing.NONE) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!player.getAbilities().mayBuild || state.getValue(GLAZING) != FrameGlazing.NONE) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(pos, state.setValue(GLAZING, glazing), Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static VoxelShape stateShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.UP || facing == Direction.DOWN) {
            if (state.getValue(GLAZING) == FrameGlazing.NONE) {
                return facing == Direction.UP ? LEGACY_UP : LEGACY_DOWN;
            }
            return LEGACY_GLAZED_SHAPES.get(facing);
        }

        int axisIndex = facing.getAxis() == Direction.Axis.X ? 0 : 1;
        VoxelShape frame = FRAME_SHAPES[axisIndex][connectionIndex(state)];
        if (state.getValue(GLAZING) == FrameGlazing.NONE) {
            return frame;
        }
        return Shapes.or(frame, facing.getAxis() == Direction.Axis.X ? GLASS_X : GLASS_Z);
    }

    private static int connectionIndex(BlockState state) {
        int flags = 0;
        if (state.getValue(NEGATIVE_CONNECTED)) flags |= 1;
        if (state.getValue(POSITIVE_CONNECTED)) flags |= 2;
        if (state.getValue(DOWN_CONNECTED)) flags |= 4;
        if (state.getValue(UP_CONNECTED)) flags |= 8;
        return flags;
    }

    private static VoxelShape[][] createFrameShapes() {
        VoxelShape[][] result = new VoxelShape[2][16];
        for (int flags = 0; flags < 16; flags++) {
            int negativeWidth = (flags & 1) != 0 ? 1 : 2;
            int positiveWidth = (flags & 2) != 0 ? 1 : 2;
            int downWidth = (flags & 4) != 0 ? 1 : 2;
            int upWidth = (flags & 8) != 0 ? 1 : 2;

            // Normal X: pane lies in Y/Z, negative side is NORTH.
            result[0][flags] = Shapes.or(
                    Block.box(6, 0, 0, 10, downWidth, 16),
                    Block.box(6, 16 - upWidth, 0, 10, 16, 16),
                    Block.box(6, downWidth, 0, 10, 16 - upWidth, negativeWidth),
                    Block.box(6, downWidth, 16 - positiveWidth, 10, 16 - upWidth, 16)
            );

            // Normal Z: pane lies in X/Y, negative side is WEST.
            result[1][flags] = Shapes.or(
                    Block.box(0, 0, 6, 16, downWidth, 10),
                    Block.box(0, 16 - upWidth, 6, 16, 16, 10),
                    Block.box(0, downWidth, 6, negativeWidth, 16 - upWidth, 10),
                    Block.box(16 - positiveWidth, downWidth, 6, 16, 16 - upWidth, 10)
            );
        }
        return result;
    }

    private static Map<Direction, VoxelShape> createLegacyGlazedShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, Shapes.or(LEGACY_UP, DirectionalShapeUtils.create(Direction.UP, LEGACY_GLASS_BOXES)));
        shapes.put(Direction.DOWN, Shapes.or(LEGACY_DOWN, DirectionalShapeUtils.create(Direction.DOWN, LEGACY_GLASS_BOXES)));
        return shapes;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return stateShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return stateShape(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return transform(state, rotation::rotate);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return transform(state, mirror::mirror);
    }

    private static BlockState transform(BlockState state, Function<Direction, Direction> transform) {
        Direction oldFacing = state.getValue(FACING);
        Direction newFacing = transform.apply(oldFacing);
        if (oldFacing.getAxis() == Direction.Axis.Y || newFacing.getAxis() == Direction.Axis.Y) {
            return state.setValue(FACING, newFacing);
        }

        Direction oldNegative = negativeInPlane(oldFacing);
        Direction transformedNegative = transform.apply(oldNegative);
        Direction newNegative = negativeInPlane(newFacing);

        boolean negative = state.getValue(NEGATIVE_CONNECTED);
        boolean positive = state.getValue(POSITIVE_CONNECTED);
        if (transformedNegative != newNegative) {
            boolean swap = negative;
            negative = positive;
            positive = swap;
        }

        return state
                .setValue(FACING, newFacing)
                .setValue(NEGATIVE_CONNECTED, negative)
                .setValue(POSITIVE_CONNECTED, positive);
    }
}
