package ru.beggar.berlimatrm.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** A fake slot: it renders a copy of the ingredient and never lets the player take it out. */
public final class GhostSlot extends Slot {

    public GhostSlot(IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {}

    @Override
    public void putStack(ItemStack stack) {
        inventory.setInventorySlotContents(getSlotIndex(), stack);
        onSlotChanged();
    }
}
