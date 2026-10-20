package de.faithfulblocks.architecture;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Describes one supported vanilla building material.
 *
 * @param id stable FaithfulBlocks material id used for generated block ids
 * @param inputItem vanilla item consumed by the architecture table
 * @param baseBlock vanilla block whose physical properties are inherited
 */
public record ArchitectureMaterial(String id, Item inputItem, Block baseBlock) {
}
