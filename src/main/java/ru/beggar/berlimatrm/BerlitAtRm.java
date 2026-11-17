package ru.beggar.berlimatrm;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/**
 * Berlit_ATRM - an Avaritia 9x9 recipe maker with the Berlit GUI style.
 */
@Mod(
    modid = BerlitAtRm.MODID,
    name = BerlitAtRm.NAME,
    version = Tags.VERSION,
    dependencies = "required-after:MineTweaker3;required-after:Avaritia")
public class BerlitAtRm {

    public static final String MODID = "berlimatrm";
    public static final String NAME = "Berlit_ATRM";

    @Mod.Instance(MODID)
    public static BerlitAtRm instance;

    @SidedProxy(clientSide = "ru.beggar.berlimatrm.ClientProxy", serverSide = "ru.beggar.berlimatrm.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
