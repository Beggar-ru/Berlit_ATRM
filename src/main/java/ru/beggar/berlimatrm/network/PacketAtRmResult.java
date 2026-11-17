package ru.beggar.berlimatrm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import ru.beggar.berlimatrm.gui.GuiAtRm;

/** Server -> client: result of a GUI edit. */
public class PacketAtRmResult implements IMessage {

    private boolean success;
    private String key;
    private String detail;

    public PacketAtRmResult() {}

    public PacketAtRmResult(boolean success, String key, String detail) {
        this.success = success;
        this.key = key;
        this.detail = detail;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        success = buf.readBoolean();
        key = ByteBufUtils.readUTF8String(buf);
        detail = buf.readBoolean() ? ByteBufUtils.readUTF8String(buf) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(success);
        ByteBufUtils.writeUTF8String(buf, key);
        buf.writeBoolean(detail != null);
        if (detail != null) {
            ByteBufUtils.writeUTF8String(buf, detail);
        }
    }

    public static class Handler implements IMessageHandler<PacketAtRmResult, IMessage> {

        @Override
        public IMessage onMessage(final PacketAtRmResult message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.func_152344_a(() -> {
                if (mc.currentScreen instanceof GuiAtRm) {
                    ((GuiAtRm) mc.currentScreen).showResult(message.success, message.key, message.detail);
                } else if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(
                        message.detail == null ? new ChatComponentTranslation(message.key)
                            : new ChatComponentTranslation(message.key, message.detail));
                }
            });
            return null;
        }
    }
}
