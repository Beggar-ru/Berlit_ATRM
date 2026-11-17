package ru.beggar.berlimatrm.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

/**
 * One cell of a recipe: empty, an exact item (optionally with any damage and/or an NBT
 * filter), an ore dictionary entry, or the "any item" wildcard.
 */
public final class AtRmIngredient {

    public static final AtRmIngredient EMPTY = new AtRmIngredient(null, null, false, false, null);
    public static final AtRmIngredient ANY_ITEM = new AtRmIngredient(null, null, true, false, null);

    private final ItemStack stack;
    private final String oreName;
    private final boolean anyItem;
    private final boolean anyDamage;
    private final NBTTagCompound filter;

    private AtRmIngredient(ItemStack stack, String oreName, boolean anyItem, boolean anyDamage, NBTTagCompound filter) {
        this.stack = stack;
        this.oreName = oreName;
        this.anyItem = anyItem;
        this.anyDamage = anyDamage;
        this.filter = filter;
    }

    public static AtRmIngredient item(ItemStack stack, boolean anyDamage, NBTTagCompound nbt) {
        if (stack == null) {
            return EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        // The stack identifies the item (id + damage); matching NBT is a separate, optional filter.
        copy.setTagCompound(null);
        return new AtRmIngredient(copy, null, false, anyDamage, nbt);
    }

    public static AtRmIngredient ore(String oreName, NBTTagCompound nbt) {
        if (oreName == null || oreName.trim()
            .isEmpty()) {
            return EMPTY;
        }
        return new AtRmIngredient(null, oreName, false, false, nbt);
    }

    public static AtRmIngredient anyItem() {
        return ANY_ITEM;
    }

    public static AtRmIngredient readFromNbt(NBTTagCompound tag) {
        if (tag == null) {
            return EMPTY;
        }
        if (tag.getBoolean("AnyItem")) {
            return ANY_ITEM;
        }
        NBTTagCompound filter = tag.hasKey("Filter") ? tag.getCompoundTag("Filter") : null;
        String ore = tag.hasKey("Ore") ? tag.getString("Ore") : null;
        ItemStack stack = tag.hasKey("Stack") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Stack")) : null;
        boolean anyDamage = tag.getBoolean("AnyDamage");
        if (stack != null) {
            stack.stackSize = 1;
        }
        if (ore != null) {
            return new AtRmIngredient(null, ore, false, false, filter);
        }
        if (stack == null) {
            return EMPTY;
        }
        return new AtRmIngredient(stack, null, false, anyDamage, filter);
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        if (anyItem) {
            tag.setBoolean("AnyItem", true);
            return tag;
        }
        if (oreName != null) {
            tag.setString("Ore", oreName);
            if (filter != null) {
                tag.setTag("Filter", filter);
            }
            return tag;
        }
        if (stack == null) {
            return tag;
        }
        NBTTagCompound stackTag = new NBTTagCompound();
        stack.writeToNBT(stackTag);
        tag.setTag("Stack", stackTag);
        tag.setBoolean("AnyDamage", anyDamage);
        if (filter != null) {
            tag.setTag("Filter", filter);
        }
        return tag;
    }

    public boolean isEmpty() {
        return stack == null && oreName == null && !anyItem;
    }

    public boolean isAnyItem() {
        return anyItem;
    }

    public String getOreName() {
        return oreName;
    }

    public boolean isAnyDamage() {
        return anyDamage;
    }

    public ItemStack getStack() {
        return stack;
    }

    /** NBT filter attached to this ingredient, if any. */
    public NBTTagCompound getFilter() {
        return filter;
    }

    public boolean matches(ItemStack candidate) {
        if (anyItem) {
            return candidate != null;
        }
        if (oreName != null) {
            if (candidate == null) {
                return false;
            }
            List<ItemStack> ores = OreDictionary.getOres(oreName);
            for (ItemStack ore : ores) {
                if (ore == null || ore.getItem() == null || ore.getItem() != candidate.getItem()) {
                    continue;
                }
                int oreDamage = ore.getItemDamage();
                if (oreDamage != OreDictionary.WILDCARD_VALUE && oreDamage != candidate.getItemDamage()) {
                    continue;
                }
                if (NbtUtil.contains(filter, candidate.getTagCompound())) {
                    return true;
                }
            }
            return false;
        }
        if (stack == null) {
            return candidate == null;
        }
        if (candidate == null || candidate.getItem() != stack.getItem()) {
            return false;
        }
        if (!anyDamage && stack.getItemDamage() != OreDictionary.WILDCARD_VALUE
            && candidate.getItemDamage() != OreDictionary.WILDCARD_VALUE
            && stack.getItemDamage() != candidate.getItemDamage()) {
            return false;
        }
        return NbtUtil.contains(filter, candidate.getTagCompound());
    }

    /** Stable, human-readable identity used for duplicate detection. */
    public String signature() {
        if (anyItem) {
            return "*";
        }
        if (oreName != null) {
            return "ore:" + oreName + (filter != null ? "#" + NbtUtil.canonical(filter) : "");
        }
        if (stack == null) {
            return "-";
        }
        String name = RecipeTokens.itemName(stack);
        return name + (anyDamage ? ":*" : "") + (filter != null ? "#" + NbtUtil.canonical(filter) : "");
    }
}
