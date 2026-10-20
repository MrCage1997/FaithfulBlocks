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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Quarter-block panel; clicked face determines the outward direction. */
public final class FacadeOffsetBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final double[][] BOXES = {
            { 0, 0, 0, 16, 4, 16 }
    };

    private static final VoxelShape UP = DirectionalShapeUtils.create(Direction.UP, BOXES);
    private static final VoxelShape DOWN = DirectionalShapeUtils.create(Direction.DOWN, BOXES);
    private static final VoxelShape NORTH = DirectionalShapeUtils.create(Direction.NORTH, BOXES);
    private static final VoxelShape SOUTH = DirectionalShapeUtils.create(Direction.SOUTH, BOXES);
    private static final VoxelShape EAST = DirectionalShapeUtils.create(Direction.EAST, BOXES);
    private static final VoxelShape WEST = DirectionalShapeUtils.create(Direction.WEST, BOXES);

    public FacadeOffsetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();

        // When extending an existing facade surface, keep its plane instead of
        // turning the new panel toward the side that was clicked. This makes
        // long facade bands buildable without temporary scaffold blocks.
        for (Direction offset : Direction.values()) {
            BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(offset));
            if (neighbour.getBlock() instanceof FacadeOffsetBlock) {
                Direction neighbourFacing = neighbour.getValue(FACING);
                if (offset.getAxis() != neighbourFacing.getAxis()) {
                    facing = neighbourFacing;
                    break;
                }
            }
        }

        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static VoxelShape shape(Direction facing) {
        return switch (facing) {
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
