package ru.beggar.berlimatrm.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * NBT helpers for recipe ingredients.
 *
 * <p>
 * Ingredient tags match by <em>containment</em>: every key present in the recipe tag must be
 * present in the actual stack tag. That way a recipe can match an item that carries extra
 * bookkeeping tags (repair cost, custom model data, ...). Numeric tags of different widths
 * compare by value.
 */
public final class NbtUtil {

    private NbtUtil() {}

    /** True when {@code actual} contains every key/value of {@code expected}. */
    public static boolean contains(NBTTagCompound expected, NBTTagCompound actual) {
        if (expected == null || expected.hasNoTags()) {
            return true;
        }
        if (actual == null) {
            return false;
        }

        Set<String> keys = expected.func_150296_c();
        for (String key : keys) {
            if (!actual.hasKey(key)) {
                return false;
            }
            if (!tagContains(expected.getTag(key), actual.getTag(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean tagContains(NBTBase expected, NBTBase actual) {
        if (expected == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }

        if (expected instanceof NBTBase.NBTPrimitive && actual instanceof NBTBase.NBTPrimitive) {
            return ((NBTBase.NBTPrimitive) expected).func_150286_g() == ((NBTBase.NBTPrimitive) actual).func_150286_g();
        }

        int type = expected.getId();
        if (type != actual.getId()) {
            return false;
        }

        if (type == 10) {
            NBTTagCompound left = (NBTTagCompound) expected;
            NBTTagCompound right = (NBTTagCompound) actual;
            for (String key : left.func_150296_c()) {
                if (!right.hasKey(key)) {
                    return false;
                }
                if (!tagContains(left.getTag(key), right.getTag(key))) {
                    return false;
                }
            }
            return true;
        }

        // Strings, lists and primitive arrays: exact value equality is what an ingredient filter
        // wants. NBTBase.equals is overridden by each of those classes.
        return expected.equals(actual);
    }

    /** Canonical, order-independent text form - used for duplicate detection. */
    public static String canonical(NBTBase tag) {
        if (tag == null) {
            return "";
        }

        if (tag instanceof NBTBase.NBTPrimitive) {
            return "n:" + ((NBTBase.NBTPrimitive) tag).func_150286_g();
        }

        int type = tag.getId();
        switch (type) {
            case 7:
                return "b:" + Arrays.toString(((NBTTagByteArray) tag).func_150292_c());
            case 8:
                return "s:" + ((NBTTagString) tag).func_150285_a_();
            case 9: {
                NBTTagList list = (NBTTagList) tag;
                StringBuilder builder = new StringBuilder("l[");
                for (int i = 0; i < list.tagCount(); i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    builder.append(element(list, i));
                }
                return builder.append(']')
                    .toString();
            }
            case 10: {
                NBTTagCompound compound = (NBTTagCompound) tag;
                List<String> keys = new ArrayList<>(compound.func_150296_c());
                Collections.sort(keys);
                StringBuilder builder = new StringBuilder("c{");
                for (String key : keys) {
                    builder.append(key)
                        .append('=')
                        .append(canonical(compound.getTag(key)))
                        .append(';');
                }
                return builder.append('}')
                    .toString();
            }
            case 11:
                return "i:" + Arrays.toString(((NBTTagIntArray) tag).func_150302_c());
            default:
                return tag.toString();
        }
    }

    /** Best-effort element accessor for canonical strings. */
    private static String element(NBTTagList list, int index) {
        switch (list.func_150303_d()) {
            case 10:
                return canonical(list.getCompoundTagAt(index));
            case 8:
                return "s:" + list.getStringTagAt(index);
            case 6:
                return "n:" + list.func_150309_d(index);
            case 5:
                return "n:" + list.func_150308_e(index);
            case 11:
                return "i:" + Arrays.toString(list.func_150306_c(index));
            default:
                return list.toString();
        }
    }

    /** Text shown in the GUI's NBT field. */
    public static String toText(NBTTagCompound tag) {
        return tag == null ? "" : tag.toString();
    }

    /** Parses the GUI's NBT field; returns null for empty or invalid input. */
    public static NBTTagCompound parse(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            NBTBase parsed = JsonToNBT.func_150315_a(trimmed);
            return parsed instanceof NBTTagCompound ? (NBTTagCompound) parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isValidSnbt(String text) {
        return text == null || text.trim()
            .isEmpty() || parse(text) != null;
    }
}
