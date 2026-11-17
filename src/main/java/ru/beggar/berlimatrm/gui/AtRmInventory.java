package ru.beggar.berlimatrm.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** The 9x9 ghost matrix plus the result slot; slots copy the cursor stack instead of moving it. */
public final class AtRmInventory implements IInventory {

    public static final int GRID_SLOTS = 81;
    public static final int OUTPUT_SLOT = 81;
    public static final int SIZE = 82;

    private final ItemStack[] stacks = new ItemStack[SIZE];

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < SIZE ? stacks[index] : null;
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (index < 0 || index >= SIZE || stacks[index] == null) {
            return null;
        }
        ItemStack stack = stacks[index];
        if (stack.stackSize <= amount) {
            stacks[index] = null;
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            stacks[index] = null;
        }
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return getStackInSlot(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < SIZE) {
            stacks[index] = stack;
        }
    }

    @Override
    public String getInventoryName() {
        return "container.berlimatrm.recipe";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    public void clearGrid() {
        for (int i = 0; i < GRID_SLOTS; i++) {
            stacks[i] = null;
        }
    }

    public void clearAll() {
        for (int i = 0; i < SIZE; i++) {
            stacks[i] = null;
        }
    }
}
