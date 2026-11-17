package ru.beggar.berlimatrm.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for the recipe maker.
 *
 * <p>
 * Slots 0..80 are the ghost 9x9 matrix, slot 81 is the result. Clicks on them are intercepted:
 * the cursor stack is copied into the slot (1 item for ingredients, the whole stack for the
 * result), an empty cursor clears the slot, and the cursor itself never changes. The player
 * inventory at the bottom is a normal container inventory, so items can be picked up and put
 * back.
 */
public final class ContainerAtRm extends Container {

    private final AtRmInventory inventory = new AtRmInventory();
    private final InventoryPlayer playerInventory;

    public ContainerAtRm(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;

        for (int row = 0; row < AtRmLayout.GRID; row++) {
            for (int col = 0; col < AtRmLayout.GRID; col++) {
                addSlotToContainer(
                    new GhostSlot(
                        inventory,
                        AtRmLayout.cellIndex(col, row),
                        AtRmLayout.gridX(col),
                        AtRmLayout.gridY(row)));
            }
        }
        addSlotToContainer(
            new GhostSlot(inventory, AtRmInventory.OUTPUT_SLOT, AtRmLayout.RESULT_X, AtRmLayout.RESULT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(
                    new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        AtRmLayout.PLAYER_SLOT_X + col * AtRmLayout.SLOT,
                        AtRmLayout.PLAYER_SLOT_Y + row * AtRmLayout.SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(
                new Slot(
                    playerInventory,
                    col,
                    AtRmLayout.PLAYER_SLOT_X + col * AtRmLayout.SLOT,
                    AtRmLayout.PLAYER_HOTBAR_Y));
        }
    }

    public AtRmInventory getGhostInventory() {
        return inventory;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer player) {
        if (slotId >= 0 && slotId < AtRmInventory.SIZE) {
            return clickGhostSlot(slotId, button, mode, player);
        }
        return super.slotClick(slotId, button, mode, player);
    }

    private ItemStack clickGhostSlot(int slotId, int button, int mode, EntityPlayer player) {
        // Only a plain left/right click edits a ghost slot. Shift-click, hotbar swap,
        // creative clone, drop and drag modes have no meaning here.
        if (mode != 0) {
            return null;
        }

        if (button == 1 || button == 2) {
            inventory.setInventorySlotContents(slotId, null);
        } else {
            ItemStack held = player.inventory.getItemStack();
            if (held == null) {
                inventory.setInventorySlotContents(slotId, null);
            } else {
                ItemStack copy = held.copy();
                if (slotId != AtRmInventory.OUTPUT_SLOT) {
                    copy.stackSize = 1;
                }
                inventory.setInventorySlotContents(slotId, copy);
            }
        }

        detectAndSendChanges();
        return null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = index >= 0 && index < inventorySlots.size() ? (Slot) inventorySlots.get(index) : null;
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        // Shift-clicking a ghost slot must not move its copy into the inventory.
        if (index < AtRmInventory.SIZE) {
            return null;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (!mergeItemStack(stack, AtRmInventory.SIZE, inventorySlots.size(), true)) {
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    public void clearGrid() {
        inventory.clearGrid();
        detectAndSendChanges();
    }

    public ItemStack getResult() {
        return inventory.getStackInSlot(AtRmInventory.OUTPUT_SLOT);
    }

    public InventoryPlayer getPlayerInventory() {
        return playerInventory;
    }
}
