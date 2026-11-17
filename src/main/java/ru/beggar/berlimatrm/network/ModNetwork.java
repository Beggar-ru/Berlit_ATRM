package ru.beggar.berlimatrm.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("berlimatrm");

    private ModNetwork() {}

    public static void init() {
        INSTANCE.registerMessage(PacketAtRmApply.Handler.class, PacketAtRmApply.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PacketAtRmResult.Handler.class, PacketAtRmResult.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(PacketAtRmClear.Handler.class, PacketAtRmClear.class, 2, Side.SERVER);
    }
}
