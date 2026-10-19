package gr8pefish.ironbackpacks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Performance check for development, inert unless {@code ironbackpacks.perf} is set ({@code ./gradlew
 * :neoforge:runPerf} / {@code :fabric:runPerf}, world {@code run/saves/ib_scene}). The player stands on a sky platform
 * with the inventory screen open; measured one after another, each against the same vanilla case:
 * <ul>
 * <li>36 vanilla shulker boxes full of cobblestone in the inventory;</li>
 * <li>36 full diamond storage backpacks, without upgrades and with the damage bar upgrade (the bar is asked every frame);</li>
 * <li>400 full shulker boxes, then 400 full backpacks lying on the ground (item entities: server tick and rendering).</li>
 * </ul>
 * Logs {@code [perf] PERF} lines (average FPS and server tick), then quits.
 */
public final class PerfScene {
    private static final boolean ENABLED = System.getProperty("ironbackpacks.perf") != null;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    private static BlockPos base;
    private static boolean measuring;
    private static long fpsSum, tickNanos;
    private static int fpsSamples, tickSamples;

    private record Step(int at, Consumer<Minecraft> action) {}

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    private static void initSteps() {
        int t = 40;
        at(t, mc -> {
            // nothing may cap the frame rate, or both sides of a comparison read the same number
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            // 1.21.2+: the frame rate drops to 30 FPS after 30 s without input (the agent never touches the game)
            mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
            mc.options.cloudStatus().set(CloudStatus.OFF);
            server(mc, PerfScene::platform);
        });
        t += 300;
        String[] cases = {"empty inventory", "36 full shulker boxes", "36 full diamond backpacks", "36 full diamond backpacks with the damage bar"};
        for (int i = 0; i < cases.length; i++) {
            int kind = i;
            at(t, mc -> {
                mc.setScreen(null);
                server(mc, p -> fillInventory(p, kind));
            });
            at(t + 10, mc -> mc.setScreen(new InventoryScreen(mc.player)));
            at(t + 60, mc -> start());
            at(t + 260, mc -> stop(cases[kind] + ", inventory open"));
            t += 270;
        }
        at(t, mc -> {
            mc.setScreen(null);
            server(mc, p -> fillInventory(p, 0));
        });
        String[] drops = {"400 full shulker boxes on the ground", "400 full diamond backpacks on the ground"};
        for (int i = 0; i < drops.length; i++) {
            int kind = i;
            at(t + 5, mc -> server(mc, p -> drop(p, kind == 1)));
            at(t + 65, mc -> start());
            at(t + 265, mc -> stop(drops[kind]));
            t += 270;
        }
        at(t, mc -> {
            server(mc, p -> clearDrops(p.level()));
            IronBackpacks.LOGGER.info("[perf] DONE");
            mc.stop();
        });
    }

    /** End of every client tick (called from ClientEventHandler.onClientTick). */
    public static void onTick(Minecraft mc) {
        if (!ENABLED) return;
        if (DevScene.createWorld(mc)) return;
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        if (tick == 0 && STEPS.isEmpty()) initSteps();
        // the window is usually unfocused while this runs: never pause, or the integrated server stops
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (measuring && tick % 20 == 0) {
            fpsSum += mc.getFps();
            fpsSamples++;
        }
        if (measuring) {
            tickNanos += mc.getSingleplayerServer().getAverageTickTimeNanos();
            tickSamples++;
        }
        tick++;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                try {
                    step.action().accept(mc);
                } catch (RuntimeException e) {
                    IronBackpacks.LOGGER.error("[perf] step at tick {} failed", tick, e);
                }
            }
        }
    }

    private static void start() {
        fpsSum = tickNanos = 0;
        fpsSamples = tickSamples = 0;
        measuring = true;
    }

    private static void stop(String name) {
        measuring = false;
        double fps = fpsSamples == 0 ? 0 : fpsSum / (double) fpsSamples;
        double mspt = tickSamples == 0 ? 0 : tickNanos / (double) tickSamples / 1e6;
        IronBackpacks.LOGGER.info(String.format("[perf] PERF %s: %.1f FPS, %.2f ms per server tick", name, fps, mspt));
    }

    /** A stone platform high in the air east of the spawn, the player in survival standing on it. */
    private static void platform(ServerPlayer player) {
        ServerLevel level = player.level();
        DevScene.command(level, "time set noon");
        DevScene.command(level, "weather clear");
        BlockPos spawn = level.getRespawnData().pos();
        base = new BlockPos(spawn.getX() + 200, 150, spawn.getZ());
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-12, -1, -12), base.offset(12, 3, 12))) {
            level.setBlock(p, p.getY() == base.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        clearDrops(level);
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, java.util.Set.of(), 0, 30, true);
    }

    private static ItemStack fullShulker() {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return box;
    }

    private static ItemStack fullBackpack(boolean damageBar) {
        ItemStack stack = IronBackpacksAPI.getStack(IronBackpacksAPI.getBackpackType(RegistrarIronBackpacks.id("diamond")), BackpackSpecialty.STORAGE);
        if (damageBar) stack = EventHandler.tryUpgradeBackpack(stack, IronBackpacksAPI.getStack(IronBackpacksAPI.getUpgrade(RegistrarIronBackpacks.id("damage_bar"))));
        BackpackInfo info = BackpackInfo.fromStack(stack);
        for (int i = 0; i < info.getInventory().getSlots(); i++) info.getInventory().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        return IronBackpacksAPI.applyPackInfo(stack, info);
    }

    /** 0 empty, 1 shulker boxes, 2 backpacks, 3 backpacks with the damage bar: every main inventory and hotbar slot. */
    private static void fillInventory(ServerPlayer player, int kind) {
        var inv = player.getInventory();
        inv.clearContent();
        if (kind == 0) return;
        ItemStack stack = kind == 1 ? fullShulker() : fullBackpack(kind == 3);
        for (int i = 0; i < 36; i++) inv.setItem(i, stack.copy());
    }

    private static void drop(ServerPlayer player, boolean backpacks) {
        ServerLevel level = player.level();
        clearDrops(level);
        ItemStack stack = backpacks ? fullBackpack(false) : fullShulker();
        for (int i = 0; i < 400; i++) {
            ItemEntity item = new ItemEntity(level, base.getX() - 9.5 + (i % 20), base.getY() + 0.1, base.getZ() + 2.5 + (i / 20) * 0.5, stack.copy(), 0, 0, 0);
            item.setPickUpDelay(32767);
            level.addFreshEntity(item);
        }
    }

    private static void clearDrops(ServerLevel level) {
        if (base == null) return;
        level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(20)).forEach(ItemEntity::discard);
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server.getPlayerList().getPlayer(mc.player.getUUID()));
            } catch (RuntimeException e) {
                IronBackpacks.LOGGER.error("[perf] server step failed", e);
            }
        });
    }

    private PerfScene() {}
}
