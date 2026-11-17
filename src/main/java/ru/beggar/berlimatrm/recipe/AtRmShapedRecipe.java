package ru.beggar.berlimatrm.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/**
 * Shaped 9x9 recipe built by the GUI.
 *
 * <p>
 * Matching mirrors {@code ExtremeShapedOreRecipe}: the pattern may sit anywhere in the crafting
 * matrix, empty cells inside its bounding box must stay empty, and the mirrored layout can be
 * accepted too. Unlike Avaritia's built-in classes, ingredients can carry NBT filters.
 */
public final class AtRmShapedRecipe implements IRecipe {

    private static final int MAX_GRID = 9;

    private final ItemStack output;
    private final int width;
    private final int height;
    private final AtRmIngredient[] cells;
    private final boolean mirrored;

    public AtRmShapedRecipe(ItemStack output, int width, int height, AtRmIngredient[] cells, boolean mirrored) {
        this.output = output.copy();
        this.width = width;
        this.height = height;
        this.cells = cells;
        this.mirrored = mirrored;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    @Override
    public boolean matches(InventoryCrafting matrix, World world) {
        for (int x = 0; x <= MAX_GRID - width; x++) {
            for (int y = 0; y <= MAX_GRID - height; y++) {
                if (checkMatch(matrix, x, y, false)) {
                    return true;
                }
                if (mirrored && checkMatch(matrix, x, y, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMatch(InventoryCrafting matrix, int startX, int startY, boolean mirror) {
        for (int col = 0; col < MAX_GRID; col++) {
            for (int row = 0; row < MAX_GRID; row++) {
                int subX = col - startX;
                int subY = row - startY;

                AtRmIngredient expected;
                if (subX >= 0 && subY >= 0 && subX < width && subY < height) {
                    int index = (mirror ? width - subX - 1 : subX) + subY * width;
                    expected = cells[index] == null ? AtRmIngredient.EMPTY : cells[index];
                } else {
                    expected = AtRmIngredient.EMPTY;
                }

                ItemStack actual = matrix.getStackInRowAndColumn(col, row);
                if (!expected.matches(actual)) {
                    return false;
                }
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
        return width * height;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return output;
    }
}
