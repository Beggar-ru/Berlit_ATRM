package ru.beggar.berlimatrm.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameData;

/** Textual forms: item ids, ingredients and MineTweaker tokens for the script export. */
public final class RecipeTokens {

    private RecipeTokens() {}

    public static String itemName(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "unknown";
        }
        Object name = GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
        if (name != null) {
            return name.toString();
        }
        return stack.getItem()
            .getUnlocalizedName()
            .replace("item.", "")
            .replace("tile.", "");
    }

    public static String outputToken(ItemStack stack) {
        String base = withTag(baseToken(stack), stack == null ? null : stack.getTagCompound());
        if (base == null) {
            return null;
        }
        return stack.stackSize > 1 ? base + " * " + stack.stackSize : base;
    }

    /** Token without an amount - used by {@code remove()}, where stack size is irrelevant. */
    public static String removalToken(ItemStack stack) {
        return withTag(baseToken(stack), stack == null ? null : stack.getTagCompound());
    }

    public static String ingredientToken(AtRmIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return "null";
        }
        if (ingredient.isAnyItem()) {
            return "<*>";
        }
        if (ingredient.getOreName() != null) {
            String base = "<ore:" + ingredient.getOreName() + ">";
            return withTag(base, ingredient.getFilter());
        }
        ItemStack stack = ingredient.getStack();
        String base = baseToken(stack);
        if (base == null) {
            return null;
        }
        String token = withTag(base, ingredient.getFilter());
        if (token == null) {
            return null;
        }
        if (ingredient.isAnyDamage() && stack.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
            token += ".anyDamage()";
        }
        return token;
    }

    private static String baseToken(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder("<").append(itemName(stack));
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            builder.append(":*");
        } else if (stack.getItemDamage() != 0) {
            builder.append(':')
                .append(stack.getItemDamage());
        }
        return builder.append('>')
            .toString();
    }

    private static String withTag(String token, NBTTagCompound tag) {
        if (token == null) {
            return null;
        }
        if (tag == null || tag.hasNoTags()) {
            return token;
        }
        String zen = zenTag(tag);
        return zen == null ? null : token + ".withTag(" + zen + ")";
    }

    /**
     * Converts an NBT compound to MineTweaker's map-literal form.
     *
     * @return null when the tag cannot be represented (byte arrays, non-identifier keys, ...);
     *         the recipe is then still applied from the config file, only the .zs export skips it.
     */
    public static String zenTag(NBTTagCompound tag) {
        return zenValue(tag);
    }

    private static String zenValue(NBTBase base) {
        if (base == null) {
            return "null";
        }
        if (base instanceof NBTBase.NBTPrimitive) {
            int type = base.getId();
            long asLong = ((NBTBase.NBTPrimitive) base).func_150291_c();
            double asDouble = ((NBTBase.NBTPrimitive) base).func_150286_g();
            if (type == 1 || type == 2 || type == 3 || type == 4) {
                if (asLong < Integer.MIN_VALUE || asLong > Integer.MAX_VALUE) {
                    return null;
                }
                return Long.toString(asLong);
            }
            String text = Double.toString(asDouble);
            return text.contains(".") ? text : text + ".0";
        }

        switch (base.getId()) {
            case 8:
                return quote(((NBTTagString) base).func_150285_a_());
            case 9: {
                NBTTagList list = (NBTTagList) base;
                StringBuilder builder = new StringBuilder("[");
                for (int i = 0; i < list.tagCount(); i++) {
                    String value = zenElement(list, i);
                    if (value == null) {
                        return null;
                    }
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(value);
                }
                return builder.append(']')
                    .toString();
            }
            case 10: {
                NBTTagCompound compound = (NBTTagCompound) base;
                StringBuilder builder = new StringBuilder("{");
                boolean first = true;
                for (String key : compound.func_150296_c()) {
                    if (!isIdentifier(key)) {
                        return null;
                    }
                    String value = zenValue(compound.getTag(key));
                    if (value == null) {
                        return null;
                    }
                    if (!first) {
                        builder.append(", ");
                    }
                    first = false;
                    builder.append(key)
                        .append(": ")
                        .append(value);
                }
                return builder.append('}')
                    .toString();
            }
            default:
                return null;
        }
    }

    private static String zenElement(NBTTagList list, int index) {
        switch (list.func_150303_d()) {
            case 10:
                return zenValue(list.getCompoundTagAt(index));
            case 8:
                return quote(list.getStringTagAt(index));
            case 6: {
                String text = Double.toString(list.func_150309_d(index));
                return text.contains(".") ? text : text + ".0";
            }
            case 5: {
                String text = Float.toString(list.func_150308_e(index));
                return text.contains(".") ? text : text + ".0";
            }
            default:
                return null;
        }
    }

    private static boolean isIdentifier(String key) {
        if (key.isEmpty() || !Character.isJavaIdentifierStart(key.charAt(0))) {
            return false;
        }
        for (int i = 1; i < key.length(); i++) {
            if (!Character.isJavaIdentifierPart(key.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String quote(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.append('"')
            .toString();
    }
}
