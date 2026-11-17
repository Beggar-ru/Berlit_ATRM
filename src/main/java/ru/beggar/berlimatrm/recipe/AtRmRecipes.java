package ru.beggar.berlimatrm.recipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.oredict.OreDictionary;

import fox.spiteful.avaritia.crafting.ExtremeCraftingManager;
import ru.beggar.berlimatrm.CommonProxy;
import ru.beggar.berlimatrm.Config;

/**
 * Server-side registry of the recipes authored in the GUI plus the list of vanilla/Avaritia
 * recipes the player asked to delete.
 *
 * <p>
 * Recipes are applied directly to {@link ExtremeCraftingManager}, so no restart or
 * {@code /mt reload} is needed. They are persisted in {@code config/berlimatrm/recipes.dat}
 * (lossless NBT) and mirrored into {@code config/berlimatrm/BerlitATRM.zs} as a readable
 * MineTweaker export. The export is deliberately not auto-loaded: recipes survive even when the
 * script is edited or deleted.
 */
public final class AtRmRecipes {

    public enum Status {
        OK,
        DUPLICATE,
        NO_OUTPUT,
        NO_INPUT,
        NO_MATCH,
        INVALID,
        LIMIT
    }

    public static final class Result {

        public final Status status;
        public final int count;
        public final String detail;

        private Result(Status status, int count, String detail) {
            this.status = status;
            this.count = count;
            this.detail = detail;
        }

        public static Result ok(int count) {
            return new Result(Status.OK, count, null);
        }

        public static Result error(Status status) {
            return new Result(status, 0, null);
        }

        public static Result error(Status status, String detail) {
            return new Result(status, 0, detail);
        }
    }

    private static final List<AtRmRecipe> RECIPES = new ArrayList<>();
    private static final List<ItemStack> REMOVALS = new ArrayList<>();
    /** Persisted recipes currently registered with Avaritia, parallel to {@link #RECIPES}. */
    private static final List<IRecipe> APPLIED = new ArrayList<>();
    /** Recipes added by a MineTweaker script through {@link #addRuntime}. */
    private static final Map<String, IRecipe> RUNTIME = new LinkedHashMap<>();
    private static final Set<String> SIGNATURES = new HashSet<>();

    private static File dataFile;
    private static File exportFile;
    private static boolean loaded;

    private AtRmRecipes() {}

    public static void init(File configDirectory) {
        File dir = new File(configDirectory, "berlimatrm");
        if (!dir.exists() && !dir.mkdirs()) {
            CommonProxy.LOG.error("Could not create {}", dir);
        }
        dataFile = new File(dir, "recipes.dat");
        exportFile = new File(dir, "BerlitATRM.zs");
        loadFromDisk();
    }

    public static File getExportFile() {
        return exportFile;
    }

    public static File getDataFile() {
        return dataFile;
    }

    public static int size() {
        return RECIPES.size();
    }

    public static int removalCount() {
        return REMOVALS.size();
    }

    // ------------------------------------------------------------------ loading

    private static void loadFromDisk() {
        RECIPES.clear();
        REMOVALS.clear();
        APPLIED.clear();
        RUNTIME.clear();
        loaded = true;

        if (dataFile == null || !dataFile.isFile()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(dataFile)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            if (root == null) {
                return;
            }
            NBTTagList recipes = root.getTagList("Recipes", 10);
            for (int i = 0; i < recipes.tagCount(); i++) {
                AtRmRecipe recipe = AtRmRecipe.fromNbt(recipes.getCompoundTagAt(i));
                if (recipe != null) {
                    RECIPES.add(recipe);
                }
            }
            NBTTagList removed = root.getTagList("Removed", 10);
            for (int i = 0; i < removed.tagCount(); i++) {
                NBTTagCompound tag = removed.getCompoundTagAt(i);
                ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
                if (stack != null) {
                    REMOVALS.add(stack);
                }
            }
            CommonProxy.LOG.info("Loaded {} recipe(s) and {} removal(s)", RECIPES.size(), REMOVALS.size());
        } catch (Exception e) {
            CommonProxy.LOG.error("Could not read " + dataFile, e);
        }
    }

    /** Removes previously applied persisted recipes so a reload cannot register duplicates. */
    private static void clearApplied() {
        if (APPLIED.isEmpty()) {
            return;
        }
        List<Object> registry = ExtremeCraftingManager.getInstance()
            .getRecipeList();
        for (IRecipe recipe : APPLIED) {
            if (recipe != null) {
                registry.remove(recipe);
            }
        }
        APPLIED.clear();
    }

