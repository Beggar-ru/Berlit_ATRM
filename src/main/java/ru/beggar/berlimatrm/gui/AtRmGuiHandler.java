package ru.beggar.berlimatrm.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;

/** Creates the recipe maker container on the server and the screen on the client. */
public final class AtRmGuiHandler implements IGuiHandler {

    public static final int GUI_ID = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == GUI_ID ? new ContainerAtRm(player.inventory) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == GUI_ID ? new GuiAtRm(new ContainerAtRm(player.inventory)) : null;
    }
}
