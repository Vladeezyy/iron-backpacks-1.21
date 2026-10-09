package gr8pefish.ironbackpacks.platform;

import java.nio.file.Path;
import java.util.function.Supplier;

import gr8pefish.ironbackpacks.container.ContainerBackpack;
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

/** What the common code needs from the mod loader (NeoForge or Fabric); found with {@link Services}. */
public interface IPlatform {

    /** "neoforge" or "fabric". */
    String name();

    boolean isClient();

    Path configDir();

    // Registration

    /** Registers an entry (NeoForge: through a deferred register, Fabric: right away); the supplier gives the entry once registered. */
    <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory);

    /** Creates a synced registry with a default entry (the backpack types and upgrades). */
    <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, ResourceLocation defaultKey);

    /** Whether an upgrade is switched on in the config (1.12 Upgrades.enableDamageBar and so on). */
    boolean upgradeEnabled(String upgrade);

    CreativeModeTab.Builder creativeTabBuilder();

    // Menus

    MenuType<ContainerBackpack> createBackpackMenuType();

    void openBackpackMenu(ServerPlayer player, MenuProvider provider, ContainerBackpack.OpenData data);

    // Networking

    void sendToServer(CustomPacketPayload payload);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    // Player data (1.12 capability "equipped_backpack")

    ItemStack getEquippedBackpack(Player player);

    void setEquippedBackpack(Player player, ItemStack stack);

    // GameTests

    /** Lets a GameTest mock player's connection carry every mod's packets. */
    void configureMockConnection(Connection connection);
}
