package gr8pefish.ironbackpacks.fabric;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

/** Recipe load condition {@code ironbackpacks:upgrade_enabled} (see the NeoForge one): the upgrade is registered. */
public record UpgradeEnabledCondition(ResourceLocation upgrade) implements ResourceCondition {

    public static final MapCodec<UpgradeEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("upgrade").forGetter(UpgradeEnabledCondition::upgrade)
    ).apply(i, UpgradeEnabledCondition::new));
    public static final ResourceConditionType<UpgradeEnabledCondition> TYPE = ResourceConditionType.create(RegistrarIronBackpacks.id("upgrade_enabled"), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(@Nullable HolderLookup.Provider registryLookup) {
        return !IronBackpacksAPI.getUpgrade(upgrade).isNull();
    }
}
