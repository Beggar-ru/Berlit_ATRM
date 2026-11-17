package ru.beggar.berlimatrm.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import ru.beggar.berlimatrm.network.ModNetwork;
import ru.beggar.berlimatrm.network.PacketAtRmApply;
import ru.beggar.berlimatrm.network.PacketAtRmClear;
import ru.beggar.berlimatrm.recipe.AtRmIngredient;
import ru.beggar.berlimatrm.recipe.AtRmRecipe;
import ru.beggar.berlimatrm.recipe.NbtUtil;

/**
 * The recipe maker screen: 9x9 ghost grid with a "*" selector per cell, a selected-item strip
 * above it, a recipe-mode menu (shaped / shapeless / remove) and per-cell ingredient options
 * (ore dictionary, damage wildcard, any item, NBT tag).
 */
public class GuiAtRm extends net.minecraft.client.gui.inventory.GuiContainer {

    private static final int ID_MODE_SHAPED = 0;
    private static final int ID_MODE_SHAPELESS = 1;
    private static final int ID_MODE_REMOVE = 2;
    private static final int ID_MIRRORED = 3;
    private static final int ID_CLEAR = 4;
    private static final int ID_OK = 5;
    private static final int ID_CLOSE = 6;
    private static final int ID_ORE_PREV = 7;
    private static final int ID_ORE_NEXT = 8;
    private static final int ID_RESET = 9;
    private static final int ID_FROM_ITEM = 10;
    private static final int ID_ORE_BOX = 11;
    private static final int ID_ANY_DAMAGE_BOX = 12;
    private static final int ID_ANY_ITEM_BOX = 13;

    private static final int STAR_SIZE = 9;

    private enum Mode {
        SHAPED,
        SHAPELESS,
        REMOVE
    }

    private final ContainerAtRm container;
    private final CellOptions[] options = new CellOptions[AtRmLayout.GRID * AtRmLayout.GRID];
    private final ItemStack[] snapshot = new ItemStack[AtRmLayout.GRID * AtRmLayout.GRID];

    private Mode mode = Mode.SHAPED;
    private boolean mirrored = true;
    private int selected = -1;
    private int nbtSlot = -2;

    private ModeButton[] modeButtons;
    private AtRmCheckbox mirroredBox;
    private AtRmCheckbox oreBox;
    private AtRmCheckbox anyDamageBox;
    private AtRmCheckbox anyItemBox;
    private ThemedTextField nbtField;
    private ThemedButton orePrev;
    private ThemedButton oreNext;
    private ThemedButton resetButton;
    private ThemedButton fromItemButton;
    private ThemedButton clearButton;
    private ThemedButton okButton;

    private String statusKey;
    private String statusDetail;
    private boolean statusOk;
    private int statusTicks;

    /** GUI scale we temporarily reduced for this screen, or -1 when no change was needed. */
    private int previousGuiScale = -1;
    private boolean switchingScale;

