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
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A Minecraft-style architectural 45-degree slope built from eight 2/16 steps.
 *
 * The low edge faces the player when placed, while neighbouring slope blocks on
 * the same half automatically form the same straight / inner / outer corner
 * states as vanilla stairs. FACING intentionally stores the LOW edge direction;
 * the virtual stair-facing used for corner detection is therefore the opposite
 * direction (the direction in which the slope rises).
 */
public final class SteppedSlopeBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

    private static final Map<Half, Map<Direction, Map<StairsShape, VoxelShape>>> SHAPES = createShapes();

    public SteppedSlopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM)
                .setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        double localY = context.getClickLocation().y - context.getClickedPos().getY();
        Half half = clickedFace != Direction.DOWN
                && (clickedFace == Direction.UP || localY <= 0.5D)
                ? Half.BOTTOM
                : Half.TOP;

        BlockState placed = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, half)
                .setValue(SHAPE, StairsShape.STRAIGHT);

        return placed.setValue(SHAPE, getSlopeShape(placed, context.getLevel(), context.getClickedPos()));
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
            return state.setValue(SHAPE, getSlopeShape(state, level, pos));
        }
        return state;
    }

    /**
     * Vanilla-stair corner detection adapted to this block's low-edge FACING.
     * The virtual facing points uphill and is what vanilla stairs would store.
     */
    private StairsShape getSlopeShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction uphill = state.getValue(FACING).getOpposite();

        BlockState front = level.getBlockState(pos.relative(uphill));
        if (isMatchingSlope(front) && front.getValue(HALF) == state.getValue(HALF)) {
            Direction neighbourUphill = front.getValue(FACING).getOpposite();
            if (neighbourUphill.getAxis() != uphill.getAxis()
                    && canTakeShape(state, level, pos, neighbourUphill.getOpposite())) {
                return neighbourUphill == uphill.getCounterClockWise()
                        ? StairsShape.OUTER_LEFT
                        : StairsShape.OUTER_RIGHT;
            }
        }

        BlockState back = level.getBlockState(pos.relative(uphill.getOpposite()));
        if (isMatchingSlope(back) && back.getValue(HALF) == state.getValue(HALF)) {
            Direction neighbourUphill = back.getValue(FACING).getOpposite();
            if (neighbourUphill.getAxis() != uphill.getAxis()
                    && canTakeShape(state, level, pos, neighbourUphill)) {
                return neighbourUphill == uphill.getCounterClockWise()
                        ? StairsShape.INNER_LEFT
                        : StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction neighbourDirection) {
        BlockState neighbour = level.getBlockState(pos.relative(neighbourDirection));
        return !isMatchingSlope(neighbour)
                || neighbour.getValue(HALF) != state.getValue(HALF)
                || neighbour.getValue(FACING) != state.getValue(FACING);
    }

    private boolean isMatchingSlope(BlockState state) {
        return state.getBlock() instanceof SteppedSlopeBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HALF))
                .get(state.getValue(FACING))
                .get(state.getValue(SHAPE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(HALF))
                .get(state.getValue(FACING))
                .get(state.getValue(SHAPE));
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
            for (Direction lowEdge : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                Direction highEdge = lowEdge.getOpposite();
                Map<StairsShape, VoxelShape> byShape = new EnumMap<>(StairsShape.class);

                for (StairsShape stairsShape : StairsShape.values()) {
                    VoxelShape combined = Shapes.empty();
                    for (int step = 0; step < 8; step++) {
                        double widthFromHighEdge = 16.0 - step * 2.0;
                        double y0;
                        double y1;
                        if (half == Half.BOTTOM) {
                            y0 = step * 2.0;
                            y1 = y0 + 2.0;
                        } else {
                            y1 = 16.0 - step * 2.0;
                            y0 = y1 - 2.0;
                        }

                        combined = Shapes.or(combined,
                                layerShape(highEdge, stairsShape, y0, y1, widthFromHighEdge));
                    }
                    byShape.put(stairsShape, combined);
                }
                byFacing.put(lowEdge, byShape);
            }
            result.put(half, byFacing);
        }

        return result;
    }

    private static VoxelShape layerShape(
            Direction highEdge,
            StairsShape stairsShape,
            double y0,
            double y1,
            double width
    ) {
        return switch (stairsShape) {
            case OUTER_LEFT -> cornerBand(highEdge, highEdge.getCounterClockWise(), y0, y1, width);
            case OUTER_RIGHT -> cornerBand(highEdge, highEdge.getClockWise(), y0, y1, width);
            case INNER_LEFT -> Shapes.or(
                    edgeBand(highEdge, y0, y1, width),
                    edgeBand(highEdge.getCounterClockWise(), y0, y1, width)
            );
            case INNER_RIGHT -> Shapes.or(
                    edgeBand(highEdge, y0, y1, width),
                    edgeBand(highEdge.getClockWise(), y0, y1, width)
            );
            default -> edgeBand(highEdge, y0, y1, width);
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

        for (Direction edge : new Direction[]{first, second}) {
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
