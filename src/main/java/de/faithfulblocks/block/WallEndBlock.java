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
 * Decorative stepped termination for the exposed end of a wall.
 * The broad side joins the wall while the stepped face points outward.
 */
public final class WallEndBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH = Shapes.or(
            Block.box(0, 0, 4, 16, 16, 16),
            Block.box(1, 1, 2, 15, 15, 4),
            Block.box(2, 2, 0, 14, 14, 2)
    );
    private static final VoxelShape SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 16, 12),
            Block.box(1, 1, 12, 15, 15, 14),
            Block.box(2, 2, 14, 14, 14, 16)
    );
    private static final VoxelShape WEST = Shapes.or(
            Block.box(4, 0, 0, 16, 16, 16),
            Block.box(2, 1, 1, 4, 15, 15),
            Block.box(0, 2, 2, 2, 14, 14)
    );
    private static final VoxelShape EAST = Shapes.or(
            Block.box(0, 0, 0, 12, 16, 16),
            Block.box(12, 1, 1, 14, 15, 15),
            Block.box(14, 2, 2, 16, 14, 14)
    );

    public WallEndBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Stacked wall ends keep the same exposed direction automatically.
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (below.getBlock() instanceof WallEndBlock) {
            return defaultBlockState().setValue(FACING, below.getValue(FACING));
        }
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        if (above.getBlock() instanceof WallEndBlock) {
            return defaultBlockState().setValue(FACING, above.getValue(FACING));
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
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