    public GuiAtRm(ContainerAtRm container) {
        super(container);
        this.container = container;
        this.xSize = AtRmLayout.WIDTH;
        this.ySize = AtRmLayout.HEIGHT;
        for (int i = 0; i < options.length; i++) {
            options[i] = new CellOptions();
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void initGui() {
        if (adaptGuiScale()) {
            return;
        }
        super.initGui();

        int left = guiLeft;
        int top = guiTop;

        modeButtons = new ModeButton[3];
        String[] modeLabels = { tr("berlimatrm.gui.mode.shaped"), tr("berlimatrm.gui.mode.shapeless"),
            tr("berlimatrm.gui.mode.remove") };
        for (int i = 0; i < modeButtons.length; i++) {
            modeButtons[i] = new ModeButton(
                i,
                left + AtRmLayout.MENU_X + 6,
                top + AtRmLayout.MODE_Y[i],
                AtRmLayout.MENU_WIDTH - 12,
                AtRmLayout.MODE_HEIGHT,
                modeLabels[i]);
            buttonList.add(modeButtons[i]);
        }

        mirroredBox = new AtRmCheckbox(
            ID_MIRRORED,
            left + AtRmLayout.MIRROR_X,
            top + AtRmLayout.MIRROR_Y,
            tr("berlimatrm.gui.mirrored"),
            mirrored);
        buttonList.add(mirroredBox);

        clearButton = new ThemedButton(
            ID_CLEAR,
            left + AtRmLayout.CLEAR_X,
            top + AtRmLayout.CLEAR_Y,
            AtRmLayout.CLEAR_WIDTH,
            AtRmLayout.CLEAR_HEIGHT,
            tr("berlimatrm.gui.clear"));
        buttonList.add(clearButton);

        okButton = new ThemedButton(
            ID_OK,
            left + AtRmLayout.OK_X,
            top + AtRmLayout.OK_Y,
            AtRmLayout.OK_WIDTH,
            AtRmLayout.OK_HEIGHT,
            tr("berlimatrm.gui.ok"));
        buttonList.add(okButton);

        buttonList.add(
            new ThemedButton(
                ID_CLOSE,
                left + AtRmLayout.CLOSE_X,
                top + AtRmLayout.CLOSE_Y,
                AtRmLayout.CLOSE_WIDTH,
                AtRmLayout.CLOSE_HEIGHT,
                tr("berlimatrm.gui.close")));

        int checkX = left + AtRmLayout.OPTIONS_INNER_X;
        oreBox = new AtRmCheckbox(ID_ORE_BOX, checkX, top + AtRmLayout.CHECKBOX_Y, tr("berlimatrm.gui.ore"), false);
        buttonList.add(oreBox);
        checkX += oreBox.width + 8;

        anyDamageBox = new AtRmCheckbox(
            ID_ANY_DAMAGE_BOX,
            checkX,
            top + AtRmLayout.CHECKBOX_Y,
            tr("berlimatrm.gui.any_damage"),
            false);
        buttonList.add(anyDamageBox);
        checkX += anyDamageBox.width + 8;

        anyItemBox = new AtRmCheckbox(
            ID_ANY_ITEM_BOX,
            checkX,
            top + AtRmLayout.CHECKBOX_Y,
            tr("berlimatrm.gui.any_item"),
            false);
        buttonList.add(anyItemBox);

        orePrev = new ThemedButton(
            ID_ORE_PREV,
            left + AtRmLayout.ORE_PREV_X,
            top + AtRmLayout.ORE_ROW_Y,
            AtRmLayout.ORE_BUTTON_SIZE,
            AtRmLayout.ORE_BUTTON_SIZE,
            "<");
        buttonList.add(orePrev);

        oreNext = new ThemedButton(
            ID_ORE_NEXT,
            left + AtRmLayout.ORE_NEXT_X,
            top + AtRmLayout.ORE_ROW_Y,
            AtRmLayout.ORE_BUTTON_SIZE,
            AtRmLayout.ORE_BUTTON_SIZE,
            ">");
        buttonList.add(oreNext);

        resetButton = new ThemedButton(
            ID_RESET,
            left + AtRmLayout.RESET_X,
            top + AtRmLayout.ORE_ROW_Y,
            AtRmLayout.RESET_WIDTH,
            AtRmLayout.ORE_BUTTON_SIZE,
            tr("berlimatrm.gui.reset"));
        buttonList.add(resetButton);

        fromItemButton = new ThemedButton(
            ID_FROM_ITEM,
            left + AtRmLayout.FROM_ITEM_X,
            top + AtRmLayout.NBT_ROW_Y - 1,
            AtRmLayout.FROM_ITEM_WIDTH,
            AtRmLayout.ORE_BUTTON_SIZE,
            tr("berlimatrm.gui.from_item"));
        buttonList.add(fromItemButton);

        nbtField = new ThemedTextField(
            fontRendererObj,
            left + AtRmLayout.NBT_FIELD_X,
            top + AtRmLayout.NBT_ROW_Y,
            AtRmLayout.NBT_FIELD_WIDTH,
            AtRmLayout.NBT_FIELD_HEIGHT);
        nbtField.setVisible(true);
        nbtField.setFocused(false);

        nbtSlot = -2;
        applyModeToButtons();
        syncSnapshots(true);
        refreshWidgets();
    }

    /**
     * Keeps the 340x382 screen usable in a small window: if the current GUI scale would clip it,
     * use the largest scale that fits and restore the player's setting when the screen closes.
     * Re-evaluated on resize, so maximising the window while the screen is open also works.
     */
    private boolean adaptGuiScale() {
        if (switchingScale) {
            return false;
        }
        int displayWidth = org.lwjgl.opengl.Display.getWidth();
        int displayHeight = org.lwjgl.opengl.Display.getHeight();
        if (displayWidth <= 0 || displayHeight <= 0) {
            displayWidth = mc.displayWidth;
            displayHeight = mc.displayHeight;
        }

        int current = new ScaledResolution(mc, displayWidth, displayHeight).getScaleFactor();
        int preferred = previousGuiScale >= 0 ? previousGuiScale : mc.gameSettings.guiScale;
        int desired;
        if (preferred > 0) {
            desired = Math.min(preferred, 4);
        } else {
            desired = 1;
            while (desired < 4 && displayWidth / (desired + 1) >= 320 && displayHeight / (desired + 1) >= 240) {
                desired++;
            }
        }

        int fit = 1;
        for (int candidate = desired; candidate > 1; candidate--) {
            if (displayWidth / candidate >= xSize && displayHeight / candidate >= ySize) {
                fit = candidate;
                break;
            }
        }
        if (fit == current) {
            return false;
        }

        if (previousGuiScale < 0) {
            previousGuiScale = mc.gameSettings.guiScale;
        }
        switchingScale = true;
        mc.gameSettings.guiScale = fit;
        mc.resize(displayWidth, displayHeight);
        switchingScale = false;
        return true;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (previousGuiScale >= 0) {
            final int restore = previousGuiScale;
            previousGuiScale = -1;
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    Minecraft client = Minecraft.getMinecraft();
                    client.gameSettings.guiScale = restore;
                    int width = org.lwjgl.opengl.Display.getWidth();
                    int height = org.lwjgl.opengl.Display.getHeight();
                    client.resize(width > 0 ? width : client.displayWidth, height > 0 ? height : client.displayHeight);
                });
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        syncSnapshots(false);
        syncNbtField();
        refreshWidgets();
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        int star = starAt(mouseX, mouseY);
        if (button == 0 && star >= 0) {
            select(star);
            return;
        }

        boolean overNbt = mouseX >= nbtField.xPosition && mouseX < nbtField.xPosition + nbtField.width
            && mouseY >= nbtField.yPosition
            && mouseY < nbtField.yPosition + nbtField.height;
        if (overNbt) {
            nbtField.setFocused(true);
            nbtField.mouseClicked(mouseX, mouseY, button);
        } else {
            nbtField.setFocused(false);
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char letter, int key) {
        if (nbtField.isFocused() && nbtField.textboxKeyTyped(letter, key)) {
            storeNbtText();
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            sendRecipe();
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(letter, key);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int clickMode) {
        super.handleMouseClick(slot, slotId, mouseButton, clickMode);
        if (slot != null && slotId >= 0 && slotId < AtRmLayout.GRID * AtRmLayout.GRID) {
            select(slotId);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null) {
            return;
        }
        switch (button.id) {
            case ID_MODE_SHAPED:
                setMode(Mode.SHAPED);
                break;
            case ID_MODE_SHAPELESS:
                setMode(Mode.SHAPELESS);
                break;
            case ID_MODE_REMOVE:
                setMode(Mode.REMOVE);
                break;
            case ID_MIRRORED:
                mirroredBox.toggle();
                mirrored = mirroredBox.isChecked();
                break;
            case ID_CLEAR:
                ModNetwork.INSTANCE.sendToServer(new PacketAtRmClear());
                for (int i = 0; i < options.length; i++) {
                    options[i].reset();
                }
                break;
            case ID_OK:
                sendRecipe();
                break;
            case ID_CLOSE:
                mc.displayGuiScreen(null);
                break;
            case ID_ORE_PREV:
                cycleOre(-1);
                break;
            case ID_ORE_NEXT:
                cycleOre(1);
                break;
            case ID_RESET:
                resetSelectedOptions();
                break;
            case ID_FROM_ITEM:
                fillNbtFromItem();
                break;
            case ID_ORE_BOX:
                oreBox.toggle();
                applyOreBox();
                break;
            case ID_ANY_DAMAGE_BOX:
                anyDamageBox.toggle();
                applyAnyDamageBox();
                break;
            case ID_ANY_ITEM_BOX:
                anyItemBox.toggle();
                applyAnyItemBox();
                break;
            default:
                break;
        }
        refreshWidgets();
    }

    // ------------------------------------------------------------------ actions

    private void setMode(Mode newMode) {
        mode = newMode;
        applyModeToButtons();
        refreshWidgets();
    }

    private void applyModeToButtons() {
        if (modeButtons == null) {
            return;
        }
        for (int i = 0; i < modeButtons.length; i++) {
            modeButtons[i].setSelected(i == mode.ordinal());
        }
        if (mirroredBox != null) {
            mirroredBox.setChecked(mirrored);
            mirroredBox.enabled = mode == Mode.SHAPED;
        }
    }

    private void select(int slot) {
        if (slot < 0 || slot >= options.length) {
            return;
        }
        selected = slot;
        nbtSlot = -2;
        refreshWidgets();
    }

    private CellOptions selectedOptions() {
        return selected >= 0 && selected < options.length ? options[selected] : null;
    }

    private void applyOreBox() {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        if (oreBox.isChecked()) {
            List<String> names = oreNames(selected);
            if (names.isEmpty()) {
                oreBox.setChecked(false);
                showStatus(false, "berlimatrm.status.no_ore", Integer.toString(selected + 1));
                return;
            }
            options.oreDictionary = true;
            options.oreIndex = Math.max(0, Math.min(options.oreIndex, names.size() - 1));
            options.oreName = names.get(options.oreIndex);
            options.anyDamage = false;
            anyDamageBox.setChecked(false);
        } else {
            options.oreDictionary = false;
            options.oreName = null;
        }
    }

    private void applyAnyDamageBox() {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        options.anyDamage = anyDamageBox.isChecked();
    }

    private void applyAnyItemBox() {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        options.anyItem = anyItemBox.isChecked();
        if (options.anyItem) {
            options.oreDictionary = false;
            options.oreName = null;
            options.anyDamage = false;
            options.nbtText = "";
            nbtSlot = -2;
        }
    }

    private void cycleOre(int delta) {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        List<String> names = oreNames(selected);
        if (names.isEmpty()) {
            return;
        }
        options.oreIndex = (options.oreIndex + delta + names.size()) % names.size();
        if (options.oreDictionary) {
            options.oreName = names.get(options.oreIndex);
        }
    }

    private void resetSelectedOptions() {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        options.reset();
        nbtSlot = -2;
    }

    private void fillNbtFromItem() {
        CellOptions options = selectedOptions();
        if (options == null) {
            return;
        }
        ItemStack stack = container.getGhostInventory()
            .getStackInSlot(selected);
        options.nbtText = stack != null && stack.hasTagCompound() ? NbtUtil.toText(stack.getTagCompound()) : "";
        nbtSlot = -2;
    }

    private void storeNbtText() {
        CellOptions options = selectedOptions();
        if (options != null) {
            options.nbtText = nbtField.getText();
        }
    }

    private void sendRecipe() {
        ItemStack output = container.getResult();
        if (output == null) {
            showStatus(false, "berlimatrm.status.no_output", null);
            return;
        }

        if (mode == Mode.REMOVE) {
            NBTTagCompound payload = new NBTTagCompound();
            NBTTagCompound outputTag = new NBTTagCompound();
            output.writeToNBT(outputTag);
            payload.setTag("Output", outputTag);
            ModNetwork.INSTANCE.sendToServer(new PacketAtRmApply((byte) 2, payload));
            return;
        }

        AtRmRecipe recipe = buildRecipe(output);
        if (recipe == null) {
            return;
        }
        ModNetwork.INSTANCE
            .sendToServer(new PacketAtRmApply((byte) (mode == Mode.SHAPELESS ? 1 : 0), recipe.writeToNbt()));
    }

    private AtRmRecipe buildRecipe(ItemStack output) {
        AtRmIngredient[] grid = new AtRmIngredient[AtRmLayout.GRID * AtRmLayout.GRID];

        for (int slot = 0; slot < grid.length; slot++) {
            ItemStack stack = container.getGhostInventory()
                .getStackInSlot(slot);
            CellOptions options = this.options[slot];

            if (stack == null) {
                grid[slot] = AtRmIngredient.EMPTY;
                continue;
            }
            if (options.nbtText != null && !options.nbtText.trim()
                .isEmpty() && !NbtUtil.isValidSnbt(options.nbtText)) {
                showStatus(false, "berlimatrm.status.bad_nbt", Integer.toString(slot + 1));
                return null;
            }

            NBTTagCompound filter = NbtUtil.parse(options.nbtText);
            if (options.anyItem) {
                grid[slot] = AtRmIngredient.ANY_ITEM;
            } else if (options.oreDictionary && options.oreName != null) {
                grid[slot] = AtRmIngredient.ore(options.oreName, filter);
            } else {
                grid[slot] = AtRmIngredient.item(stack, options.anyDamage, filter);
            }
        }

        if (mode == Mode.SHAPELESS) {
            List<AtRmIngredient> ingredients = new ArrayList<>();
            for (AtRmIngredient ingredient : grid) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }
            AtRmRecipe recipe = AtRmRecipe.shapeless(output, ingredients);
            if (recipe == null) {
                showStatus(false, "berlimatrm.status.no_input", null);
            }
            return recipe;
        }

        AtRmRecipe recipe = AtRmRecipe.shaped(output, grid, mirrored);
        if (recipe == null) {
            showStatus(false, "berlimatrm.status.no_input", null);
        }
        return recipe;
    }

    // ------------------------------------------------------------------ state sync

    private void syncSnapshots(boolean force) {
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack current = container.getGhostInventory()
                .getStackInSlot(slot);
            boolean same = sameStack(snapshot[slot], current);
            if (!force && same) {
                continue;
            }
            if (!same) {
                options[slot].reset();
                if (slot == selected) {
                    nbtSlot = -2;
                }
            }
            snapshot[slot] = current == null ? null : current.copy();
        }
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left == null || right == null) {
            return left == right;
        }
        return ItemStack.areItemStacksEqual(left, right);
    }

