package gr8pefish.ironbackpacks.core;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;

public class UpgradeEventHandler {

    /** Everlasting: a dropped backpack doesn't despawn (1.12: extra life, setNoDespawn, cancel the expiry). */
    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (stack.getItem() instanceof IBackpack) {
            BackpackInfo info = ((IBackpack) stack.getItem()).getBackpackInfo(stack);
            if (info.hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_EVERLASTING))) {
                event.setExtraLife(6000);
                event.getEntity().setUnlimitedLifetime();
            }
        }
    }
}
