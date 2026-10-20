package de.faithfulblocks.block;

import de.faithfulblocks.FaithfulBlocks;
import de.faithfulblocks.architecture.ArchitectureMaterial;
import de.faithfulblocks.architecture.ArchitectureMaterials;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class ModBlocks {
    public static final Block ARCHITECTURE_TABLE = register(
            "architecture_table",
            ArchitectureTableBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONECUTTER)
    );

    private static final Map<ArchitectureMaterial, Block> ROUNDED_CORNERS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> NARROW_COLUMNS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> ROUNDED_INNER_CORNERS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> PEDESTALS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> CORNICES = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> ARCHES = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> WALL_CAPS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> WALL_ENDS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> PILASTERS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> LARGE_COLUMNS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> LARGE_COLUMN_PEDESTALS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> WALL_PEDESTALS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> WALL_CORNICES = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> WINDOW_FRAMES = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> BEVELED_INNER_CORNERS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> BEVELED_CORNERS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> FACADE_OFFSETS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> STEPPED_SLOPES = new LinkedHashMap<>();
    // Legacy 0.7.x ids stay registered so existing test worlds do not lose placed blocks.
    private static final Map<ArchitectureMaterial, Block> ARCH_LEFTS = new LinkedHashMap<>();
    private static final Map<ArchitectureMaterial, Block> ARCH_RIGHTS = new LinkedHashMap<>();

    static {
        for (ArchitectureMaterial material : ArchitectureMaterials.ALL) {
            ROUNDED_CORNERS.put(material, register(
                    material.id() + "_rounded_corner",
                    RoundedCornerBlock::new,
                    shapedProperties(material)
            ));

            NARROW_COLUMNS.put(material, register(
                    material.id() + "_narrow_column",
                    NarrowColumnBlock::new,
                    shapedProperties(material)
            ));

            ROUNDED_INNER_CORNERS.put(material, register(
                    material.id() + "_rounded_inner_corner",
                    RoundedInnerCornerBlock::new,
                    shapedProperties(material)
            ));

            PEDESTALS.put(material, register(
                    material.id() + "_pedestal",
                    PedestalBlock::new,
                    shapedProperties(material)
            ));

            WINDOW_FRAMES.put(material, register(material.id() + "_window_frame",
                    WindowFrameBlock::new, shapedProperties(material)));
            BEVELED_INNER_CORNERS.put(material, register(material.id() + "_beveled_inner_corner",
                    BeveledInnerCornerBlock::new, shapedProperties(material)));
            BEVELED_CORNERS.put(material, register(material.id() + "_beveled_corner",
                    BeveledCornerBlock::new, shapedProperties(material)));
            FACADE_OFFSETS.put(material, register(material.id() + "_facade_offset",
                    FacadeOffsetBlock::new, shapedProperties(material)));
            STEPPED_SLOPES.put(material, register(material.id() + "_stepped_slope",
                    SteppedSlopeBlock::new, shapedProperties(material)));

            // Legacy block and item IDs remain registered for existing worlds.
            CORNICES.put(material, register(
                    material.id() + "_cornice",
                    CorniceBlock::new,
                    shapedProperties(material)
            ));

            ARCHES.put(material, register(
                    material.id() + "_arch",
                    ConnectingArchBlock::new,
                    shapedProperties(material)
            ));

            WALL_CAPS.put(material, register(
                    material.id() + "_wall_cap",
                    WallCapBlock::new,
                    shapedProperties(material)
            ));

            WALL_ENDS.put(material, register(
                    material.id() + "_wall_end",
                    WallEndBlock::new,
                    shapedProperties(material)
            ));

            PILASTERS.put(material, register(
                    material.id() + "_pilaster",
                    PilasterBlock::new,
                    shapedProperties(material)
            ));

            LARGE_COLUMNS.put(material, register(
                    material.id() + "_large_column",
                    LargeColumnBlock::new,
                    shapedProperties(material)
            ));

            LARGE_COLUMN_PEDESTALS.put(material, register(
                    material.id() + "_large_column_pedestal",
                    LargeColumnPedestalBlock::new,
                    shapedProperties(material)
            ));

            WALL_PEDESTALS.put(material, register(
                    material.id() + "_wall_pedestal",
                    WallPedestalBlock::new,
                    shapedProperties(material)
            ));

            WALL_CORNICES.put(material, register(
                    material.id() + "_wall_cornice",
                    WallCorniceBlock::new,
                    shapedProperties(material)
            ));

            ARCH_LEFTS.put(material, register(
                    material.id() + "_arch_left",
                    properties -> new ArchSegmentBlock(properties, ArchSegmentBlock.Side.LEFT),
                    shapedProperties(material)
            ));

            ARCH_RIGHTS.put(material, register(
                    material.id() + "_arch_right",
                    properties -> new ArchSegmentBlock(properties, ArchSegmentBlock.Side.RIGHT),
                    shapedProperties(material)
            ));
        }
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties shapedProperties(ArchitectureMaterial material) {
        return BlockBehaviour.Properties.ofFullCopy(material.baseBlock()).noOcclusion();
    }

    private static Block require(Map<ArchitectureMaterial, Block> map, ArchitectureMaterial material, String type) {
        Block block = map.get(material);
        if (block == null) {
            throw new IllegalArgumentException("No " + type + " registered for material: " + material.id());
        }
        return block;
    }

    public static Block roundedCorner(ArchitectureMaterial material) {
        return require(ROUNDED_CORNERS, material, "rounded corner");
    }

    public static Block narrowColumn(ArchitectureMaterial material) {
        return require(NARROW_COLUMNS, material, "narrow column");
    }

    public static Block roundedInnerCorner(ArchitectureMaterial material) {
        return require(ROUNDED_INNER_CORNERS, material, "rounded inner corner");
    }

    public static Block pedestal(ArchitectureMaterial material) {
        return require(PEDESTALS, material, "pedestal");
    }

    public static Block windowFrame(ArchitectureMaterial material) {
        return require(WINDOW_FRAMES, material, "window frame");
    }

    public static Block beveledInnerCorner(ArchitectureMaterial material) {
        return require(BEVELED_INNER_CORNERS, material, "beveled inner corner");
    }

    public static Block beveledCorner(ArchitectureMaterial material) {
        return require(BEVELED_CORNERS, material, "beveled corner");
    }

    public static Block facadeOffset(ArchitectureMaterial material) {
        return require(FACADE_OFFSETS, material, "facade offset");
    }

    public static Block steppedSlope(ArchitectureMaterial material) {
        return require(STEPPED_SLOPES, material, "stepped slope");
    }

    public static Block cornice(ArchitectureMaterial material) {
        return require(CORNICES, material, "cornice");
    }

    public static Block arch(ArchitectureMaterial material) {
        return require(ARCHES, material, "arch");
    }

    public static Block wallCap(ArchitectureMaterial material) {
        return require(WALL_CAPS, material, "wall cap");
    }

    public static Block wallEnd(ArchitectureMaterial material) {
        return require(WALL_ENDS, material, "wall end");
    }

    public static Block pilaster(ArchitectureMaterial material) {
        return require(PILASTERS, material, "pilaster");
    }

    public static Block largeColumn(ArchitectureMaterial material) {
        return require(LARGE_COLUMNS, material, "large column");
    }

    public static Block largeColumnPedestal(ArchitectureMaterial material) {
        return require(LARGE_COLUMN_PEDESTALS, material, "large column pedestal");
    }

    public static Block wallPedestal(ArchitectureMaterial material) {
        return require(WALL_PEDESTALS, material, "wall pedestal");
    }

    public static Block wallCornice(ArchitectureMaterial material) {
        return require(WALL_CORNICES, material, "wall cornice");
    }

    public static Block archLeft(ArchitectureMaterial material) {
        return require(ARCH_LEFTS, material, "left arch segment");
    }

    public static Block archRight(ArchitectureMaterial material) {
        return require(ARCH_RIGHTS, material, "right arch segment");
    }

    private static Block register(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties
    ) {
        Identifier id = FaithfulBlocks.id(name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Block block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        BlockItem blockItem = new BlockItem(
                block,
                new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)
        );
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(ARCHITECTURE_TABLE.asItem());
            for (ArchitectureMaterial material : ArchitectureMaterials.ALL) {
                entries.accept(roundedCorner(material).asItem());
                entries.accept(roundedInnerCorner(material).asItem());
                entries.accept(narrowColumn(material).asItem());
                entries.accept(pedestal(material).asItem());
                entries.accept(beveledCorner(material).asItem());
                entries.accept(beveledInnerCorner(material).asItem());
                entries.accept(facadeOffset(material).asItem());
                entries.accept(steppedSlope(material).asItem());
                entries.accept(windowFrame(material).asItem());
                entries.accept(arch(material).asItem());
                entries.accept(wallCap(material).asItem());
                entries.accept(wallEnd(material).asItem());
                entries.accept(pilaster(material).asItem());
                entries.accept(largeColumn(material).asItem());
                entries.accept(largeColumnPedestal(material).asItem());
                entries.accept(wallPedestal(material).asItem());
                entries.accept(wallCornice(material).asItem());
            }
        });
    }
}