    private void syncNbtField() {
        if (selected < 0) {
            if (nbtSlot != -1) {
                nbtField.setText("");
                nbtSlot = -1;
            }
            return;
        }
        if (nbtSlot != selected) {
            nbtField.setText(options[selected].nbtText == null ? "" : options[selected].nbtText);
            nbtSlot = selected;
        }
    }

    private void refreshWidgets() {
        CellOptions options = selectedOptions();
        boolean hasSelection = options != null;
        ItemStack stack = hasSelection ? container.getGhostInventory()
            .getStackInSlot(selected) : null;

        if (mirroredBox != null) {
            mirroredBox.enabled = mode == Mode.SHAPED;
        }
        if (oreBox != null) {
            oreBox.setChecked(hasSelection && options.oreDictionary);
            oreBox.enabled = hasSelection && stack != null && !options.anyItem && !oreNames(selected).isEmpty();
        }
        if (anyDamageBox != null) {
            anyDamageBox.setChecked(hasSelection && options.anyDamage);
            anyDamageBox.enabled = hasSelection && stack != null && !options.anyItem && !options.oreDictionary;
        }
        if (anyItemBox != null) {
            anyItemBox.setChecked(hasSelection && options.anyItem);
            anyItemBox.enabled = hasSelection && stack != null;
        }
        if (orePrev != null) {
            boolean oreAvailable = hasSelection && stack != null && !options.anyItem && !oreNames(selected).isEmpty();
            orePrev.enabled = oreAvailable;
            oreNext.enabled = oreAvailable;
        }
        if (resetButton != null) {
            resetButton.enabled = hasSelection;
        }
        if (fromItemButton != null) {
            fromItemButton.enabled = hasSelection && stack != null && stack.hasTagCompound();
        }
        if (nbtField != null) {
            nbtField.setEnabled(hasSelection && stack != null && !options.anyItem);
        }
        if (clearButton != null) {
            clearButton.enabled = hasGridItems();
        }
        if (okButton != null) {
            okButton.enabled = container.getResult() != null && (mode == Mode.REMOVE || hasGridItems());
        }
    }

