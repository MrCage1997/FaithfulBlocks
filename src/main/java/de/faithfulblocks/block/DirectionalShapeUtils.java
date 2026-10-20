package de.faithfulblocks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Utility for rotating model-space box collections from UP into any block-face direction. */
final class DirectionalShapeUtils {
    private DirectionalShapeUtils() {
    }

    static VoxelShape create(Direction facing, double[][] boxes) {
        VoxelShape result = Shapes.empty();
        for (double[] box : boxes) {
            result = Shapes.or(result, orientedBox(
                    facing,
                    box[0], box[1], box[2],
                    box[3], box[4], box[5]
            ));
        }
        return result;
    }

    private static VoxelShape orientedBox(
            Direction facing,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        double outMinX = Double.POSITIVE_INFINITY;
        double outMinY = Double.POSITIVE_INFINITY;
        double outMinZ = Double.POSITIVE_INFINITY;
        double outMaxX = Double.NEGATIVE_INFINITY;
        double outMaxY = Double.NEGATIVE_INFINITY;
        double outMaxZ = Double.NEGATIVE_INFINITY;

        for (double x : new double[] { minX, maxX }) {
            for (double y : new double[] { minY, maxY }) {
                for (double z : new double[] { minZ, maxZ }) {
                    double[] point = transform(facing, x, y, z);
                    outMinX = Math.min(outMinX, point[0]);
                    outMinY = Math.min(outMinY, point[1]);
                    outMinZ = Math.min(outMinZ, point[2]);
                    outMaxX = Math.max(outMaxX, point[0]);
                    outMaxY = Math.max(outMaxY, point[1]);
                    outMaxZ = Math.max(outMaxZ, point[2]);
                }
            }
        }

        return Block.box(outMinX, outMinY, outMinZ, outMaxX, outMaxY, outMaxZ);
    }

    /**
     * Rotates coordinates around the block centre so the original +Y axis points at {@code facing}.
     */
    private static double[] transform(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case UP -> new double[] { x, y, z };
            case DOWN -> new double[] { x, 16.0 - y, 16.0 - z };
            case NORTH -> new double[] { x, z, 16.0 - y };
            case SOUTH -> new double[] { x, 16.0 - z, y };
            case EAST -> new double[] { y, 16.0 - x, z };
            case WEST -> new double[] { 16.0 - y, x, z };
        };
    }
}
