package ru.beggar.berlimatrm.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** A recipe as edited in the GUI: either a shaped 9x9 layout or a shapeless list. */
public final class AtRmRecipe {

    public static final int GRID = 9;
    public static final int CELLS = GRID * GRID;

    public enum Kind {
        SHAPED,
        SHAPELESS
    }

    private final ItemStack output;
    private final Kind kind;
    private final boolean mirrored;
    private final AtRmIngredient[] grid;
    private final AtRmIngredient[] items;
    private final int minRow;
    private final int minCol;
    private final int width;
    private final int height;
    private final AtRmIngredient[] cells;

    private AtRmRecipe(ItemStack output, Kind kind, boolean mirrored, AtRmIngredient[] grid, AtRmIngredient[] items) {
        this.output = output.copy();
        this.kind = kind;
        this.mirrored = mirrored;
        this.grid = grid;

        if (kind == Kind.SHAPED) {
            int minR = GRID;
            int minC = GRID;
            int maxR = -1;
            int maxC = -1;
            for (int row = 0; row < GRID; row++) {
                for (int col = 0; col < GRID; col++) {
                    if (grid[col + row * GRID] != null && !grid[col + row * GRID].isEmpty()) {
                        minR = Math.min(minR, row);
                        minC = Math.min(minC, col);
                        maxR = Math.max(maxR, row);
                        maxC = Math.max(maxC, col);
                    }
                }
            }
            this.minRow = minR == GRID ? 0 : minR;
            this.minCol = minC == GRID ? 0 : minC;
            this.width = maxC < 0 ? 0 : maxC - minC + 1;
            this.height = maxR < 0 ? 0 : maxR - minR + 1;
            this.cells = new AtRmIngredient[Math.max(0, width * height)];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    cells[col + row * width] = grid[(minCol + col) + (minRow + row) * GRID];
                }
            }
            this.items = null;
        } else {
            this.minRow = 0;
            this.minCol = 0;
            this.width = 0;
            this.height = 0;
            this.cells = null;
            this.items = items;
        }
    }

    /** @return null when the layout is empty or malformed. */
    public static AtRmRecipe shaped(ItemStack output, AtRmIngredient[] grid, boolean mirrored) {
        if (output == null || grid == null || grid.length != CELLS) {
            return null;
        }
        AtRmRecipe recipe = new AtRmRecipe(output, Kind.SHAPED, mirrored, grid.clone(), null);
        return recipe.cells.length == 0 ? null : recipe;
    }

    /** @return null when the list is empty or contains only empty ingredients. */
    public static AtRmRecipe shapeless(ItemStack output, List<AtRmIngredient> ingredients) {
        if (output == null || ingredients == null) {
            return null;
        }
        List<AtRmIngredient> list = new ArrayList<>();
        for (AtRmIngredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                list.add(ingredient);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        return new AtRmRecipe(output, Kind.SHAPELESS, false, null, list.toArray(new AtRmIngredient[0]));
    }

    public static AtRmRecipe fromNbt(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("Output")) {
            return null;
        }
        ItemStack output = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Output"));
        if (output == null) {
            return null;
        }
        if (output.stackSize < 1) {
            output.stackSize = 1;
        } else if (output.stackSize > 64) {
            output.stackSize = 64;
        }
        boolean shaped = tag.getBoolean("Shaped");
        boolean mirrored = tag.getBoolean("Mirrored");

        if (shaped) {
            AtRmIngredient[] grid = new AtRmIngredient[CELLS];
            NBTTagList list = tag.getTagList("Grid", 10);
            for (int i = 0; i < CELLS; i++) {
                grid[i] = i < list.tagCount() ? AtRmIngredient.readFromNbt(list.getCompoundTagAt(i))
                    : AtRmIngredient.EMPTY;
            }
            return shaped(output, grid, mirrored);
        }

        List<AtRmIngredient> items = new ArrayList<>();
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            items.add(AtRmIngredient.readFromNbt(list.getCompoundTagAt(i)));
        }
        return shapeless(output, items);
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound outputTag = new NBTTagCompound();
        output.writeToNBT(outputTag);
        tag.setTag("Output", outputTag);
        tag.setBoolean("Shaped", kind == Kind.SHAPED);
        tag.setBoolean("Mirrored", mirrored);

        if (kind == Kind.SHAPED) {
            NBTTagList list = new NBTTagList();
            for (AtRmIngredient ingredient : grid) {
                list.appendTag(ingredient.writeToNbt());
            }
            tag.setTag("Grid", list);
        } else {
            NBTTagList list = new NBTTagList();
            for (AtRmIngredient ingredient : items) {
                list.appendTag(ingredient.writeToNbt());
            }
            tag.setTag("Items", list);
        }
        return tag;
    }

    public ItemStack getOutput() {
        return output;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isShaped() {
        return kind == Kind.SHAPED;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public AtRmIngredient[] getGrid() {
        return grid;
    }

    public AtRmIngredient[] getItems() {
        return items;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public AtRmIngredient getCell(int col, int row) {
        if (cells == null || col < 0 || row < 0 || col >= width || row >= height) {
            return AtRmIngredient.EMPTY;
        }
        return cells[col + row * width] == null ? AtRmIngredient.EMPTY : cells[col + row * width];
    }

    public net.minecraft.item.crafting.IRecipe createRecipe() {
        if (kind == Kind.SHAPED) {
            return new AtRmShapedRecipe(output, width, height, cells, mirrored);
        }
        return new AtRmShapelessRecipe(output, items);
    }

    /** Identity used to reject an exact duplicate of a recipe that is already registered. */
    public String signature() {
        StringBuilder builder = new StringBuilder();
        builder.append(RecipeTokens.itemName(output))
            .append(':')
            .append(output.getItemDamage())
            .append('#')
            .append(NbtUtil.canonical(output.getTagCompound()))
            .append('|')
            .append(kind)
            .append('|')
            .append(mirrored)
            .append('|');

        if (kind == Kind.SHAPED) {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    builder.append(getCell(col, row).signature())
                        .append(',');
                }
            }
        } else {
            List<String> signatures = new ArrayList<>();
            for (AtRmIngredient ingredient : items) {
                signatures.add(ingredient.signature());
            }
            Collections.sort(signatures);
            for (String signature : signatures) {
                builder.append(signature)
                    .append(',');
            }
        }
        return builder.toString();
    }

    /** MineTweaker statements for the .zs export; empty when the recipe cannot be represented. */
    public List<String> scriptLines() {
        List<String> lines = new ArrayList<>();
        String outputToken = RecipeTokens.outputToken(output);
        if (outputToken == null) {
            return lines;
        }

        if (kind == Kind.SHAPELESS) {
            StringBuilder builder = new StringBuilder("mods.berlimatrm.Avaritia.addShapeless(").append(outputToken)
                .append(", [");
            for (int i = 0; i < items.length; i++) {
                String token = RecipeTokens.ingredientToken(items[i]);
                if (token == null) {
                    return lines;
                }
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(token);
            }
            builder.append("]);");
            lines.add(builder.toString());
            return lines;
        }

        StringBuilder builder = new StringBuilder("mods.berlimatrm.Avaritia.addShaped(").append(outputToken)
            .append(", [");
        for (int row = 0; row < height; row++) {
            if (row > 0) {
                builder.append(", ");
            }
            builder.append('[');
            for (int col = 0; col < width; col++) {
                String token = RecipeTokens.ingredientToken(getCell(col, row));
                if (token == null) {
                    return lines;
                }
                if (col > 0) {
                    builder.append(", ");
                }
                builder.append(token);
            }
            builder.append(']');
        }
        builder.append("], ")
            .append(mirrored)
            .append(");");
        lines.add(builder.toString());
        return lines;
    }
}
