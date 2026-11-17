package ru.beggar.berlimatrm;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Forge config: who may edit recipes, how many, and whether the .zs export is written. */
public final class Config {

    public static boolean requireOp = true;
    public static int maxRecipes = 512;
    public static boolean exportScript = true;

    private Config() {}

    public static void synchronize(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();

        requireOp = configuration.getBoolean(
            "requireOp",
            "general",
            true,
            "Only operators (permission level 2) may add or remove recipes. Disable for private packs.");
        maxRecipes = configuration
            .getInt("maxRecipes", "general", 512, 1, 8192, "Upper bound on recipes stored by the GUI.");
        exportScript = configuration.getBoolean(
            "exportScript",
            "general",
            true,
            "Write a human-readable MineTweaker export next to the recipe data on every change.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
