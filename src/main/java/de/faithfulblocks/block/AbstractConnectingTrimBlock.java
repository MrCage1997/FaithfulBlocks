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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Shared stair-like corner logic for horizontal architectural trims. */
abstract class AbstractConnectingTrimBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

    private final Map<Direction, Map<StairsShape, VoxelShape>> shapes;

    protected AbstractConnectingTrimBlock(Properties properties, double[][] bands) {
        super(properties);
        this.shapes = createShapes(bands);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(SHAPE, StairsShape.STRAIGHT);
        return placed.setValue(SHAPE, getTrimShape(placed, context.getLevel(), context.getClickedPos()));
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
            return state.setValue(SHAPE, getTrimShape(state, level, pos));
        }
        return state;
    }

    private StairsShape getTrimShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);

        BlockState front = level.getBlockState(pos.relative(facing));
        if (isMatchingTrim(front)) {
            Direction neighbourFacing = front.getValue(FACING);
            if (neighbourFacing.getAxis() != facing.getAxis()
                    && canTakeShape(state, level, pos, neighbourFacing.getOpposite())) {
                return neighbourFacing == facing.getCounterClockWise()
                        ? StairsShape.OUTER_LEFT
                        : StairsShape.OUTER_RIGHT;
            }
        }

        BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
        if (isMatchingTrim(back)) {
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
        return !isMatchingTrim(neighbour) || neighbour.getValue(FACING) != state.getValue(FACING);
    }

    private boolean isMatchingTrim(BlockState state) {
        return state.getBlock().getClass() == this.getClass();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING)).get(state.getValue(SHAPE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING)).get(state.getValue(SHAPE));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static Map<Direction, Map<StairsShape, VoxelShape>> createShapes(double[][] bands) {
        Map<Direction, Map<StairsShape, VoxelShape>> result = new EnumMap<>(Direction.class);
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            Map<StairsShape, VoxelShape> byShape = new EnumMap<>(StairsShape.class);
            for (StairsShape shape : StairsShape.values()) {
                VoxelShape combined = Shapes.empty();
                for (double[] band : bands) {
                    combined = Shapes.or(combined, createBand(facing, shape, band[0], band[1], band[2]));
                }
                byShape.put(shape, combined);
            }
            result.put(facing, byShape);
        }
        return result;
    }

    private static VoxelShape createBand(Direction facing, StairsShape shape, double y0, double y1, double width) {
        return switch (shape) {
            case OUTER_LEFT -> cornerBand(facing, facing.getCounterClockWise(), y0, y1, width);
            case OUTER_RIGHT -> cornerBand(facing, facing.getClockWise(), y0, y1, width);
            case INNER_LEFT -> Shapes.or(edgeBand(facing, y0, y1, width), edgeBand(facing.getCounterClockWise(), y0, y1, width));
            case INNER_RIGHT -> Shapes.or(edgeBand(facing, y0, y1, width), edgeBand(facing.getClockWise(), y0, y1, width));
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
        double minX = 0, maxX = 16, minZ = 0, maxZ = 16;
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
