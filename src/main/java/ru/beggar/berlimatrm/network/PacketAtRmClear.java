package ru.beggar.berlimatrm.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.beggar.berlimatrm.Config;
import ru.beggar.berlimatrm.gui.ContainerAtRm;

/** Client -> server: clear the ghost crafting grid. */
public class PacketAtRmClear implements IMessage {

    public PacketAtRmClear() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketAtRmClear, IMessage> {

        @Override
        public IMessage onMessage(PacketAtRmClear message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            ServerTaskQueue.schedule(() -> {
                if (Config.requireOp && !player.canCommandSenderUseCommand(2, "atrm")) {
                    return;
                }
                if (player.openContainer instanceof ContainerAtRm) {
                    ((ContainerAtRm) player.openContainer).clearGrid();
                }
            });
            return null;
        }
    }
}
