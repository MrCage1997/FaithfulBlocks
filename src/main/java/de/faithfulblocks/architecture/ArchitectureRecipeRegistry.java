package de.faithfulblocks.architecture;

import de.faithfulblocks.block.ModBlocks;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ArchitectureRecipeRegistry {
    private static final Map<Item, EnumMap<ArchitectureShape, Item>> RECIPES = new IdentityHashMap<>();
    private static final Map<Item, Item> REVERSE_RECIPES = new IdentityHashMap<>();

    private ArchitectureRecipeRegistry() {
    }

    public static void initialize() {
        for (ArchitectureMaterial material : ArchitectureMaterials.ALL) {
            register(material.inputItem(), ArchitectureShape.ROUNDED_CORNER, ModBlocks.roundedCorner(material).asItem());
            registerReverse(ModBlocks.roundedCorner(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.NARROW_COLUMN, ModBlocks.narrowColumn(material).asItem());
            registerReverse(ModBlocks.narrowColumn(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.ROUNDED_INNER_CORNER, ModBlocks.roundedInnerCorner(material).asItem());
            registerReverse(ModBlocks.roundedInnerCorner(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.PEDESTAL, ModBlocks.pedestal(material).asItem());
            registerReverse(ModBlocks.pedestal(material).asItem(), material.inputItem());
            // CORNICE keeps its ID, but is no longer offered by the table.
            register(material.inputItem(), ArchitectureShape.BEVELED_CORNER, ModBlocks.beveledCorner(material).asItem());
            registerReverse(ModBlocks.beveledCorner(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.FACADE_OFFSET, ModBlocks.facadeOffset(material).asItem());
            registerReverse(ModBlocks.facadeOffset(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.BEVELED_INNER_CORNER, ModBlocks.beveledInnerCorner(material).asItem());
            registerReverse(ModBlocks.beveledInnerCorner(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.WINDOW_FRAME, ModBlocks.windowFrame(material).asItem());
            registerReverse(ModBlocks.windowFrame(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.STEPPED_SLOPE, ModBlocks.steppedSlope(material).asItem());
            registerReverse(ModBlocks.steppedSlope(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.ARCH, ModBlocks.arch(material).asItem());
            registerReverse(ModBlocks.arch(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.WALL_CAP, ModBlocks.wallCap(material).asItem());
            registerReverse(ModBlocks.wallCap(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.WALL_END, ModBlocks.wallEnd(material).asItem());
            registerReverse(ModBlocks.wallEnd(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.PILASTER, ModBlocks.pilaster(material).asItem());
            registerReverse(ModBlocks.pilaster(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.LARGE_COLUMN, ModBlocks.largeColumn(material).asItem());
            registerReverse(ModBlocks.largeColumn(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.LARGE_COLUMN_PEDESTAL, ModBlocks.largeColumnPedestal(material).asItem());
            registerReverse(ModBlocks.largeColumnPedestal(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.WALL_PEDESTAL, ModBlocks.wallPedestal(material).asItem());
            registerReverse(ModBlocks.wallPedestal(material).asItem(), material.inputItem());
            register(material.inputItem(), ArchitectureShape.WALL_CORNICE, ModBlocks.wallCornice(material).asItem());
            registerReverse(ModBlocks.wallCornice(material).asItem(), material.inputItem());
            registerReverse(ModBlocks.cornice(material).asItem(), material.inputItem());
            registerReverse(ModBlocks.archLeft(material).asItem(), material.inputItem());
            registerReverse(ModBlocks.archRight(material).asItem(), material.inputItem());
        }
    }

    public static void register(Item input, ArchitectureShape shape, Item output) {
        RECIPES.computeIfAbsent(input, ignored -> new EnumMap<>(ArchitectureShape.class)).put(shape, output);
    }

    public static boolean supports(Item input) {
        return RECIPES.containsKey(input) || REVERSE_RECIPES.containsKey(input);
    }

    public static Item find(Item input, ArchitectureShape shape) {
        if (shape == ArchitectureShape.RETURN_ORIGINAL) {
            return REVERSE_RECIPES.get(input);
        }

        EnumMap<ArchitectureShape, Item> shapes = RECIPES.get(input);
        return shapes == null ? null : shapes.get(shape);
    }

    private static void registerReverse(Item architectureItem, Item originalItem) {
        REVERSE_RECIPES.put(architectureItem, originalItem);
    }
}
