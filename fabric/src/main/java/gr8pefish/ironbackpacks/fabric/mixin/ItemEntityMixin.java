package gr8pefish.ironbackpacks.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gr8pefish.ironbackpacks.core.UpgradeEventHandler;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Everlasting on Fabric (no item expire event): a backpack about to despawn gets an unlimited lifetime instead. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    private int age;

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public abstract void setUnlimitedLifetime();

    @Inject(method = "tick", at = @At("HEAD"))
    private void ironbackpacks$everlasting(CallbackInfo ci) {
        // vanilla removes the item when age reaches 6000 after this tick's increment
        if (age >= 5999 && age != Short.MIN_VALUE && !((ItemEntity) (Object) this).level().isClientSide && UpgradeEventHandler.keepsAlive(getItem()))
            setUnlimitedLifetime();
    }
}
