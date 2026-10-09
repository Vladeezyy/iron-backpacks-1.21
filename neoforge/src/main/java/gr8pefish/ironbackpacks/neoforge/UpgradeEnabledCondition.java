package gr8pefish.ironbackpacks.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Recipe condition {@code ironbackpacks:upgrade_enabled}: the upgrade is registered (1.12 registered an upgrade's
 * recipe only when the upgrade was enabled in the config).
 */
public record UpgradeEnabledCondition(ResourceLocation upgrade) implements ICondition {

    public static final MapCodec<UpgradeEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("upgrade").forGetter(UpgradeEnabledCondition::upgrade)
    ).apply(i, UpgradeEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return !IronBackpacksAPI.getUpgrade(upgrade).isNull();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
