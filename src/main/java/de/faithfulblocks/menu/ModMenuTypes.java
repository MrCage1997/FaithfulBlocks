package de.faithfulblocks.menu;

import de.faithfulblocks.FaithfulBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
    public static final MenuType<ArchitectureTableMenu> ARCHITECTURE_TABLE = register(
            "architecture_table",
            ArchitectureTableMenu::new
    );

    private ModMenuTypes() {
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(
            String name,
            MenuType.MenuSupplier<T> supplier
    ) {
        return Registry.register(
                BuiltInRegistries.MENU,
                FaithfulBlocks.id(name),
                new MenuType<>(supplier, FeatureFlagSet.of())
        );
    }

    public static void initialize() {
        // Triggers static registration.
    }
}
