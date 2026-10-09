package gr8pefish.ironbackpacks.fabric;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.platform.IPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The Fabric side of {@link IPlatform}: registers right away, Fabric registries, attachments, networking. */
public class FabricPlatform implements IPlatform {

    /** 1.12 capability "equipped_backpack". */
    private static final AttachmentType<ItemStack> EQUIPPED_BACKPACK = AttachmentRegistry.<ItemStack>builder()
            .persistent(ItemStack.OPTIONAL_CODEC).initializer(() -> ItemStack.EMPTY)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(IronBackpacks.MODID, "equipped_backpack"));

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        Registry<R> target = (Registry<R>) BuiltInRegistries.REGISTRY.get(registry.location());
        T entry = Registry.register(target, ResourceLocation.fromNamespaceAndPath(IronBackpacks.MODID, name), factory.get());
        return () -> entry;
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultKey) {
        return FabricRegistryBuilder.createDefaulted(key, defaultKey).attribute(RegistryAttribute.SYNCED).buildAndRegister();
    }

    @Override
    public boolean upgradeEnabled(String upgrade) {
        return FabricConfig.upgradeEnabled(upgrade);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public MenuType<ContainerBackpack> createBackpackMenuType() {
        return new ExtendedScreenHandlerType<>(ContainerBackpack::fromNetwork, ContainerBackpack.OpenData.STREAM_CODEC.cast());
    }

    @Override
    public void openBackpackMenu(ServerPlayer player, MenuProvider provider, ContainerBackpack.OpenData data) {
        player.openMenu(new ExtendedScreenHandlerFactory<ContainerBackpack.OpenData>() {
            @Override
            public ContainerBackpack.OpenData getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player p) {
                return provider.createMenu(windowId, inventory, p);
            }
        });
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public ItemStack getEquippedBackpack(Player player) {
        return player.getAttachedOrElse(EQUIPPED_BACKPACK, ItemStack.EMPTY);
    }

    @Override
    public void setEquippedBackpack(Player player, ItemStack stack) {
        player.setAttached(EQUIPPED_BACKPACK, stack);
    }

    @Override
    public void configureMockConnection(Connection connection) {
        // Fabric sends to any connection
    }
}
