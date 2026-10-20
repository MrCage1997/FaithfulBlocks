package de.faithfulblocks;

import de.faithfulblocks.architecture.ArchitectureRecipeRegistry;
import de.faithfulblocks.block.ModBlocks;
import de.faithfulblocks.menu.ModMenuTypes;
import de.faithfulblocks.update.UpdateChecker;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FaithfulBlocks implements ModInitializer {
    public static final String MOD_ID = "faithfulblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModMenuTypes.initialize();
        ArchitectureRecipeRegistry.initialize();
        UpdateChecker.initializeAsync();
        LOGGER.info("FaithfulBlocks initialized.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