    /** Applies persisted removals and recipes; safe to call at every server start / reload. */
    public static void applyPersisted() {
        if (!loaded) {
            return;
        }
        clearApplied();
        for (ItemStack removal : REMOVALS) {
            removeMatchingFromRegistry(removal);
        }
        for (AtRmRecipe recipe : RECIPES) {
            // A script may already have registered the same recipe (for example after copying the
            // export into scripts/). Keep APPLIED parallel to RECIPES by storing a null marker.
            if (RUNTIME.containsKey(recipe.signature())) {
                APPLIED.add(null);
                continue;
            }
            IRecipe applied = recipe.createRecipe();
            ExtremeCraftingManager.getInstance()
                .getRecipeList()
                .add(applied);
            APPLIED.add(applied);
        }
        pruneRuntime();
        rebuildSignatures();
        CommonProxy.LOG.info("Applied {} recipe(s); {} removal rule(s) active", APPLIED.size(), REMOVALS.size());
    }

    // ------------------------------------------------------------------ GUI actions

    public static Result add(AtRmRecipe recipe) {
        if (recipe == null) {
            return Result.error(Status.INVALID);
        }
        if (RECIPES.size() >= Config.maxRecipes) {
            return Result.error(Status.LIMIT);
        }
        String signature = recipe.signature();
        if (SIGNATURES.contains(signature)) {
            return Result.error(Status.DUPLICATE);
        }
        String invalid = validate(recipe);
        if (invalid != null) {
            return Result.error(Status.INVALID, invalid);
        }

        IRecipe applied = recipe.createRecipe();
        ExtremeCraftingManager.getInstance()
            .getRecipeList()
            .add(applied);
        RECIPES.add(recipe);
        APPLIED.add(applied);
        SIGNATURES.add(signature);

        save();
        exportScript();
        return Result.ok(1);
    }

    public static Result remove(ItemStack output) {
        if (output == null) {
            return Result.error(Status.NO_OUTPUT);
        }
        int removed = removeEverywhere(output);
        ItemStack recorded = output;
        // A named/enchanted item picked up from the inventory is normally meant as "remove the
        // recipe for this item", not as an exact-NBT filter. Fall back to item+damage when the
        // tagged lookup found nothing.
        if (removed == 0 && output.hasTagCompound()) {
            ItemStack plain = output.copy();
            plain.setTagCompound(null);
            removed = removeEverywhere(plain);
            if (removed > 0) {
                recorded = plain;
            }
        }
        if (removed == 0) {
            return Result.error(Status.NO_MATCH);
        }
        if (!containsStack(REMOVALS, recorded)) {
            REMOVALS.add(recorded.copy());
        }
        save();
        exportScript();
        return Result.ok(removed);
    }

