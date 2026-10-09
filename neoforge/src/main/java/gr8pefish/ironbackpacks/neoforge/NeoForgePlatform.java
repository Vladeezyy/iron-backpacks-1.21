package gr8pefish.ironbackpacks.neoforge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.platform.IPlatform;
import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** The NeoForge side of {@link IPlatform}: deferred registers on the mod bus, NeoForge registries, attachments, payloads. */
public class NeoForgePlatform implements IPlatform {

    /** Set by {@link #init} from the mod constructor, before the common registrar loads. */
    private static IEventBus modBus;
    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new HashMap<>();
    private static final List<Registry<?>> NEW_REGISTRIES = new ArrayList<>();

    /** 1.12 capability "equipped_backpack". */
    private static Supplier<AttachmentType<ItemStack>> EQUIPPED_BACKPACK;

    static void init(IEventBus bus) {
        modBus = bus;
        EQUIPPED_BACKPACK = deferred(NeoForgeRegistries.Keys.ATTACHMENT_TYPES).register("equipped_backpack",
                () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());
    }

    @SuppressWarnings("unchecked")
    static <R> DeferredRegister<R> deferred(ResourceKey<? extends Registry<R>> registry) {
        return (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry, key -> {
            DeferredRegister<R> register = DeferredRegister.create(registry, IronBackpacks.MODID);
            register.register(modBus);
            return register;
        });
    }

    static void onNewRegistry(NewRegistryEvent event) {
        NEW_REGISTRIES.forEach(event::register);
    }

    @Override
    public String name() {
        return "neoforge";
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        return deferred(registry).register(name, factory);
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultKey) {
        Registry<T> registry = new RegistryBuilder<>(key).defaultKey(defaultKey).sync(true).create();
        NEW_REGISTRIES.add(registry);
        return registry;
    }

    @Override
    public boolean upgradeEnabled(String upgrade) {
        return ConfigHandler.upgradeEnabled(upgrade);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public MenuType<ContainerBackpack> createBackpackMenuType() {
        return IMenuTypeExtension.create((windowId, inventory, buf) ->
                ContainerBackpack.fromNetwork(windowId, inventory, ContainerBackpack.OpenData.STREAM_CODEC.decode(buf)));
    }

    @Override
    public void openBackpackMenu(ServerPlayer player, MenuProvider provider, ContainerBackpack.OpenData data) {
        player.openMenu(provider, buf -> ContainerBackpack.OpenData.STREAM_CODEC.encode(buf, data));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public ItemStack getEquippedBackpack(Player player) {
        return player.getData(EQUIPPED_BACKPACK);
    }

    @Override
    public void setEquippedBackpack(Player player, ItemStack stack) {
        player.setData(EQUIPPED_BACKPACK, stack);
    }

    @Override
    public void configureMockConnection(Connection connection) {
        NetworkRegistry.configureMockConnection(connection);
    }
}
