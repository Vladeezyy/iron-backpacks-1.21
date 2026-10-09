package gr8pefish.ironbackpacks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.util.InventoryBlacklist;

/** Shared by both loaders (1.12 IronBackpacks + CommonProxy); the loader entry points call these. */
public final class IronBackpacks {

    public static final String MODID = "ironbackpacks";
    public static final String NAME = "Iron Backpacks";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    /** Mod construction: hands every registry entry to the loader. */
    public static void init() {
        RegistrarIronBackpacks.init();
    }

    /** 1.12 CommonProxy.init: after registration. */
    public static void setup() {
        IronBackpacksAPI.initBackpackVariantList();
        InventoryBlacklist.initBlacklist();
    }

    private IronBackpacks() {}
}
