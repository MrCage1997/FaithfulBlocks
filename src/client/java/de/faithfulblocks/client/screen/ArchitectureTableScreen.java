package de.faithfulblocks.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.faithfulblocks.architecture.ArchitectureShape;
import de.faithfulblocks.menu.ArchitectureTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Stonecutter-style architecture selector. The input and output stay in normal
 * slots while available architectural shapes are presented as compact item
 * icons in a vanilla recipe grid.
 */
public final class ArchitectureTableScreen extends AbstractContainerScreen<ArchitectureTableMenu> {
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");

    private static final int COLUMNS = 4;
    private static final int VISIBLE_ROWS = 3;
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 18;
    private static final int SCROLLER_FULL_HEIGHT = 54;
    private static final int GRID_X = 52;
    private static final int GRID_Y = 14;

    private float scrollOffset;
    private boolean scrolling;
    private int startIndex;
    private boolean displayShapes;

    public ArchitectureTableScreen(ArchitectureTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.registerUpdateListener(this::containerChanged);
        this.titleLabelY--;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int xOrigin = this.leftPos;
        int yOrigin = this.topPos;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                xOrigin,
                yOrigin,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );

        int scrollY = (int) (41.0F * this.scrollOffset);
        Identifier scroller = isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        int scrollerX = xOrigin + 119;
        int scrollerY = yOrigin + 15;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scroller, scrollerX, scrollerY + scrollY, 12, 15);

        if (mouseX >= scrollerX && mouseY >= scrollerY && mouseX < scrollerX + 12 && mouseY < scrollerY + SCROLLER_FULL_HEIGHT) {
            if (isScrollBarActive()) {
                graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            } else {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }

        int gridX = this.leftPos + GRID_X;
        int gridY = this.topPos + GRID_Y;
        int endIndex = this.startIndex + COLUMNS * VISIBLE_ROWS;
        extractShapeButtons(graphics, mouseX, mouseY, gridX, gridY, endIndex);
        extractShapeItems(graphics, gridX, gridY, endIndex);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (!this.displayShapes) {
            return;
        }

        List<ArchitectureShape> shapes = this.menu.getAvailableShapes();
        int gridX = this.leftPos + GRID_X;
        int gridY = this.topPos + GRID_Y;
        int endIndex = Math.min(this.startIndex + COLUMNS * VISIBLE_ROWS, shapes.size());

        for (int index = this.startIndex; index < endIndex; index++) {
            int visualIndex = index - this.startIndex;
            int itemX = gridX + visualIndex % COLUMNS * BUTTON_WIDTH;
            int itemY = gridY + visualIndex / COLUMNS * BUTTON_HEIGHT + 2;

            if (mouseX >= itemX && mouseX < itemX + BUTTON_WIDTH && mouseY >= itemY && mouseY < itemY + BUTTON_HEIGHT) {
                ItemStack preview = this.menu.getResultFor(shapes.get(index));
                if (!preview.isEmpty()) {
                    graphics.setTooltipForNextFrame(this.font, preview, mouseX, mouseY);
                }
            }
        }
    }

    private void extractShapeButtons(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int endIndex
    ) {
        if (!this.displayShapes) {
            return;
        }

        List<ArchitectureShape> shapes = this.menu.getAvailableShapes();
        endIndex = Math.min(endIndex, shapes.size());

        for (int index = this.startIndex; index < endIndex; index++) {
            int visualIndex = index - this.startIndex;
            int posX = x + visualIndex % COLUMNS * BUTTON_WIDTH;
            int row = visualIndex / COLUMNS;
            int posY = y + row * BUTTON_HEIGHT + 2;
            ArchitectureShape shape = shapes.get(index);

            Identifier sprite;
            if (shape.buttonId() == this.menu.getSelectedShapeId()) {
                sprite = RECIPE_SELECTED_SPRITE;
            } else if (mouseX >= posX && mouseY >= posY && mouseX < posX + BUTTON_WIDTH && mouseY < posY + BUTTON_HEIGHT) {
                sprite = RECIPE_HIGHLIGHTED_SPRITE;
            } else {
                sprite = RECIPE_SPRITE;
            }

            int textureY = posY - 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, posX, textureY, BUTTON_WIDTH, BUTTON_HEIGHT);

            if (mouseX >= posX && mouseY >= textureY && mouseX < posX + BUTTON_WIDTH && mouseY < textureY + BUTTON_HEIGHT) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    private void extractShapeItems(GuiGraphicsExtractor graphics, int x, int y, int endIndex) {
        if (!this.displayShapes) {
            return;
        }

        List<ArchitectureShape> shapes = this.menu.getAvailableShapes();
        endIndex = Math.min(endIndex, shapes.size());

        for (int index = this.startIndex; index < endIndex; index++) {
            int visualIndex = index - this.startIndex;
            int posX = x + visualIndex % COLUMNS * BUTTON_WIDTH;
            int row = visualIndex / COLUMNS;
            int posY = y + row * BUTTON_HEIGHT + 2;
            ItemStack preview = this.menu.getResultFor(shapes.get(index));
            if (!preview.isEmpty()) {
                graphics.item(preview, posX, posY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.displayShapes) {
            List<ArchitectureShape> shapes = this.menu.getAvailableShapes();
            int xOrigin = this.leftPos + GRID_X;
            int yOrigin = this.topPos + GRID_Y;
            int endIndex = Math.min(this.startIndex + COLUMNS * VISIBLE_ROWS, shapes.size());

            for (int index = this.startIndex; index < endIndex; index++) {
                int visualIndex = index - this.startIndex;
                double localX = event.x() - (xOrigin + visualIndex % COLUMNS * BUTTON_WIDTH);
                double localY = event.y() - (yOrigin + visualIndex / COLUMNS * BUTTON_HEIGHT);

                if (localX >= 0.0 && localY >= 0.0 && localX < BUTTON_WIDTH && localY < BUTTON_HEIGHT) {
                    ArchitectureShape shape = shapes.get(index);
                    if (this.menu.clickMenuButton(this.minecraft.player, shape.buttonId())) {
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F)
                        );
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, shape.buttonId());
                        return true;
                    }
                }
            }

            int scrollerX = this.leftPos + 119;
            int scrollerY = this.topPos + 9;
            if (event.x() >= scrollerX && event.x() < scrollerX + 12
                    && event.y() >= scrollerY && event.y() < scrollerY + SCROLLER_FULL_HEIGHT) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling && isScrollBarActive()) {
            int scrollTop = this.topPos + GRID_Y;
            int scrollBottom = scrollTop + SCROLLER_FULL_HEIGHT;
            this.scrollOffset = ((float) event.y() - scrollTop - 7.5F) / (scrollBottom - scrollTop - 15.0F);
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffset * getOffscreenRows() + 0.5F) * COLUMNS;
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }

        if (isScrollBarActive()) {
            int offscreenRows = getOffscreenRows();
            float delta = (float) scrollY / offscreenRows;
            this.scrollOffset = Mth.clamp(this.scrollOffset - delta, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffset * offscreenRows + 0.5F) * COLUMNS;
        }

        return true;
    }

    private boolean isScrollBarActive() {
        return this.displayShapes && this.menu.getAvailableShapes().size() > COLUMNS * VISIBLE_ROWS;
    }

    private int getOffscreenRows() {
        return (this.menu.getAvailableShapes().size() + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS;
    }

    private void containerChanged() {
        this.displayShapes = this.menu.hasInputItem();
        this.scrollOffset = 0.0F;
        this.startIndex = 0;
    }
}
