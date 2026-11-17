package ru.beggar.berlimatrm.command;

import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import ru.beggar.berlimatrm.BerlitAtRm;
import ru.beggar.berlimatrm.gui.AtRmGuiHandler;
import ru.beggar.berlimatrm.recipe.AtRmRecipes;

/** {@code /atrm} opens the recipe maker; {@code /atrm export|reload} manage the saved file. */
public class CommandAtRm extends CommandBase {

    @Override
    public String getCommandName() {
        return "atrm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "berlimatrm.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return ru.beggar.berlimatrm.Config.requireOp ? 2 : 0;
    }

    /**
     * 1.7.10's {@code EntityPlayerMP.canCommandSenderUseCommand} ignores the numeric level and only
     * checks the op list, so the config switch has to bypass the whole check explicitly.
     */
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return !ru.beggar.berlimatrm.Config.requireOp || super.canCommandSenderUseCommand(sender);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            player.openGui(BerlitAtRm.instance, AtRmGuiHandler.GUI_ID, player.worldObj, 0, 0, 0);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("export".equals(sub)) {
            AtRmRecipes.exportScript();
            sender.addChatMessage(
                new ChatComponentTranslation(
                    "berlimatrm.command.exported",
                    AtRmRecipes.size(),
                    AtRmRecipes.getExportFile() == null ? "?"
                        : AtRmRecipes.getExportFile()
                            .getAbsolutePath()));
            return;
        }
        if ("reload".equals(sub)) {
            AtRmRecipes.reload();
            sender.addChatMessage(new ChatComponentTranslation("berlimatrm.command.reloaded", AtRmRecipes.size()));
            return;
        }
        throw new WrongUsageException("berlimatrm.command.usage");
    }
}
