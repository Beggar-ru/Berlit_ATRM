package ru.beggar.berlimatrm.gui;

/** Per-cell options edited in the bottom panel; client-side until OK is pressed. */
public final class CellOptions {

    public boolean oreDictionary;
    public boolean anyDamage;
    public boolean anyItem;
    public String oreName;
    public String nbtText = "";
    public int oreIndex;

    public void reset() {
        oreDictionary = false;
        anyDamage = false;
        anyItem = false;
        oreName = null;
        nbtText = "";
        oreIndex = 0;
    }

    public boolean hasNbt() {
        return !nbtText.trim()
            .isEmpty();
    }
}
