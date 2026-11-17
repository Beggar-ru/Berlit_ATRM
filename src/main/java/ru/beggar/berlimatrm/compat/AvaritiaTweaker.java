package ru.beggar.berlimatrm.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;
import ru.beggar.berlimatrm.recipe.AtRmIngredient;
import ru.beggar.berlimatrm.recipe.AtRmRecipe;
import ru.beggar.berlimatrm.recipe.AtRmRecipes;
import ru.beggar.berlimatrm.recipe.RecipeTokens;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * MineTweaker / CraftTweaker API for Avaritia's 9x9 recipes, including NBT filters.
 *
 * <pre>
 * import mods.berlimatrm.Avaritia;
 *
 * // 9 rows of 9 ingredients; null means an empty cell.
 * Avaritia.addShaped(&lt;minecraft:diamond&gt;, [
 *     [&lt;minecraft:iron_block&gt;, null, ...],
 *     ...
 * ], true);
 *
 * Avaritia.addShapeless(&lt;minecraft:diamond&gt;, [&lt;ore:ingotIron&gt;, &lt;minecraft:coal&gt;]);
 * Avaritia.remove(&lt;minecraft:diamond&gt;);
 * </pre>
 */
@ZenClass("mods.berlimatrm.Avaritia")
public final class AvaritiaTweaker {

    private AvaritiaTweaker() {}

    public static void register() {
        MineTweakerAPI.registerClass(AvaritiaTweaker.class);
    }

    @ZenMethod
    public static void addShaped(IItemStack output, IIngredient[][] grid, @Optional boolean mirrored) {
        ItemStack result = MineTweakerMC.getItemStack(output);
        if (result == null || grid == null) {
            MineTweakerAPI.logError("Avaritia.addShaped: invalid output or grid");
            return;
        }

        AtRmIngredient[] cells = new AtRmIngredient[AtRmRecipe.CELLS];
        java.util.Arrays.fill(cells, AtRmIngredient.EMPTY);
        for (int row = 0; row < grid.length && row < AtRmRecipe.GRID; row++) {
            IIngredient[] sourceRow = grid[row];
            if (sourceRow == null) {
                continue;
            }
            for (int col = 0; col < sourceRow.length && col < AtRmRecipe.GRID; col++) {
                cells[col + row * AtRmRecipe.GRID] = convert(sourceRow[col]);
            }
        }

        AtRmRecipe recipe = AtRmRecipe.shaped(result, cells, mirrored);
        if (recipe == null) {
            MineTweakerAPI.logError("Avaritia.addShaped: the grid is empty");
            return;
        }
        MineTweakerAPI.apply(new AddAction(recipe));
    }

    @ZenMethod
    public static void addShapeless(IItemStack output, IIngredient[] ingredients) {
        ItemStack result = MineTweakerMC.getItemStack(output);
        if (result == null || ingredients == null) {
            MineTweakerAPI.logError("Avaritia.addShapeless: invalid output or ingredients");
            return;
        }
        List<AtRmIngredient> list = new ArrayList<>();
        for (IIngredient ingredient : ingredients) {
            AtRmIngredient converted = convert(ingredient);
            if (converted != null && !converted.isEmpty()) {
                list.add(converted);
            }
        }
        AtRmRecipe recipe = AtRmRecipe.shapeless(result, list);
        if (recipe == null) {
            MineTweakerAPI.logError("Avaritia.addShapeless: no ingredients");
            return;
        }
        MineTweakerAPI.apply(new AddAction(recipe));
    }

    @ZenMethod
    public static void remove(IItemStack output) {
        ItemStack target = MineTweakerMC.getItemStack(output);
        if (target == null) {
            MineTweakerAPI.logError("Avaritia.remove: invalid item");
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(target));
    }

    private static AtRmIngredient convert(IIngredient ingredient) {
        if (ingredient == null) {
            return AtRmIngredient.EMPTY;
        }
        if (ingredient instanceof IOreDictEntry) {
            return AtRmIngredient.ore(((IOreDictEntry) ingredient).getName(), null);
        }
        if (ingredient instanceof IItemStack) {
            ItemStack stack = MineTweakerMC.getItemStack((IItemStack) ingredient);
            if (stack == null) {
                return AtRmIngredient.EMPTY;
            }
            return AtRmIngredient
                .item(stack, stack.getItemDamage() == OreDictionary.WILDCARD_VALUE, stack.getTagCompound());
        }
        // <*> and compound ingredients: match any non-empty stack.
        return AtRmIngredient.ANY_ITEM;
    }

    private static final class AddAction implements IUndoableAction {

        private final AtRmRecipe recipe;
        private IRecipe added;

        private AddAction(AtRmRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            added = AtRmRecipes.addRuntime(recipe);
        }

        @Override
        public boolean canUndo() {
            return added != null;
        }

        @Override
        public void undo() {
            AtRmRecipes.removeRuntimeRecipe(added);
            added = null;
        }

        @Override
        public String describe() {
            return "Adding Avaritia recipe for " + RecipeTokens.outputToken(recipe.getOutput());
        }

        @Override
        public String describeUndo() {
            return "Removing Avaritia recipe for " + RecipeTokens.outputToken(recipe.getOutput());
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class RemoveAction implements IUndoableAction {

        private final ItemStack target;
        private List<IRecipe> removed;

        private RemoveAction(ItemStack target) {
            this.target = target;
        }

        @Override
        public void apply() {
            removed = AtRmRecipes.removeRuntime(target);
        }

        @Override
        public boolean canUndo() {
            return removed != null && !removed.isEmpty();
        }

        @Override
        public void undo() {
            AtRmRecipes.reAddRuntime(removed);
        }

        @Override
        public String describe() {
            return "Removing Avaritia recipe(s) for " + RecipeTokens.outputToken(target);
        }

        @Override
        public String describeUndo() {
            return "Restoring Avaritia recipe(s) for " + RecipeTokens.outputToken(target);
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
