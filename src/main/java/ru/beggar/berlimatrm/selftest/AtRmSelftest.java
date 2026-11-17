package ru.beggar.berlimatrm.selftest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;

import fox.spiteful.avaritia.crafting.ExtremeCraftingManager;
import ru.beggar.berlimatrm.CommonProxy;
import ru.beggar.berlimatrm.recipe.AtRmIngredient;
import ru.beggar.berlimatrm.recipe.AtRmRecipe;
import ru.beggar.berlimatrm.recipe.AtRmRecipes;
import ru.beggar.berlimatrm.recipe.NbtUtil;

/**
 * Headless smoke test, enabled with {@code BERLIT_ATRM_SELFTEST=1}:
 * shaped, NBT-filtered, ore-dictionary and shapeless recipes plus removal.
 */
public final class AtRmSelftest {

    private static final Container DUMMY = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }
    };

    private AtRmSelftest() {}

    public static void run() {
        int passed = 0;
        int failed = 0;
        try {
            ExtremeCraftingManager manager = ExtremeCraftingManager.getInstance();

            ItemStack shapedOutput = tagged(new ItemStack(Items.diamond), "ATRM shaped test");
            AtRmIngredient[] grid = new AtRmIngredient[AtRmRecipe.CELLS];
            Arrays.fill(grid, AtRmIngredient.item(new ItemStack(Items.iron_ingot), false, null));
            IRecipe shaped = AtRmRecipes.addRuntime(AtRmRecipe.shaped(shapedOutput, grid, true));

            InventoryCrafting shapedInv = inventory();
            for (int i = 0; i < AtRmRecipe.CELLS; i++) {
                shapedInv.setInventorySlotContents(i, new ItemStack(Items.iron_ingot));
            }
            ItemStack shapedMatch = manager.findMatchingRecipe(shapedInv, null);
            boolean shapedOk = shaped != null && shapedMatch != null && shapedMatch.getItem() == Items.diamond;
            passed += shapedOk ? 1 : 0;
            failed += shapedOk ? 0 : 1;
            log("shaped", shapedOk);

            // NBT filter: a diamond with this exact tag must be required.
            NBTTagCompound filter = new NBTTagCompound();
            NBTTagCompound display = new NBTTagCompound();
            display.setString("Name", "ATRM NBT test");
            filter.setTag("display", display);
            ItemStack nbtOutput = tagged(new ItemStack(Items.emerald), "ATRM nbt output");
            AtRmIngredient[] nbtGrid = new AtRmIngredient[AtRmRecipe.CELLS];
            Arrays.fill(nbtGrid, AtRmIngredient.EMPTY);
            nbtGrid[40] = AtRmIngredient.item(new ItemStack(Items.diamond), false, filter);
            IRecipe nbtRecipe = AtRmRecipes.addRuntime(AtRmRecipe.shaped(nbtOutput, nbtGrid, false));

            InventoryCrafting nbtInv = inventory();
            nbtInv.setInventorySlotContents(40, new ItemStack(Items.diamond));
            boolean plainRejected = nbtRecipe != null && manager.findMatchingRecipe(nbtInv, null) == null;
            ItemStack taggedDiamond = new ItemStack(Items.diamond);
            NBTTagCompound actual = new NBTTagCompound();
            NBTTagCompound actualDisplay = new NBTTagCompound();
            actualDisplay.setString("Name", "ATRM NBT test");
            actualDisplay.setInteger("Color", 7);
            actual.setTag("display", actualDisplay);
            taggedDiamond.setTagCompound(actual);
            nbtInv.setInventorySlotContents(40, taggedDiamond);
            ItemStack nbtMatch = manager.findMatchingRecipe(nbtInv, null);
            boolean nbtOk = plainRejected && nbtMatch != null && nbtMatch.getItem() == Items.emerald;
            passed += nbtOk ? 1 : 0;
            failed += nbtOk ? 0 : 1;
            log("nbt", nbtOk);

            // Ore dictionary ingredient.
            ItemStack oreOutput = tagged(new ItemStack(Items.gold_ingot), "ATRM ore output");
            AtRmIngredient[] oreGrid = new AtRmIngredient[AtRmRecipe.CELLS];
            Arrays.fill(oreGrid, AtRmIngredient.EMPTY);
            oreGrid[0] = AtRmIngredient.ore("ingotIron", null);
            IRecipe oreRecipe = AtRmRecipes.addRuntime(AtRmRecipe.shaped(oreOutput, oreGrid, false));
            InventoryCrafting oreInv = inventory();
            oreInv.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
            ItemStack oreMatch = manager.findMatchingRecipe(oreInv, null);
            boolean oreOk = oreRecipe != null && oreMatch != null && oreMatch.getItem() == Items.gold_ingot;
            passed += oreOk ? 1 : 0;
            failed += oreOk ? 0 : 1;
            log("ore", oreOk);

            // Shapeless recipe, ingredients in arbitrary cells.
            ItemStack shapelessOutput = tagged(new ItemStack(Items.redstone), "ATRM shapeless output");
            IRecipe shapeless = AtRmRecipes.addRuntime(
                AtRmRecipe.shapeless(
                    shapelessOutput,
                    Arrays.asList(
                        AtRmIngredient.item(new ItemStack(Items.rotten_flesh), false, null),
                        AtRmIngredient.item(new ItemStack(Items.slime_ball), false, null))));
            InventoryCrafting shapelessInv = inventory();
            shapelessInv.setInventorySlotContents(3, new ItemStack(Items.slime_ball));
            shapelessInv.setInventorySlotContents(80, new ItemStack(Items.rotten_flesh));
            ItemStack shapelessMatch = manager.findMatchingRecipe(shapelessInv, null);
            boolean shapelessOk = shapeless != null && shapelessMatch != null
                && shapelessMatch.getItem() == Items.redstone;
            passed += shapelessOk ? 1 : 0;
            failed += shapelessOk ? 0 : 1;
            log("shapeless", shapelessOk);

            // Removal by output, ignoring recipes without the filter tag.
            ItemStack removeOutput = tagged(new ItemStack(Items.diamond), "ATRM remove test");
            AtRmIngredient[] removeGrid = new AtRmIngredient[AtRmRecipe.CELLS];
            Arrays.fill(removeGrid, AtRmIngredient.item(new ItemStack(Items.gold_ingot), false, null));
            IRecipe removeRecipe = AtRmRecipes.addRuntime(AtRmRecipe.shaped(removeOutput, removeGrid, false));
            InventoryCrafting removeInv = inventory();
            for (int i = 0; i < AtRmRecipe.CELLS; i++) {
                removeInv.setInventorySlotContents(i, new ItemStack(Items.gold_ingot));
            }
            int removed = AtRmRecipes.removeRuntime(removeOutput)
                .size();
            boolean removalOk = removeRecipe != null && removed >= 1
                && manager.findMatchingRecipe(removeInv, null) == null;
            passed += removalOk ? 1 : 0;
            failed += removalOk ? 0 : 1;
            log("removal", removalOk);

            // Cleanup runtime test recipes.
            AtRmRecipes.removeRuntime(shapedOutput);
            AtRmRecipes.removeRuntime(nbtOutput);
            AtRmRecipes.removeRuntime(oreOutput);
            AtRmRecipes.removeRuntime(shapelessOutput);
            AtRmRecipes.removeRuntime(removeOutput);

            // The GUI's NBT text field uses SNBT; it must parse back into the same filter.
            NBTTagCompound parsed = NbtUtil.parse("{display:{Name:\"ATRM parse test\"}}");
            boolean parseOk = parsed != null && "ATRM parse test".equals(
                parsed.getCompoundTag("display")
                    .getString("Name"));
            passed += parseOk ? 1 : 0;
            failed += parseOk ? 0 : 1;
            log("nbt_text", parseOk);

            // Persistence: save, reload from disk, verify, then restore the original config.
            File dataFile = AtRmRecipes.getDataFile();
            File exportFile = AtRmRecipes.getExportFile();
            byte[] dataBackup = readFile(dataFile);
            byte[] exportBackup = readFile(exportFile);

            ItemStack persistOutput = tagged(new ItemStack(Items.diamond), "ATRM persist test");
            AtRmIngredient[] persistGrid = new AtRmIngredient[AtRmRecipe.CELLS];
            Arrays.fill(persistGrid, AtRmIngredient.item(new ItemStack(Items.iron_ingot), false, null));
            AtRmRecipes.Result addResult = AtRmRecipes.add(AtRmRecipe.shaped(persistOutput, persistGrid, false));
            boolean savedOk = addResult.status == AtRmRecipes.Status.OK && dataFile != null && dataFile.isFile();
            passed += savedOk ? 1 : 0;
            failed += savedOk ? 0 : 1;
            log("persist_save", savedOk);

            AtRmRecipes.reload();
            InventoryCrafting persistInv = inventory();
            for (int i = 0; i < AtRmRecipe.CELLS; i++) {
                persistInv.setInventorySlotContents(i, new ItemStack(Items.iron_ingot));
            }
            ItemStack persistMatch = manager.findMatchingRecipe(persistInv, null);
            boolean reloadOk = persistMatch != null && persistMatch.getItem() == Items.diamond
                && persistMatch.hasTagCompound();
            byte[] exportBytes = readFile(exportFile);
            String exportText = exportBytes == null ? "" : new String(exportBytes, "UTF-8");
            boolean exportOk = exportText.contains("addShaped(") && exportText.contains("ATRM persist test");
            passed += reloadOk ? 1 : 0;
            failed += reloadOk ? 0 : 1;
            log("persist_reload", reloadOk);
            passed += exportOk ? 1 : 0;
            failed += exportOk ? 0 : 1;
            log("export", exportOk);

            AtRmRecipes.Result removeResult = AtRmRecipes.remove(persistOutput);
            boolean removeOk = removeResult.status == AtRmRecipes.Status.OK && !hasRecipeFor(manager, persistOutput);
            passed += removeOk ? 1 : 0;
            failed += removeOk ? 0 : 1;
            log("persist_remove", removeOk);

            restoreFile(dataFile, dataBackup);
            restoreFile(exportFile, exportBackup);
            AtRmRecipes.reload();

            AtRmRecipes.logSelfTestSummary();
        } catch (Throwable t) {
            failed++;
            CommonProxy.LOG.error("BERLIT_ATRM_SELFTEST crashed", t);
        }

        CommonProxy.LOG.info("BERLIT_ATRM_SELFTEST summary: passed={} failed={}", passed, failed);
    }

    private static InventoryCrafting inventory() {
        return new InventoryCrafting(DUMMY, 9, 9);
    }

    private static boolean hasRecipeFor(ExtremeCraftingManager manager, ItemStack target) {
        for (Object entry : manager.getRecipeList()) {
            if (!(entry instanceof IRecipe)) {
                continue;
            }
            ItemStack output = ((IRecipe) entry).getRecipeOutput();
            if (output == null || output.getItem() != target.getItem()) {
                continue;
            }
            if (output.getItemDamage() != target.getItemDamage()) {
                continue;
            }
            if (NbtUtil.contains(target.getTagCompound(), output.getTagCompound())) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack tagged(ItemStack stack, String name) {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", name);
        tag.setTag("display", display);
        stack.setTagCompound(tag);
        return stack;
    }

    private static byte[] readFile(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            CommonProxy.LOG.error("Selftest could not read " + file, e);
            return null;
        }
    }

    private static void restoreFile(File file, byte[] bytes) {
        if (file == null) {
            return;
        }
        try {
            if (bytes == null) {
                Files.deleteIfExists(file.toPath());
            } else {
                Files.write(file.toPath(), bytes);
            }
        } catch (IOException e) {
            CommonProxy.LOG.error("Selftest could not restore " + file, e);
        }
    }

    private static void log(String name, boolean ok) {
        CommonProxy.LOG.info("BERLIT_ATRM_SELFTEST {}: {}", name, ok ? "ok" : "FAILED");
    }
}
