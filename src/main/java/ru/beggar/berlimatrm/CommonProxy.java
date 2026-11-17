package ru.beggar.berlimatrm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import ru.beggar.berlimatrm.command.CommandAtRm;
import ru.beggar.berlimatrm.compat.AvaritiaTweaker;
import ru.beggar.berlimatrm.gui.AtRmGuiHandler;
import ru.beggar.berlimatrm.network.ModNetwork;
import ru.beggar.berlimatrm.network.ServerTaskQueue;
import ru.beggar.berlimatrm.recipe.AtRmRecipes;
import ru.beggar.berlimatrm.selftest.AtRmSelftest;

public class CommonProxy {

    public static final Logger LOG = LogManager.getLogger(BerlitAtRm.MODID);

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronize(event.getSuggestedConfigurationFile());
        AtRmRecipes.init(event.getModConfigurationDirectory());
        AvaritiaTweaker.register();
        // TickEvent is an FML bus event in 1.7.10, not a Forge bus event.
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerTaskQueue.Handler());

        LOG.info(
            "Berlit_ATRM {} loaded (requireOp={}, maxRecipes={})",
            Tags.VERSION,
            Config.requireOp,
            Config.maxRecipes);
    }

    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(BerlitAtRm.instance, new AtRmGuiHandler());
        ModNetwork.init();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAtRm());
        try {
            AtRmRecipes.applyPersisted();
            AtRmRecipes.exportScript();
        } catch (Throwable t) {
            LOG.error("Could not apply saved Avaritia recipes", t);
        }

        String selftest = System.getenv("BERLIT_ATRM_SELFTEST");
        if (selftest != null && !selftest.isEmpty() && !"0".equals(selftest)) {
            AtRmSelftest.run();
        }
    }
}