    private boolean hasGridItems() {
        for (int slot = 0; slot < AtRmLayout.GRID * AtRmLayout.GRID; slot++) {
            if (container.getGhostInventory()
                .getStackInSlot(slot) != null) {
                return true;
            }
        }
        return false;
    }

    private List<String> oreNames(int slot) {
        if (slot < 0 || slot >= AtRmLayout.GRID * AtRmLayout.GRID) {
            return Collections.emptyList();
        }
        ItemStack stack = container.getGhostInventory()
            .getStackInSlot(slot);
        if (stack == null) {
            return Collections.emptyList();
        }
        int[] ids = OreDictionary.getOreIDs(stack);
        if (ids == null || ids.length == 0) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(ids.length);
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            if (name != null && !"Unknown".equals(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private int starAt(int mouseX, int mouseY) {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;
        for (int row = 0; row < AtRmLayout.GRID; row++) {
            for (int col = 0; col < AtRmLayout.GRID; col++) {
                int x = AtRmLayout.gridX(col) + 1;
                int y = AtRmLayout.gridY(row) + 1;
                if (relX >= x && relX < x + STAR_SIZE && relY >= y && relY < y + STAR_SIZE) {
                    return AtRmLayout.cellIndex(col, row);
                }
            }
        }
        return -1;
    }

    public void showResult(boolean ok, String key, String detail) {
        showStatus(ok, key, detail);
    }

    private void showStatus(boolean ok, String key, String detail) {
        statusOk = ok;
        statusKey = key;
        statusDetail = detail;
        statusTicks = 240;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(GuiKit.KIT);

        int x = guiLeft;
        int y = guiTop;

        drawKitPanel(x, y, AtRmLayout.WIDTH, AtRmLayout.CONTENT_HEIGHT);
        drawKitPanel(
            x + AtRmLayout.INVENTORY_X,
            y + AtRmLayout.INVENTORY_TOP,
            AtRmLayout.INVENTORY_WIDTH,
            AtRmLayout.INVENTORY_HEIGHT);

        drawRect(x + 8, y + 15, x + AtRmLayout.WIDTH - 9, y + 16, GuiTheme.FRAME_BORDER);
        drawRect(x + 8, y + 16, x + AtRmLayout.WIDTH - 9, y + 17, GuiTheme.FRAME_RULE);

        // selected-item strip
        drawRect(
            x + AtRmLayout.SELECT_X - 1,
            y + AtRmLayout.SELECT_Y - 1,
            x + AtRmLayout.WIDTH - 8,
            y + AtRmLayout.SELECT_Y + AtRmLayout.SELECT_HEIGHT + 1,
            GuiTheme.WELL);
        drawRect(
            x + AtRmLayout.SELECT_X - 1,
            y + AtRmLayout.SELECT_Y - 1,
            x + AtRmLayout.WIDTH - 8,
            y + AtRmLayout.SELECT_Y,
            GuiTheme.WELL_BORDER);
        GuiKit.CELL.drawSprite(
            zLevel,
            x + AtRmLayout.SELECT_X - 1,
            y + AtRmLayout.SELECT_Y - 1,
            AtRmLayout.SLOT,
            AtRmLayout.SLOT);

        // crafting grid
        for (int row = 0; row < AtRmLayout.GRID; row++) {
            for (int col = 0; col < AtRmLayout.GRID; col++) {
                GuiKit.CELL.drawSprite(
                    zLevel,
                    x + AtRmLayout.gridX(col) - 1,
                    y + AtRmLayout.gridY(row) - 1,
                    AtRmLayout.SLOT,
                    AtRmLayout.SLOT);
            }
        }

        // separator between grid and menu
        drawRect(
            x + AtRmLayout.MENU_X - 5,
            y + AtRmLayout.MENU_Y,
            x + AtRmLayout.MENU_X - 4,
            y + AtRmLayout.MENU_Y + AtRmLayout.MENU_HEIGHT,
            GuiTheme.FRAME_RULE);

        // result row: a recess groups the label and the output slot
        drawRect(
            x + AtRmLayout.MENU_X + 6,
            y + AtRmLayout.MENU_Y + 82,
            x + AtRmLayout.MENU_X + AtRmLayout.MENU_WIDTH - 6,
            y + AtRmLayout.MENU_Y + 102,
            GuiTheme.WELL);
        drawRect(
            x + AtRmLayout.MENU_X + 6,
            y + AtRmLayout.MENU_Y + 82,
            x + AtRmLayout.MENU_X + AtRmLayout.MENU_WIDTH - 6,
            y + AtRmLayout.MENU_Y + 83,
            GuiTheme.WELL_BORDER);
        GuiKit.CELL.drawSprite(
            zLevel,
            x + AtRmLayout.RESULT_X - 1,
            y + AtRmLayout.RESULT_Y - 1,
            AtRmLayout.SLOT,
            AtRmLayout.SLOT);

        // options panel
        drawRect(
            x + AtRmLayout.OPTIONS_X - 2,
            y + AtRmLayout.OPTIONS_Y - 2,
            x + AtRmLayout.OPTIONS_X + AtRmLayout.OPTIONS_WIDTH + 2,
            y + AtRmLayout.OPTIONS_Y + AtRmLayout.OPTIONS_HEIGHT + 2,
            GuiTheme.WELL);
        drawRect(
            x + AtRmLayout.OPTIONS_X - 2,
            y + AtRmLayout.OPTIONS_Y - 2,
            x + AtRmLayout.OPTIONS_X + AtRmLayout.OPTIONS_WIDTH + 2,
            y + AtRmLayout.OPTIONS_Y - 1,
            GuiTheme.FRAME_BORDER);

        // player inventory cells
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiKit.CELL.drawSprite(
                    zLevel,
                    x + AtRmLayout.PLAYER_SLOT_X + col * AtRmLayout.SLOT - 1,
                    y + AtRmLayout.PLAYER_SLOT_Y + row * AtRmLayout.SLOT - 1,
                    AtRmLayout.SLOT,
                    AtRmLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            GuiKit.CELL.drawSprite(
                zLevel,
                x + AtRmLayout.PLAYER_SLOT_X + col * AtRmLayout.SLOT - 1,
                y + AtRmLayout.PLAYER_HOTBAR_Y - 1,
                AtRmLayout.SLOT,
                AtRmLayout.SLOT);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int star = starAt(mouseX, mouseY);
        if (star >= 0) {
            drawHoveringText(
                Collections.singletonList(tr("berlimatrm.gui.star_tooltip", Integer.toString(star + 1))),
                mouseX,
                mouseY,
                fontRendererObj);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Text fields use absolute screen coordinates, so cancel the container's translation here.
        if (nbtField != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-guiLeft, -guiTop, 0.0F);
            nbtField.drawTextBox();
            GL11.glPopMatrix();
        }

        fontRendererObj.drawString(tr("berlimatrm.gui.title"), 8, 4, GuiTheme.TITLE);
        String subtitle = tr("berlimatrm.gui.subtitle");
        fontRendererObj.drawString(
            subtitle,
            AtRmLayout.WIDTH - 9 - fontRendererObj.getStringWidth(subtitle),
            4,
            GuiTheme.TEXT_DISABLED);

        drawSelectedStrip();
        drawMenuLabels();
        drawOptionsLabels();
        fontRendererObj.drawString(
            tr("container.inventory"),
            AtRmLayout.INVENTORY_X + 8,
            AtRmLayout.INVENTORY_TOP + 6,
            GuiTheme.TEXT);
        drawStars(mouseX, mouseY);
        drawSelectionOutline();

        if (statusTicks > 0 && statusKey != null) {
            String message = statusDetail == null ? tr(statusKey) : tr(statusKey, statusDetail);
            message = fontRendererObj.trimStringToWidth(message, AtRmLayout.OPTIONS_WIDTH - 170);
            fontRendererObj.drawString(
                message,
                AtRmLayout.OPTIONS_X + AtRmLayout.OPTIONS_WIDTH - fontRendererObj.getStringWidth(message),
                AtRmLayout.OPTIONS_Y + 3,
                statusOk ? GuiTheme.OK : GuiTheme.ERROR);
        }
    }

    private void drawSelectedStrip() {
        ItemStack stack = selected >= 0 ? container.getGhostInventory()
            .getStackInSlot(selected) : null;
        if (stack != null) {
            drawItem(stack, AtRmLayout.SELECT_X + 1, AtRmLayout.SELECT_Y + 1);
        }

        String text;
        if (selected < 0) {
            text = tr("berlimatrm.gui.select_hint");
        } else if (stack == null) {
            text = tr("berlimatrm.gui.slot_empty", Integer.toString(selected + 1));
        } else {
            text = tr("berlimatrm.gui.slot_item", Integer.toString(selected + 1), stack.getDisplayName());
            if (stack.getItemDamage() != 0) {
                text += " #" + stack.getItemDamage();
            }
        }
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(text, 190),
            AtRmLayout.SELECT_X + 22,
            AtRmLayout.SELECT_Y + 5,
            selected >= 0 ? GuiTheme.TEXT : GuiTheme.TEXT_DISABLED);

        if (selected >= 0) {
            CellOptions options = this.options[selected];
            StringBuilder flags = new StringBuilder();
            if (options.anyItem) {
                flags.append(tr("berlimatrm.gui.flag.any_item"));
            }
            if (options.oreDictionary) {
                if (flags.length() > 0) {
                    flags.append("  ");
                }
                flags.append(tr("berlimatrm.gui.flag.ore"))
                    .append(' ')
                    .append(options.oreName == null ? "?" : options.oreName);
            }
            if (options.anyDamage) {
                if (flags.length() > 0) {
                    flags.append("  ");
                }
                flags.append(tr("berlimatrm.gui.flag.any_damage"));
            }
            if (options.hasNbt()) {
                if (flags.length() > 0) {
                    flags.append("  ");
                }
                flags.append(tr("berlimatrm.gui.flag.nbt"));
            }
            if (flags.length() > 0) {
                String trimmed = fontRendererObj.trimStringToWidth(flags.toString(), 110);
                fontRendererObj.drawString(
                    trimmed,
                    AtRmLayout.WIDTH - 10 - fontRendererObj.getStringWidth(trimmed),
                    AtRmLayout.SELECT_Y + 5,
                    GuiTheme.ACCENT);
            }
        }
    }

    private void drawMenuLabels() {
        fontRendererObj.drawString(
            tr("berlimatrm.gui.recipe"),
            AtRmLayout.MENU_X + 6,
            AtRmLayout.MENU_Y + 4,
            GuiTheme.TEXT_DISABLED);
        fontRendererObj.drawString(
            tr(mode == Mode.REMOVE ? "berlimatrm.gui.remove_hint" : "berlimatrm.gui.result"),
            AtRmLayout.MENU_X + 8,
            AtRmLayout.MENU_Y + 88,
            GuiTheme.TEXT_DISABLED);
    }

    private void drawOptionsLabels() {
        String optionsLabel = selected >= 0 ? tr("berlimatrm.gui.options", Integer.toString(selected + 1))
            : tr("berlimatrm.gui.options_none");
        fontRendererObj.drawString(optionsLabel, AtRmLayout.OPTIONS_INNER_X, AtRmLayout.OPTIONS_Y + 3, GuiTheme.TEXT);

        // recessed name field between the arrows, so the ore control reads as one widget
        drawRect(
            AtRmLayout.ORE_NAME_X - 2,
            AtRmLayout.ORE_ROW_Y - 1,
            AtRmLayout.ORE_NAME_X + AtRmLayout.ORE_NAME_WIDTH + 2,
            AtRmLayout.ORE_ROW_Y + 15,
            GuiTheme.WELL);
        drawRect(
            AtRmLayout.ORE_NAME_X - 2,
            AtRmLayout.ORE_ROW_Y - 1,
            AtRmLayout.ORE_NAME_X + AtRmLayout.ORE_NAME_WIDTH + 2,
            AtRmLayout.ORE_ROW_Y,
            GuiTheme.WELL_BORDER);
        fontRendererObj.drawString(
            tr("berlimatrm.gui.ore_label"),
            AtRmLayout.ORE_LABEL_X,
            AtRmLayout.ORE_ROW_Y + 4,
            GuiTheme.TEXT_DISABLED);
        String oreName = tr("berlimatrm.gui.ore_none");
        if (selected >= 0) {
            CellOptions options = this.options[selected];
            if (options.oreDictionary && options.oreName != null) {
                oreName = options.oreName;
            }
        }
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(oreName, AtRmLayout.ORE_NAME_WIDTH),
            AtRmLayout.ORE_NAME_X,
            AtRmLayout.ORE_ROW_Y + 4,
            GuiTheme.ACCENT);

        fontRendererObj.drawString(
            tr("berlimatrm.gui.nbt_label"),
            AtRmLayout.NBT_LABEL_X,
            AtRmLayout.NBT_ROW_Y + 2,
            GuiTheme.TEXT_DISABLED);
        if (nbtField != null && nbtField.getText()
            .isEmpty() && selected >= 0) {
            fontRendererObj.drawString(
                tr("berlimatrm.gui.nbt_hint"),
                AtRmLayout.NBT_FIELD_X + 3,
                AtRmLayout.NBT_ROW_Y + 2,
                GuiTheme.TEXT_DISABLED);
        }
    }

    private void drawStars(int mouseX, int mouseY) {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;
        for (int row = 0; row < AtRmLayout.GRID; row++) {
            for (int col = 0; col < AtRmLayout.GRID; col++) {
                int index = AtRmLayout.cellIndex(col, row);
                int x = AtRmLayout.gridX(col) + 1;
                int y = AtRmLayout.gridY(row) + 1;

                boolean hovered = relX >= x && relX < x + STAR_SIZE && relY >= y && relY < y + STAR_SIZE;
                // The selector is deliberately hidden until the cell is hovered: an always-on
                // badge in every corner reads as a texture artefact, not as a button.
                if (!hovered && index != selected) {
                    continue;
                }

                int border = hovered ? GuiTheme.ACCENT : GuiTheme.FRAME_LIGHT;
                int fill = hovered ? 0xE61B2730 : 0xE6101419;
                drawRect(x, y, x + STAR_SIZE, y + STAR_SIZE, fill);
                drawRect(x, y, x + STAR_SIZE, y + 1, border);
                drawRect(x, y + STAR_SIZE - 1, x + STAR_SIZE, y + STAR_SIZE, border);
                drawRect(x, y, x + 1, y + STAR_SIZE, border);
                drawRect(x + STAR_SIZE - 1, y, x + STAR_SIZE, y + STAR_SIZE, border);

                drawCenteredString(fontRendererObj, "*", x + STAR_SIZE / 2, y, hovered ? 0xFFFFFFFF : GuiTheme.ACCENT);
            }
        }
    }

    private void drawSelectionOutline() {
        if (selected < 0) {
            return;
        }
        int col = selected % AtRmLayout.GRID;
        int row = selected / AtRmLayout.GRID;
        int x = AtRmLayout.gridX(col);
        int y = AtRmLayout.gridY(row);
        int color = GuiTheme.ACCENT;
        drawRect(x - 1, y - 1, x + AtRmLayout.SLOT + 1, y, color);
        drawRect(x - 1, y + AtRmLayout.SLOT, x + AtRmLayout.SLOT + 1, y + AtRmLayout.SLOT + 1, color);
        drawRect(x - 1, y - 1, x, y + AtRmLayout.SLOT + 1, color);
        drawRect(x + AtRmLayout.SLOT, y - 1, x + AtRmLayout.SLOT + 1, y + AtRmLayout.SLOT + 1, color);
    }

    private void drawItem(ItemStack stack, int x, int y) {
        if (stack == null) {
            return;
        }
        RenderItem renderItem = RenderItem.getInstance();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        renderItem.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        renderItem.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ helpers

    private void drawKitPanel(int x, int y, int width, int height) {
        drawGradientRect(x, y, x + width, y + height, GuiTheme.PANEL_TOP, GuiTheme.PANEL_BOTTOM);
        GuiKit.PANEL.draw(zLevel, x, y, width, height);
    }

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }
}
