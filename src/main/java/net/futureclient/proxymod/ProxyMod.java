package net.futureclient.proxymod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ProxyMod.MODID, name = ProxyMod.NAME, version = ProxyMod.VERSION, clientSideOnly = true)
public class ProxyMod {

    public static final String MODID = "proxymod";
    public static final String NAME = "Proxy Mod";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Proxymod initialised.");
    }
}
