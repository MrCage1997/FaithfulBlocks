package de.faithfulblocks.menu;

import de.faithfulblocks.architecture.ArchitectureRecipeRegistry;
import de.faithfulblocks.architecture.ArchitectureShape;
import de.faithfulblocks.block.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArchitectureTableMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 38;

    private final ContainerLevelAccess access;
    private final DataSlot selectedShape = DataSlot.standalone();
    private long lastSoundTime;
    private Runnable updateListener = () -> { };

    private final Container inputContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArchitectureTableMenu.this.slotsChanged(this);
        }
    };

    private final Container resultContainer = new SimpleContainer(1);
    private final Slot inputSlot;
    private final Slot resultSlot;

    public ArchitectureTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ArchitectureTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenuTypes.ARCHITECTURE_TABLE, containerId);
        this.access = access;
        this.selectedShape.set(-1);

        this.inputSlot = addSlot(new Slot(this.inputContainer, 0, 20, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ArchitectureRecipeRegistry.supports(stack.getItem());
            }
        });

        this.resultSlot = addSlot(new Slot(this.resultContainer, 0, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                ItemStack consumed = ArchitectureTableMenu.this.inputSlot.remove(1);
                if (!consumed.isEmpty()) {
                    ArchitectureTableMenu.this.setupResultSlot();
                }

                ArchitectureTableMenu.this.access.execute((level, pos) -> {
                    long gameTime = level.getGameTime();
                    if (ArchitectureTableMenu.this.lastSoundTime != gameTime) {
                        level.playSound(
                                null,
                                pos,
                                SoundEvents.UI_STONECUTTER_TAKE_RESULT,
                                SoundSource.BLOCKS,
                                1.0F,
                                1.0F
                        );
                        ArchitectureTableMenu.this.lastSoundTime = gameTime;
                    }
                });

                super.onTake(player, carried);
            }
        });

        addStandardInventorySlots(inventory, 8, 84);
        addDataSlot(this.selectedShape);
    }

    public void registerUpdateListener(Runnable listener) {
        this.updateListener = listener;
        listener.run();
    }

    public boolean hasInputItem() {
        return this.inputSlot.hasItem();
    }

    public List<ArchitectureShape> getAvailableShapes() {
        ItemStack input = this.inputSlot.getItem();
        if (input.isEmpty()) {
            return List.of();
        }

        List<ArchitectureShape> shapes = new ArrayList<>();
        for (ArchitectureShape shape : ArchitectureShape.values()) {
            if (ArchitectureRecipeRegistry.find(input.getItem(), shape) != null) {
                shapes.add(shape);
            }
        }
        return shapes;
    }

    public ItemStack getResultFor(ArchitectureShape shape) {
        ItemStack input = this.inputSlot.getItem();
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Item result = ArchitectureRecipeRegistry.find(input.getItem(), shape);
        return result == null ? ItemStack.EMPTY : new ItemStack(result);
    }

    public boolean hasRecipeFor(ArchitectureShape shape) {
        return !getResultFor(shape).isEmpty();
    }

    public int getSelectedShapeId() {
        return this.selectedShape.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        ArchitectureShape shape = ArchitectureShape.byButtonId(buttonId);
        if (shape == null || !hasRecipeFor(shape)) {
            return false;
        }

        this.selectedShape.set(buttonId);
        setupResultSlot();
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        ArchitectureShape selected = ArchitectureShape.byButtonId(this.selectedShape.get());
        if (selected == null || !hasRecipeFor(selected)) {
            this.selectedShape.set(-1);
            this.resultSlot.set(ItemStack.EMPTY);
            broadcastChanges();
            this.updateListener.run();
            return;
        }

        setupResultSlot();
        this.updateListener.run();
    }

    private void setupResultSlot() {
        ArchitectureShape selected = ArchitectureShape.byButtonId(this.selectedShape.get());
        if (selected == null) {
            this.resultSlot.set(ItemStack.EMPTY);
            broadcastChanges();
            return;
        }

        this.resultSlot.set(getResultFor(selected));
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex == OUTPUT_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex == INPUT_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (ArchitectureRecipeRegistry.supports(stack.getItem())) {
                if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 2 && slotIndex < 29) {
                if (!moveItemStackTo(stack, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 29 && slotIndex < 38 && !moveItemStackTo(stack, 2, 29, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }

            slot.setChanged();

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            broadcastChanges();
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ARCHITECTURE_TABLE);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(0);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputContainer));
    }
}