    /** Removes matching recipes from Avaritia, the persisted list and the script cache. */
    private static int removeEverywhere(ItemStack output) {
        int removed = removeMatchingFromRegistry(output);

        for (int i = RECIPES.size() - 1; i >= 0; i--) {
            if (matchesOutput(
                RECIPES.get(i)
                    .getOutput(),
                output)) {
                RECIPES.remove(i);
                if (i < APPLIED.size()) {
                    APPLIED.remove(i);
                }
            }
        }
        Iterator<Map.Entry<String, IRecipe>> iterator = RUNTIME.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, IRecipe> entry = iterator.next();
            if (matchesOutput(
                entry.getValue()
                    .getRecipeOutput(),
                output)) {
                iterator.remove();
            }
        }
        rebuildSignatures();
        return removed;
    }

    private static int removeMatchingFromRegistry(ItemStack output) {
        int removed = 0;
        List<Object> registry = ExtremeCraftingManager.getInstance()
            .getRecipeList();
        Iterator<Object> iterator = registry.iterator();
        while (iterator.hasNext()) {
            Object entry = iterator.next();
            if (entry instanceof IRecipe) {
                IRecipe recipe = (IRecipe) entry;
                ItemStack result = recipe.getRecipeOutput();
                if (matchesOutput(result, output)) {
                    iterator.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    // ------------------------------------------------------------------ MineTweaker runtime

    /** Adds a script recipe without touching the config; used by the ZenScript API. */
    public static IRecipe addRuntime(AtRmRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        String invalid = validate(recipe);
        if (invalid != null) {
            CommonProxy.LOG
                .warn("Rejected script recipe ({}): {}", invalid, RecipeTokens.outputToken(recipe.getOutput()));
            return null;
        }
        String signature = recipe.signature();
        if (SIGNATURES.contains(signature)) {
            return null;
        }
        IRecipe applied = recipe.createRecipe();
        ExtremeCraftingManager.getInstance()
            .getRecipeList()
            .add(applied);
        RUNTIME.put(signature, applied);
        SIGNATURES.add(signature);
        return applied;
    }

    /** Removes recipes for an output and returns what was removed, for undo support. */
    public static List<IRecipe> removeRuntime(ItemStack output) {
        List<IRecipe> removed = new ArrayList<>();
        List<Object> registry = ExtremeCraftingManager.getInstance()
            .getRecipeList();
        Iterator<Object> iterator = registry.iterator();
        while (iterator.hasNext()) {
            Object entry = iterator.next();
            if (entry instanceof IRecipe && matchesOutput(((IRecipe) entry).getRecipeOutput(), output)) {
                iterator.remove();
                removed.add((IRecipe) entry);
            }
        }

        for (int i = RECIPES.size() - 1; i >= 0; i--) {
            if (matchesOutput(
                RECIPES.get(i)
                    .getOutput(),
                output)) {
                RECIPES.remove(i);
                if (i < APPLIED.size()) {
                    APPLIED.remove(i);
                }
            }
        }
        Iterator<Map.Entry<String, IRecipe>> runtime = RUNTIME.entrySet()
            .iterator();
        while (runtime.hasNext()) {
            if (matchesOutput(
                runtime.next()
                    .getValue()
                    .getRecipeOutput(),
                output)) {
                runtime.remove();
            }
        }
        rebuildSignatures();
        return removed;
    }

    public static void reAddRuntime(List<IRecipe> recipes) {
        if (recipes == null) {
            return;
        }
        for (IRecipe recipe : recipes) {
            ExtremeCraftingManager.getInstance()
                .getRecipeList()
                .add(recipe);
        }
    }

    public static void removeRuntimeRecipe(IRecipe recipe) {
        if (recipe == null) {
            return;
        }
        ExtremeCraftingManager.getInstance()
            .getRecipeList()
            .remove(recipe);
        RUNTIME.values()
            .remove(recipe);
        rebuildSignatures();
    }

    // ------------------------------------------------------------------ helpers

    private static void rebuildSignatures() {
        SIGNATURES.clear();
        for (int i = 0; i < RECIPES.size(); i++) {
            // Only recipes that are actually registered count. A persisted recipe skipped because a
            // script already added it must not block that script after a MineTweaker rollback.
            if (i < APPLIED.size() && APPLIED.get(i) != null) {
                SIGNATURES.add(
                    RECIPES.get(i)
                        .signature());
            }
        }
        SIGNATURES.addAll(RUNTIME.keySet());
    }

    /** Drops script entries whose recipe is no longer registered (removed by a removal rule). */
    private static void pruneRuntime() {
        List<Object> registry = ExtremeCraftingManager.getInstance()
            .getRecipeList();
        Iterator<Map.Entry<String, IRecipe>> iterator = RUNTIME.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            if (!registry.contains(
                iterator.next()
                    .getValue())) {
                iterator.remove();
            }
        }
    }

    private static boolean matchesOutput(ItemStack recipeOutput, ItemStack target) {
        if (recipeOutput == null || target == null) {
            return false;
        }
        if (recipeOutput.getItem() != target.getItem()) {
            return false;
        }
        if (target.getItemDamage() != OreDictionary.WILDCARD_VALUE
            && recipeOutput.getItemDamage() != target.getItemDamage()) {
            return false;
        }
        return NbtUtil.contains(target.getTagCompound(), recipeOutput.getTagCompound());
    }

    private static boolean containsStack(List<ItemStack> stacks, ItemStack target) {
        for (ItemStack stack : stacks) {
            if (matchesOutput(stack, target)) {
                return true;
            }
        }
        return false;
    }

    private static String validate(AtRmRecipe recipe) {
        ItemStack output = recipe.getOutput();
        if (output == null || output.getItem() == null) {
            return "invalid output";
        }
        if (recipe.isShaped()) {
            if (recipe.getWidth() <= 0 || recipe.getHeight() <= 0) {
                return "empty grid";
            }
            for (int row = 0; row < recipe.getHeight(); row++) {
                for (int col = 0; col < recipe.getWidth(); col++) {
                    String problem = validateIngredient(recipe.getCell(col, row));
                    if (problem != null) {
                        return problem;
                    }
                }
            }
        } else {
            if (recipe.getItems() == null || recipe.getItems().length == 0) {
                return "empty ingredient list";
            }
            for (AtRmIngredient ingredient : recipe.getItems()) {
                String problem = validateIngredient(ingredient);
                if (problem != null) {
                    return problem;
                }
            }
        }
        return null;
    }

    private static String validateIngredient(AtRmIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty() || ingredient.isAnyItem()) {
            return null;
        }
        if (ingredient.getOreName() != null) {
            if (OreDictionary.getOres(ingredient.getOreName())
                .isEmpty()) {
                return "unknown ore dictionary entry " + ingredient.getOreName();
            }
            return null;
        }
        ItemStack stack = ingredient.getStack();
        if (stack == null || stack.getItem() == null) {
            return "invalid ingredient";
        }
        return null;
    }

    // ------------------------------------------------------------------ persistence

    public static void save() {
        if (dataFile == null) {
            return;
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 1);

        NBTTagList recipes = new NBTTagList();
        for (AtRmRecipe recipe : RECIPES) {
            recipes.appendTag(recipe.writeToNbt());
        }
        root.setTag("Recipes", recipes);

        NBTTagList removed = new NBTTagList();
        for (ItemStack stack : REMOVALS) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            removed.appendTag(tag);
        }
        root.setTag("Removed", removed);

        File temp = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            CompressedStreamTools.writeCompressed(root, out);
        } catch (IOException e) {
            CommonProxy.LOG.error("Could not write " + temp, e);
            return;
        }
        try {
            Files.move(temp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            CommonProxy.LOG.error("Could not replace " + dataFile, e);
        }
    }

    public static void exportScript() {
        if (!Config.exportScript || exportFile == null) {
            return;
        }
        File temp = new File(exportFile.getParentFile(), exportFile.getName() + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp), "UTF-8")) {
            writer.write("// Berlit_ATRM - Avaritia 9x9 recipes generated by the in-game GUI.\n");
            writer.write("// Source of truth: config/berlimatrm/recipes.dat (edit recipes in game).\n");
            writer.write("// This file is an export only and is NOT loaded automatically.\n");
            writer.write("// Copy the lines you need into scripts/ if you also want MineTweaker to apply them.\n");
            writer.write("//\n");
            writer.write("// Removals are written before additions on purpose: a recipe for the same output\n");
            writer.write("// that was added by the GUI must survive the removal rule on reload.\n\n");

            writer.write("// ---- removals ----\n");
            for (ItemStack removal : REMOVALS) {
                String token = RecipeTokens.removalToken(removal);
                if (token == null) {
                    writer.write("// skipped removal: unrepresentable item " + removal + "\n");
                    continue;
                }
                writer.write("mods.berlimatrm.Avaritia.remove(" + token + ");\n");
            }

            writer.write("\n// ---- additions ----\n");
            for (AtRmRecipe recipe : RECIPES) {
                List<String> lines = recipe.scriptLines();
                if (lines.isEmpty()) {
                    writer.write(
                        "// skipped recipe: output " + RecipeTokens.outputToken(recipe.getOutput())
                            + " uses an NBT tag that cannot be written as a MineTweaker literal.\n");
                    continue;
                }
                writer.write("// " + RecipeTokens.outputToken(recipe.getOutput()) + "\n");
                for (String line : lines) {
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            CommonProxy.LOG.error("Could not write " + temp, e);
            return;
        }
        try {
            Files.move(temp.toPath(), exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            CommonProxy.LOG.error("Could not replace " + exportFile, e);
        }
    }

    /** Reloads recipes.dat and re-applies it; used by {@code /atrm reload}. */
    public static void reload() {
        loadFromDisk();
        applyPersisted();
        exportScript();
        CommonProxy.LOG.info("Reloaded {} recipe(s)", RECIPES.size());
    }

    /** Used by the selftest to start from a clean state. */
    public static void clearForTest() {
        clearApplied();
        RECIPES.clear();
        REMOVALS.clear();
        RUNTIME.clear();
        SIGNATURES.clear();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static ExtremeCraftingManager manager() {
        return ExtremeCraftingManager.getInstance();
    }

    public static void logSelfTestSummary() {
        CommonProxy.LOG.info(
            "BERLIT_ATRM_SELFTEST store: recipes={} removals={} registry={}",
            RECIPES.size(),
            REMOVALS.size(),
            manager().getRecipeList()
                .size());
    }
}
