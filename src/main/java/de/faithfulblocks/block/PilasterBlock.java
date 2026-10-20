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

/** Wall-attached half-column / pilaster for façades and interior arcades. */
public final class PilasterBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH = Shapes.or(
            Block.box(3, 0, 12, 13, 16, 16),
            Block.box(4, 0, 5, 12, 16, 16),
            Block.box(2, 0, 8, 4, 16, 16),
            Block.box(12, 0, 8, 14, 16, 16)
    );
    private static final VoxelShape SOUTH = rotate180(NORTH);
    private static final VoxelShape WEST = Shapes.or(
            Block.box(12, 0, 3, 16, 16, 13),
            Block.box(5, 0, 4, 16, 16, 12),
            Block.box(8, 0, 2, 16, 16, 4),
            Block.box(8, 0, 12, 16, 16, 14)
    );
    private static final VoxelShape EAST = Shapes.or(
            Block.box(0, 0, 3, 4, 16, 13),
            Block.box(0, 0, 4, 11, 16, 12),
            Block.box(0, 0, 2, 8, 16, 4),
            Block.box(0, 0, 12, 8, 16, 14)
    );

    public PilasterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Vertical stacks inherit the pilaster below/above so a facade strip can
        // be extended without repeatedly finding the original wall face.
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (below.getBlock() instanceof PilasterBlock) {
            return defaultBlockState().setValue(FACING, below.getValue(FACING));
        }
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        if (above.getBlock() instanceof PilasterBlock) {
            return defaultBlockState().setValue(FACING, above.getValue(FACING));
        }

        Direction clicked = context.getClickedFace();
        Direction facing = clicked.getAxis() != Direction.Axis.Y ? clicked : context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static VoxelShape shape(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
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
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static VoxelShape rotate180(VoxelShape ignored) {
        return Shapes.or(
                Block.box(3, 0, 0, 13, 16, 4),
                Block.box(4, 0, 0, 12, 16, 11),
                Block.box(2, 0, 0, 4, 16, 8),
                Block.box(12, 0, 0, 14, 16, 8)
        );
    }
}
