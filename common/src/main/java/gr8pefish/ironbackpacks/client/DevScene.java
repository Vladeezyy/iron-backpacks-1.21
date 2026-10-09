package gr8pefish.ironbackpacks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * A scripted test scene for development: {@code ./gradlew runScene} joins the {@code ib_scene} world, fills the
 * hotbar with every backpack, opens their GUIs, the creative tab, an anvil and the JEI tier category, saves
 * screenshots to {@code run/screenshots/scene_*.png} and quits. Inert unless the {@code ironbackpacks.scene} system
 * property is set.
 */
public final class DevScene {
    private static final boolean ENABLED = System.getProperty("ironbackpacks.scene") != null;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    private static BlockPos anvilPos;

    private record Step(int at, Consumer<Minecraft> action) {}

    static {
        at(40, mc -> server(mc, DevScene::build));
        at(80, mc -> shot(mc, "hotbar"));
        at(85, mc -> mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)));
        at(90, mc -> selectTab(mc));
        at(100, mc -> shot(mc, "creative_tab"));
        at(105, mc -> mc.setScreen(null));
        String[] names = {"basic", "iron_storage", "iron_upgrade", "gold_storage", "gold_upgrade", "diamond_storage", "diamond_upgrade"};
        for (int i = 0; i < names.length; i++) {
            int slot = i, t = 110 + i * 30;
            String name = names[i];
            at(t, mc -> select(mc, slot));
            at(t + 5, mc -> server(mc, p -> ContainerBackpack.open(p, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND)));
            at(t + 20, mc -> shot(mc, "gui_" + name));
            at(t + 25, mc -> mc.player.closeContainer());
        }
        at(320, mc -> mc.setScreen(new Showcase(packWithUpgrades())));
        at(326, mc -> shot(mc, "tooltip"));
        at(328, mc -> ClientEventHandler.sceneShift = true);
        at(334, mc -> shot(mc, "tooltip_shift"));
        at(336, mc -> mc.setScreen(new Showcase(upgrade("everlasting"))));
        at(342, mc -> shot(mc, "tooltip_upgrade"));
        at(344, mc -> ClientEventHandler.sceneShift = false);
        at(345, mc -> mc.setScreen(new Lineup()));
        at(348, mc -> shot(mc, "lineup"));
        at(349, mc -> mc.setScreen(null));
        at(350, mc -> server(mc, DevScene::openAnvil));
        at(370, mc -> shot(mc, "anvil"));
        at(375, mc -> mc.player.closeContainer());
        at(380, mc -> openJei());
        at(400, mc -> shot(mc, "jei_tier"));
        at(405, mc -> mc.setScreen(null));
        // other mods' inventory additions (Curios / Accessories slots) next to the backpacks, and key conflicts
        at(410, mc -> server(mc, p -> p.setGameMode(GameType.SURVIVAL)));
        at(420, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(435, mc -> shot(mc, "player_inventory"));
        at(440, mc -> mc.setScreen(null));
        at(445, DevScene::logKeyConflicts);
        at(450, mc -> select(mc, 1));
        at(455, mc -> server(mc, p -> ContainerBackpack.open(p, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND)));
        at(470, mc -> shot(mc, "gui_survival"));
        at(475, mc -> mc.player.closeContainer());
        at(490, mc -> mc.stop());
    }

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    /** End of every client tick (called by ClientEventHandler). */
    static void onTick(Minecraft mc) {
        if (!ENABLED) return;
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        // The window is usually unfocused while an agent runs this: never pause, or the integrated server stops.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        tick++;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                IronBackpacks.LOGGER.info("[scene] tick {}", tick);
                step.action().accept(mc);
            }
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    private static ItemStack pack(String type, BackpackSpecialty specialty) {
        return IronBackpacksAPI.getStack(IronBackpacksAPI.getBackpackType(RegistrarIronBackpacks.id(type)), specialty);
    }

    private static ItemStack upgrade(String name) {
        return IronBackpacksAPI.getStack(IronBackpacksAPI.getUpgrade(RegistrarIronBackpacks.id(name)));
    }

    /** A backpack with some things in it (the first slots) and an optional colour. */
    static ItemStack filled(ItemStack stack, int color, ItemStack... contents) {
        BackpackInfo info = BackpackInfo.fromStack(stack);
        for (int i = 0; i < contents.length && i < info.getInventory().getSlots(); i++)
            info.getInventory().setStackInSlot(i, contents[i]);
        info.setRGBColor(color);
        return IronBackpacksAPI.applyPackInfo(stack, info);
    }

    private static ItemStack packWithUpgrades() {
        ItemStack stack = pack("diamond", BackpackSpecialty.UPGRADE);
        for (String u : new String[] {"damage_bar", "lock", "everlasting"})
            stack = EventHandler.tryUpgradeBackpack(stack, upgrade(u));
        ItemStack[] contents = new ItemStack[30];
        for (int i = 0; i < contents.length; i++) contents[i] = new ItemStack(i % 2 == 0 ? Items.COBBLESTONE : Items.DIRT, 64);
        return filled(stack, -1, contents);
    }

    /** Noon on the flat world; the hotbar holds every backpack variant, the main inventory some loot. */
    private static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()), spawn.getZ());
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 20f);
        anvilPos = base.offset(0, 0, -2);
        level.setBlockAndUpdate(anvilPos, Blocks.ANVIL.defaultBlockState());

        var inv = player.getInventory();
        inv.clearContent();
        inv.setItem(0, filled(pack("basic", BackpackSpecialty.NONE), -1, new ItemStack(Items.APPLE, 12), new ItemStack(Items.TORCH, 48)));
        inv.setItem(1, filled(pack("iron", BackpackSpecialty.STORAGE), DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF,
                new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.COAL, 64), new ItemStack(Items.REDSTONE, 40)));
        inv.setItem(2, filled(pack("iron", BackpackSpecialty.UPGRADE), DyeColor.LIGHT_BLUE.getTextureDiffuseColor() & 0xFFFFFF, new ItemStack(Items.BREAD, 16)));
        inv.setItem(3, filled(pack("gold", BackpackSpecialty.STORAGE), DyeColor.LIME.getTextureDiffuseColor() & 0xFFFFFF, new ItemStack(Items.GOLD_INGOT, 20), new ItemStack(Items.DIAMOND, 3)));
        inv.setItem(4, filled(pack("gold", BackpackSpecialty.UPGRADE), -1, new ItemStack(Items.OAK_LOG, 64)));
        inv.setItem(5, filled(pack("diamond", BackpackSpecialty.STORAGE), DyeColor.PURPLE.getTextureDiffuseColor() & 0xFFFFFF, new ItemStack(Items.EMERALD, 9), new ItemStack(Items.ENDER_PEARL, 16)));
        inv.setItem(6, packWithUpgrades());
        inv.setItem(7, upgrade("damage_bar"));
        inv.setItem(8, new ItemStack(RegistrarIronBackpacks.UPGRADE.get()));
        inv.setItem(9, upgrade("lock"));
        inv.setItem(10, upgrade("extra_upgrade"));
        inv.setItem(11, upgrade("everlasting"));
        inv.setItem(12, new ItemStack(Items.SHEARS));
        inv.selected = 0;
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().selected = slot;
        server(mc, p -> p.getInventory().selected = slot);
    }

    private static void selectTab(Minecraft mc) {
        if (!(mc.screen instanceof CreativeModeInventoryScreen screen)) return;
        try {
            var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            method.setAccessible(true);
            // Fabric pages the tabs (FabricCreativeInventoryScreen.switchToPage): go to the one with ours first
            try {
                int page = (int) screen.getClass().getMethod("getPage", net.minecraft.world.item.CreativeModeTab.class).invoke(screen, RegistrarIronBackpacks.TAB_IB.get());
                screen.getClass().getMethod("switchToPage", int.class).invoke(screen, page);
            } catch (NoSuchMethodException notFabric) {
                // NeoForge selects the tab and its page
            }
            method.invoke(screen, RegistrarIronBackpacks.TAB_IB.get());
        } catch (ReflectiveOperationException e) {
            IronBackpacks.LOGGER.warn("[scene] could not select the creative tab", e);
        }
    }

    /** An anvil with an iron backpack and a damage bar upgrade: the result has the upgrade installed. */
    private static void openAnvil(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AnvilMenu(id, inv, ContainerLevelAccess.create(level, anvilPos)),
                Component.translatable("container.repair")));
        if (player.containerMenu instanceof AnvilMenu anvil) {
            anvil.getSlot(0).set(pack("iron", BackpackSpecialty.STORAGE));
            anvil.getSlot(1).set(upgrade("damage_bar"));
            anvil.createResult();
            anvil.broadcastChanges();
        }
    }

    /** Every other key mapping bound to the same key as ours (default I / H). */
    private static void logKeyConflicts(Minecraft mc) {
        for (var ours : new net.minecraft.client.KeyMapping[] {ClientEventHandler.KEY_OPEN, ClientEventHandler.KEY_EQUIP}) {
            for (var other : mc.options.keyMappings) {
                if (other != ours && !ours.isUnbound() && other.same(ours))
                    IronBackpacks.LOGGER.info("[scene] key conflict: {} and {} on {}", ours.getName(), other.getName(), ours.saveString());
            }
        }
        IronBackpacks.LOGGER.info("[scene] key check done");
    }

    private static void openJei() {
        var runtime = gr8pefish.ironbackpacks.integration.jei.IronBackpacksJEIPlugin.runtime;
        if (runtime != null)
            runtime.getRecipesGui().showTypes(List.of(gr8pefish.ironbackpacks.integration.jei.RecipeCategoryTier.TYPE));
    }

    /** An item with its tooltip in the middle of the screen. */
    private static final class Showcase extends Screen {
        private final ItemStack stack;

        Showcase(ItemStack stack) {
            super(Component.empty());
            this.stack = stack;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            g.renderItem(stack, width / 2 - 100, height / 2 - 40);
            g.renderItemDecorations(font, stack, width / 2 - 100, height / 2 - 40);
            g.renderTooltip(font, stack, width / 2 - 80, height / 2 - 40);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    /** Every backpack variant, then dyed ones and the upgrades, drawn large with their names. */
    private static final class Lineup extends Screen {
        Lineup() {
            super(Component.empty());
        }

        private static ItemStack dyed(String type, BackpackSpecialty specialty, DyeColor color) {
            return filled(pack(type, specialty), color.getTextureDiffuseColor() & 0xFFFFFF);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFF20242C);
            ItemStack[][] rows = {
                    {pack("basic", BackpackSpecialty.NONE), pack("iron", BackpackSpecialty.STORAGE), pack("iron", BackpackSpecialty.UPGRADE),
                            pack("gold", BackpackSpecialty.STORAGE), pack("gold", BackpackSpecialty.UPGRADE), pack("diamond", BackpackSpecialty.STORAGE),
                            pack("diamond", BackpackSpecialty.UPGRADE)},
                    {dyed("basic", BackpackSpecialty.NONE, DyeColor.RED), dyed("iron", BackpackSpecialty.STORAGE, DyeColor.ORANGE),
                            dyed("iron", BackpackSpecialty.UPGRADE, DyeColor.YELLOW), dyed("gold", BackpackSpecialty.STORAGE, DyeColor.LIME),
                            dyed("gold", BackpackSpecialty.UPGRADE, DyeColor.LIGHT_BLUE), dyed("diamond", BackpackSpecialty.STORAGE, DyeColor.PURPLE),
                            dyed("diamond", BackpackSpecialty.UPGRADE, DyeColor.MAGENTA)},
                    {new ItemStack(RegistrarIronBackpacks.UPGRADE.get()), upgrade("damage_bar"), upgrade("lock"), upgrade("extra_upgrade"),
                            upgrade("everlasting")}};
            int scale = 3, cell = 34 * scale / 2, top = height / 2 - 3 * cell / 2 - 10;
            for (int r = 0; r < rows.length; r++) {
                int rowCell = r == 2 ? cell * 3 / 2 : cell;   // the upgrade names are longer
                int left = width / 2 - rows[r].length * rowCell / 2;
                for (int i = 0; i < rows[r].length; i++) {
                    ItemStack stack = rows[r][i];
                    int x = left + i * rowCell + (rowCell - cell) / 2, y = top + r * (cell + 14);
                    g.pose().pushPose();
                    g.pose().translate(x + (cell - 16 * scale) / 2f, y, 0);
                    g.pose().scale(scale, scale, 1);
                    g.renderItem(stack, 0, 0);
                    g.pose().popPose();
                    if (r == 0 || r == 2) {
                        Component name = stack.getHoverName();
                        var variant = BackpackInfo.fromStack(stack).getVariant();
                        Component spec = r == 0 && variant.getBackpackType().hasSpecialties()
                                ? Component.translatable("tooltip.ironbackpacks.backpack.emphasis." + variant.getBackpackSpecialty().getName()) : null;
                        g.pose().pushPose();
                        g.pose().translate(x + cell / 2f, y + 16 * scale + 2, 0);
                        g.pose().scale(0.5f, 0.5f, 1);
                        g.drawCenteredString(font, name, 0, 0, 0xFFFFFF);
                        if (spec != null) g.drawCenteredString(font, spec, 0, 10, 0xA0A0A0);
                        g.pose().popPose();
                    }
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(),
                msg -> IronBackpacks.LOGGER.info("[scene] {}", msg.getString()));
    }

    private DevScene() {}
}
