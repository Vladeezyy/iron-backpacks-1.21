package gr8pefish.ironbackpacks.api.backpack;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import net.minecraft.world.item.ItemStack;

public interface IBackpack {

    @Nonnull
    default BackpackInfo getBackpackInfo(@Nonnull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");

        return BackpackInfo.fromStack(stack);
    }

    default int getBackpackColor(@Nonnull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");

        return BackpackInfo.getColor(stack);
    }

    default void updateBackpack(@Nonnull ItemStack stack, @Nonnull BackpackInfo backpackInfo) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");
        Preconditions.checkNotNull(backpackInfo, "BackpackInfo cannot be null");

        IronBackpacksAPI.applyPackInfo(stack, backpackInfo);
    }
}
