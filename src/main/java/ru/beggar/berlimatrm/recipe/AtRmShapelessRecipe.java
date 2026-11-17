package ru.beggar.berlimatrm.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/** Shapeless 9x9 recipe built by the GUI, with NBT-aware ingredient matching. */
public final class AtRmShapelessRecipe implements IRecipe {

    private final ItemStack output;
    private final AtRmIngredient[] items;

    public AtRmShapelessRecipe(ItemStack output, AtRmIngredient[] items) {
        this.output = output.copy();
        this.items = items;
    }

    @Override
    public boolean matches(InventoryCrafting matrix, World world) {
        int filled = 0;
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            if (matrix.getStackInSlot(i) != null) {
                filled++;
            }
        }
        if (filled != items.length) {
            return false;
        }

        boolean[] used = new boolean[matrix.getSizeInventory()];
        for (AtRmIngredient ingredient : items) {
            boolean found = false;
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                if (used[i]) {
                    continue;
                }
                if (ingredient.matches(matrix.getStackInSlot(i))) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting matrix) {
        return output.copy();
    }

    @Override
    public int getRecipeSize() {
        return items.length;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return output;
    }
}
