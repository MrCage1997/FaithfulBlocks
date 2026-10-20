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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A single architectural arch/cove block with stair-like corner joining.
 *
 * Placement follows vanilla stairs: horizontal facing comes from the player,
 * while the clicked face / click height selects a floor or upside-down variant.
 * Neighbouring arch blocks on the same half automatically become straight,
 * inner-corner or outer-corner pieces using the same five shape states as stairs.
 */
public final class ConnectingArchBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    private static final double[][] BANDS = {
            {0, 5, 4},
            {5, 9, 6},
            {9, 12, 9},
            {12, 14, 12},
            {14, 16, 16}
    };

    private static final Map<Half, Map<Direction, Map<StairsShape, VoxelShape>>> SHAPES = createShapes();

    public ConnectingArchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.TOP)
                .setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        double localY = context.getClickLocation().y - context.getClickedPos().getY();

        // Match vanilla stairs: top face / lower side half places the floor variant,
        // bottom face / upper side half places the upside-down (hanging) variant.
        Half half = clickedFace != Direction.DOWN
                && (clickedFace == Direction.UP || localY <= 0.5D)
                ? Half.BOTTOM
                : Half.TOP;

        BlockState placed = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(HALF, half)
                .setValue(SHAPE, StairsShape.STRAIGHT);
        return placed.setValue(SHAPE, getArchShape(placed, context.getLevel(), context.getClickedPos()));
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
        if (directionToNeighbour.getAxis() != Direction.Axis.Y) {
            return state.setValue(SHAPE, getArchShape(state, level, pos));
        }
        return state;
    }

    private StairsShape getArchShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);

        BlockState front = level.getBlockState(pos.relative(facing));
        if (isMatchingArch(front) && front.getValue(HALF) == state.getValue(HALF)) {
            Direction neighbourFacing = front.getValue(FACING);
            if (neighbourFacing.getAxis() != facing.getAxis()
                    && canTakeShape(state, level, pos, neighbourFacing.getOpposite())) {
                return neighbourFacing == facing.getCounterClockWise()
                        ? StairsShape.OUTER_LEFT
                        : StairsShape.OUTER_RIGHT;
            }
        }

        BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
        if (isMatchingArch(back) && back.getValue(HALF) == state.getValue(HALF)) {
            Direction neighbourFacing = back.getValue(FACING);
            if (neighbourFacing.getAxis() != facing.getAxis()
                    && canTakeShape(state, level, pos, neighbourFacing)) {
                return neighbourFacing == facing.getCounterClockWise()
                        ? StairsShape.INNER_LEFT
                        : StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction neighbourDirection) {
        BlockState neighbour = level.getBlockState(pos.relative(neighbourDirection));
        return !isMatchingArch(neighbour)
                || neighbour.getValue(HALF) != state.getValue(HALF)
                || neighbour.getValue(FACING) != state.getValue(FACING);
    }

    private boolean isMatchingArch(BlockState state) {
        return state.getBlock() instanceof ConnectingArchBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HALF)).get(state.getValue(FACING)).get(state.getValue(SHAPE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HALF)).get(state.getValue(FACING)).get(state.getValue(SHAPE));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static Map<Half, Map<Direction, Map<StairsShape, VoxelShape>>> createShapes() {
        Map<Half, Map<Direction, Map<StairsShape, VoxelShape>>> result = new EnumMap<>(Half.class);

        for (Half half : Half.values()) {
            Map<Direction, Map<StairsShape, VoxelShape>> byFacing = new EnumMap<>(Direction.class);
            for (Direction facing : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
                Map<StairsShape, VoxelShape> byShape = new EnumMap<>(StairsShape.class);
                for (StairsShape shape : StairsShape.values()) {
                    VoxelShape combined = Shapes.empty();
                    for (double[] band : BANDS) {
                        double y0 = band[0];
                        double y1 = band[1];
                        if (half == Half.BOTTOM) {
                            y0 = 16 - band[1];
                            y1 = 16 - band[0];
                        }
                        combined = Shapes.or(combined, createBand(facing, shape, y0, y1, band[2]));
                    }
                    byShape.put(shape, combined);
                }
                byFacing.put(facing, byShape);
            }
            result.put(half, byFacing);
        }

        return result;
    }

    private static VoxelShape createBand(Direction facing, StairsShape shape, double y0, double y1, double width) {
        return switch (shape) {
            case OUTER_LEFT -> cornerBand(facing, facing.getCounterClockWise(), y0, y1, width);
            case OUTER_RIGHT -> cornerBand(facing, facing.getClockWise(), y0, y1, width);
            case INNER_LEFT -> Shapes.or(
                    edgeBand(facing, y0, y1, width),
                    edgeBand(facing.getCounterClockWise(), y0, y1, width)
            );
            case INNER_RIGHT -> Shapes.or(
                    edgeBand(facing, y0, y1, width),
                    edgeBand(facing.getClockWise(), y0, y1, width)
            );
            default -> edgeBand(facing, y0, y1, width);
        };
    }

    private static VoxelShape edgeBand(Direction edge, double y0, double y1, double width) {
        return switch (edge) {
            case NORTH -> Block.box(0, y0, 0, 16, y1, width);
            case SOUTH -> Block.box(0, y0, 16 - width, 16, y1, 16);
            case WEST -> Block.box(0, y0, 0, width, y1, 16);
            case EAST -> Block.box(16 - width, y0, 0, 16, y1, 16);
            default -> Block.box(0, y0, 0, 16, y1, 16);
        };
    }

    private static VoxelShape cornerBand(Direction first, Direction second, double y0, double y1, double width) {
        double minX = 0;
        double maxX = 16;
        double minZ = 0;
        double maxZ = 16;

        Direction[] edges = { first, second };
        for (Direction edge : edges) {
            switch (edge) {
                case NORTH -> maxZ = Math.min(maxZ, width);
                case SOUTH -> minZ = Math.max(minZ, 16 - width);
                case WEST -> maxX = Math.min(maxX, width);
                case EAST -> minX = Math.max(minX, 16 - width);
                default -> { }
            }
        }

        return Block.box(minX, y0, minZ, maxX, y1, maxZ);
    }
}
