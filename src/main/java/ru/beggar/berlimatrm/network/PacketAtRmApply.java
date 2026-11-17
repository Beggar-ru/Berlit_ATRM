package ru.beggar.berlimatrm.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.beggar.berlimatrm.Config;
import ru.beggar.berlimatrm.recipe.AtRmRecipe;
import ru.beggar.berlimatrm.recipe.AtRmRecipes;

/** Client -> server: add a recipe, or remove every recipe for the result item. */
public class PacketAtRmApply implements IMessage {

    private byte mode;
    private NBTTagCompound payload;

    public PacketAtRmApply() {}

    public PacketAtRmApply(byte mode, NBTTagCompound payload) {
        this.mode = mode;
        this.payload = payload;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readByte();
        payload = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        ByteBufUtils.writeTag(buf, payload);
    }

    public static class Handler implements IMessageHandler<PacketAtRmApply, IMessage> {

        @Override
        public IMessage onMessage(PacketAtRmApply message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            ServerTaskQueue.schedule(() -> apply(player, message.mode, message.payload));
            return null;
        }
    }

    private static void apply(EntityPlayerMP player, byte mode, NBTTagCompound payload) {
        if (Config.requireOp && !player.canCommandSenderUseCommand(2, "atrm")) {
            send(player, false, "berlimatrm.status.no_permission", null);
            return;
        }

        AtRmRecipes.Result result;
        if (mode == 2) {
            ItemStack output = payload != null && payload.hasKey("Output")
                ? ItemStack.loadItemStackFromNBT(payload.getCompoundTag("Output"))
                : null;
            result = AtRmRecipes.remove(output);
        } else {
            AtRmRecipe recipe = payload == null ? null : AtRmRecipe.fromNbt(payload);
            result = recipe == null ? AtRmRecipes.Result.error(AtRmRecipes.Status.INVALID) : AtRmRecipes.add(recipe);
        }

        switch (result.status) {
            case OK:
                send(
                    player,
                    true,
                    mode == 2 ? "berlimatrm.status.removed" : "berlimatrm.status.added",
                    Integer.toString(result.count));
                break;
            case DUPLICATE:
                send(player, false, "berlimatrm.status.duplicate", null);
                break;
            case NO_OUTPUT:
                send(player, false, "berlimatrm.status.no_output", null);
                break;
            case NO_INPUT:
                send(player, false, "berlimatrm.status.no_input", null);
                break;
            case NO_MATCH:
                send(player, false, "berlimatrm.status.no_match", null);
                break;
            case LIMIT:
                send(player, false, "berlimatrm.status.limit", null);
                break;
            default:
                send(player, false, "berlimatrm.status.invalid", result.detail);
                break;
        }
    }

    private static void send(EntityPlayerMP player, boolean ok, String key, String detail) {
        ModNetwork.INSTANCE.sendTo(new PacketAtRmResult(ok, key, detail), player);
    }
}
