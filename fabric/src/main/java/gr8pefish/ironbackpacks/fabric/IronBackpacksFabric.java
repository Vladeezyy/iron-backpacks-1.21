package gr8pefish.ironbackpacks.fabric;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import gr8pefish.ironbackpacks.core.recipe.BackpackTierRecipe;
import net.minecraft.core.Registry;

public class IronBackpacksFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        IronBackpacks.init();
        RegistrarIronBackpacks.registerBackpackTypes((id, type) -> Registry.register(RegistrarIronBackpacks.REGISTRY_TYPES, id, type));
        RegistrarIronBackpacks.registerUpgrades((id, upgrade) -> Registry.register(RegistrarIronBackpacks.REGISTRY_UPGRADES, id, upgrade));
        ResourceConditions.register(UpgradeEnabledCondition.TYPE);
        // 26.x clients get no recipes unless asked: the tier recipes for the JEI category
        RecipeSynchronization.synchronizeRecipeSerializer(BackpackTierRecipe.SERIALIZER);

        PayloadTypeRegistry.serverboundPlay().register(MessageRequestAction.TYPE, MessageRequestAction.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MessageSetEquippedBackpack.TYPE, MessageSetEquippedBackpack.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MessageRequestAction.TYPE, (message, context) -> MessageRequestAction.handle(message, context.player()));

        // 1.12 CommonProxy.init; at server start so every mod's items exist for the blacklist (the client: IronBackpacksFabricClient)
        IronBackpacks.setup();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> IronBackpacks.setup());
    }
}
