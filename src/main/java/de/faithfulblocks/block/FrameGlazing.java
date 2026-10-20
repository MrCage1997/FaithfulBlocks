package de.faithfulblocks.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Saved directly in the frame block state, including the clear-glass variant. */
public enum FrameGlazing implements StringRepresentable {
    NONE("none", "air"),
    CLEAR("clear", "glass_pane"),
    WHITE("white", "white_stained_glass_pane"),
    ORANGE("orange", "orange_stained_glass_pane"),
    MAGENTA("magenta", "magenta_stained_glass_pane"),
    LIGHT_BLUE("light_blue", "light_blue_stained_glass_pane"),
    YELLOW("yellow", "yellow_stained_glass_pane"),
    LIME("lime", "lime_stained_glass_pane"),
    PINK("pink", "pink_stained_glass_pane"),
    GRAY("gray", "gray_stained_glass_pane"),
    LIGHT_GRAY("light_gray", "light_gray_stained_glass_pane"),
    CYAN("cyan", "cyan_stained_glass_pane"),
    PURPLE("purple", "purple_stained_glass_pane"),
    BLUE("blue", "blue_stained_glass_pane"),
    BROWN("brown", "brown_stained_glass_pane"),
    GREEN("green", "green_stained_glass_pane"),
    RED("red", "red_stained_glass_pane"),
    BLACK("black", "black_stained_glass_pane");

    private final String name;
    private final Identifier paneId;

    FrameGlazing(String name, String panePath) {
        this.name = name;
        this.paneId = Identifier.withDefaultNamespace(panePath);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static FrameGlazing fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (FrameGlazing glazing : values()) {
            if (glazing != NONE && glazing.paneId.equals(itemId)) {
                return glazing;
            }
        }
        return NONE;
    }
}
