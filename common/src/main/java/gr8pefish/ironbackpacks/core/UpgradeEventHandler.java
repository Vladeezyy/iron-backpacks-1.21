package gr8pefish.ironbackpacks.core;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import net.minecraft.world.item.ItemStack;

public class UpgradeEventHandler {

    /** 1.12 onItemExpire: extra life for a dropped everlasting backpack. */
    public static final int EXTRA_LIFE = 6000;

    /**
     * Everlasting: a dropped backpack doesn't despawn (1.12: extra life, setNoDespawn, cancel the expiry). The
     * loaders ask this when the item entity expires (NeoForge ItemExpireEvent, Fabric mixin into ItemEntity.tick) and
     * then make its lifetime unlimited.
     */
    public static boolean keepsAlive(ItemStack stack) {
        if (stack.getItem() instanceof IBackpack) {
            BackpackInfo info = ((IBackpack) stack.getItem()).getBackpackInfo(stack);
            return info.hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_EVERLASTING));
        }
        return false;
    }
}
