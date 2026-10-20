package de.faithfulblocks.architecture;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Central list of vanilla materials supported by the architecture system.
 * Adding a material here is enough for Java-side registration of the rounded corner.
 */
public final class ArchitectureMaterials {
    public static final ArchitectureMaterial STONE = new ArchitectureMaterial("stone", Items.STONE, Blocks.STONE);
    public static final ArchitectureMaterial COBBLESTONE = new ArchitectureMaterial("cobblestone", Items.COBBLESTONE, Blocks.COBBLESTONE);
    public static final ArchitectureMaterial STONE_BRICKS = new ArchitectureMaterial("stone_bricks", Items.STONE_BRICKS, Blocks.STONE_BRICKS);
    public static final ArchitectureMaterial DEEPSLATE_BRICKS = new ArchitectureMaterial("deepslate_bricks", Items.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);
    public static final ArchitectureMaterial TUFF_BRICKS = new ArchitectureMaterial("tuff_bricks", Items.TUFF_BRICKS, Blocks.TUFF_BRICKS);
    public static final ArchitectureMaterial ANDESITE = new ArchitectureMaterial("andesite", Items.ANDESITE, Blocks.ANDESITE);
    public static final ArchitectureMaterial DIORITE = new ArchitectureMaterial("diorite", Items.DIORITE, Blocks.DIORITE);
    public static final ArchitectureMaterial GRANITE = new ArchitectureMaterial("granite", Items.GRANITE, Blocks.GRANITE);
    public static final ArchitectureMaterial SANDSTONE = new ArchitectureMaterial("sandstone", Items.SANDSTONE, Blocks.SANDSTONE);
    public static final ArchitectureMaterial RED_SANDSTONE = new ArchitectureMaterial("red_sandstone", Items.RED_SANDSTONE, Blocks.RED_SANDSTONE);
    public static final ArchitectureMaterial BRICKS = new ArchitectureMaterial("bricks", Items.BRICKS, Blocks.BRICKS);
    public static final ArchitectureMaterial NETHER_BRICKS = new ArchitectureMaterial("nether_bricks", Items.NETHER_BRICKS, Blocks.NETHER_BRICKS);
    public static final ArchitectureMaterial OAK_PLANKS = new ArchitectureMaterial("oak_planks", Items.OAK_PLANKS, Blocks.OAK_PLANKS);
    public static final ArchitectureMaterial SPRUCE_PLANKS = new ArchitectureMaterial("spruce_planks", Items.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS);
    public static final ArchitectureMaterial BIRCH_PLANKS = new ArchitectureMaterial("birch_planks", Items.BIRCH_PLANKS, Blocks.BIRCH_PLANKS);
    public static final ArchitectureMaterial JUNGLE_PLANKS = new ArchitectureMaterial("jungle_planks", Items.JUNGLE_PLANKS, Blocks.JUNGLE_PLANKS);
    public static final ArchitectureMaterial ACACIA_PLANKS = new ArchitectureMaterial("acacia_planks", Items.ACACIA_PLANKS, Blocks.ACACIA_PLANKS);
    public static final ArchitectureMaterial DARK_OAK_PLANKS = new ArchitectureMaterial("dark_oak_planks", Items.DARK_OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
    public static final ArchitectureMaterial MANGROVE_PLANKS = new ArchitectureMaterial("mangrove_planks", Items.MANGROVE_PLANKS, Blocks.MANGROVE_PLANKS);
    public static final ArchitectureMaterial CHERRY_PLANKS = new ArchitectureMaterial("cherry_planks", Items.CHERRY_PLANKS, Blocks.CHERRY_PLANKS);
    public static final ArchitectureMaterial BAMBOO_PLANKS = new ArchitectureMaterial("bamboo_planks", Items.BAMBOO_PLANKS, Blocks.BAMBOO_PLANKS);
    public static final ArchitectureMaterial CRIMSON_PLANKS = new ArchitectureMaterial("crimson_planks", Items.CRIMSON_PLANKS, Blocks.CRIMSON_PLANKS);
    public static final ArchitectureMaterial WARPED_PLANKS = new ArchitectureMaterial("warped_planks", Items.WARPED_PLANKS, Blocks.WARPED_PLANKS);
    public static final ArchitectureMaterial PALE_OAK_PLANKS = new ArchitectureMaterial("pale_oak_planks", Items.PALE_OAK_PLANKS, Blocks.PALE_OAK_PLANKS);
    public static final ArchitectureMaterial SMOOTH_STONE = new ArchitectureMaterial("smooth_stone", Items.SMOOTH_STONE, Blocks.SMOOTH_STONE);
    public static final ArchitectureMaterial COBBLED_DEEPSLATE = new ArchitectureMaterial("cobbled_deepslate", Items.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
    public static final ArchitectureMaterial POLISHED_DEEPSLATE = new ArchitectureMaterial("polished_deepslate", Items.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE);
    public static final ArchitectureMaterial DEEPSLATE_TILES = new ArchitectureMaterial("deepslate_tiles", Items.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILES);
    public static final ArchitectureMaterial TUFF = new ArchitectureMaterial("tuff", Items.TUFF, Blocks.TUFF);
    public static final ArchitectureMaterial POLISHED_TUFF = new ArchitectureMaterial("polished_tuff", Items.POLISHED_TUFF, Blocks.POLISHED_TUFF);
    public static final ArchitectureMaterial POLISHED_ANDESITE = new ArchitectureMaterial("polished_andesite", Items.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE);
    public static final ArchitectureMaterial POLISHED_DIORITE = new ArchitectureMaterial("polished_diorite", Items.POLISHED_DIORITE, Blocks.POLISHED_DIORITE);
    public static final ArchitectureMaterial POLISHED_GRANITE = new ArchitectureMaterial("polished_granite", Items.POLISHED_GRANITE, Blocks.POLISHED_GRANITE);
    public static final ArchitectureMaterial CALCITE = new ArchitectureMaterial("calcite", Items.CALCITE, Blocks.CALCITE);
    public static final ArchitectureMaterial PACKED_MUD = new ArchitectureMaterial("packed_mud", Items.PACKED_MUD, Blocks.PACKED_MUD);
    public static final ArchitectureMaterial MUD_BRICKS = new ArchitectureMaterial("mud_bricks", Items.MUD_BRICKS, Blocks.MUD_BRICKS);
    public static final ArchitectureMaterial BLACKSTONE = new ArchitectureMaterial("blackstone", Items.BLACKSTONE, Blocks.BLACKSTONE);
    public static final ArchitectureMaterial POLISHED_BLACKSTONE = new ArchitectureMaterial("polished_blackstone", Items.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
    public static final ArchitectureMaterial POLISHED_BLACKSTONE_BRICKS = new ArchitectureMaterial("polished_blackstone_bricks", Items.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
    public static final ArchitectureMaterial RED_NETHER_BRICKS = new ArchitectureMaterial("red_nether_bricks", Items.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS);
    public static final ArchitectureMaterial END_STONE_BRICKS = new ArchitectureMaterial("end_stone_bricks", Items.END_STONE_BRICKS, Blocks.END_STONE_BRICKS);
    public static final ArchitectureMaterial PRISMARINE = new ArchitectureMaterial("prismarine", Items.PRISMARINE, Blocks.PRISMARINE);
    public static final ArchitectureMaterial PRISMARINE_BRICKS = new ArchitectureMaterial("prismarine_bricks", Items.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS);
    public static final ArchitectureMaterial DARK_PRISMARINE = new ArchitectureMaterial("dark_prismarine", Items.DARK_PRISMARINE, Blocks.DARK_PRISMARINE);
    public static final ArchitectureMaterial QUARTZ_BRICKS = new ArchitectureMaterial("quartz_bricks", Items.QUARTZ_BRICKS, Blocks.QUARTZ_BRICKS);
    public static final ArchitectureMaterial PURPUR_BLOCK = new ArchitectureMaterial("purpur_block", Items.PURPUR_BLOCK, Blocks.PURPUR_BLOCK);

    public static final List<ArchitectureMaterial> ALL = List.of(
            STONE,
            COBBLESTONE,
            STONE_BRICKS,
            DEEPSLATE_BRICKS,
            TUFF_BRICKS,
            ANDESITE,
            DIORITE,
            GRANITE,
            SANDSTONE,
            RED_SANDSTONE,
            BRICKS,
            NETHER_BRICKS,
            OAK_PLANKS,
            SPRUCE_PLANKS,
            BIRCH_PLANKS,
            JUNGLE_PLANKS,
            ACACIA_PLANKS,
            DARK_OAK_PLANKS,
            MANGROVE_PLANKS,
            CHERRY_PLANKS,
            BAMBOO_PLANKS,
            CRIMSON_PLANKS,
            WARPED_PLANKS,
            PALE_OAK_PLANKS,
            SMOOTH_STONE,
            COBBLED_DEEPSLATE,
            POLISHED_DEEPSLATE,
            DEEPSLATE_TILES,
            TUFF,
            POLISHED_TUFF,
            POLISHED_ANDESITE,
            POLISHED_DIORITE,
            POLISHED_GRANITE,
            CALCITE,
            PACKED_MUD,
            MUD_BRICKS,
            BLACKSTONE,
            POLISHED_BLACKSTONE,
            POLISHED_BLACKSTONE_BRICKS,
            RED_NETHER_BRICKS,
            END_STONE_BRICKS,
            PRISMARINE,
            PRISMARINE_BRICKS,
            DARK_PRISMARINE,
            QUARTZ_BRICKS,
            PURPUR_BLOCK
    );

    private ArchitectureMaterials() {
    }
}
