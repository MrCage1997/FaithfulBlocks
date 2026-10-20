package de.faithfulblocks.client;

import de.faithfulblocks.client.screen.ArchitectureTableScreen;
import de.faithfulblocks.menu.ModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class FaithfulBlocksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenuTypes.ARCHITECTURE_TABLE, ArchitectureTableScreen::new);
    }
}
