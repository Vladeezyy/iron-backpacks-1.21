package gr8pefish.ironbackpacks.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackVariant;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.item.ItemBackpack;
import gr8pefish.ironbackpacks.network.RequestAction;
import gr8pefish.ironbackpacks.util.InventoryBlacklist;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import gr8pefish.ironbackpacks.platform.Services;

/**
 * In-game checks for the port, run headless with {@code ./gradlew :neoforge:runGameTestServer} and
 * {@code ./gradlew :fabric:runGametest}. They compare the port with the 1.12.2 behaviour (numbers taken from the 1.12.2
 * source). The loaders register this class with their game test registries.
 */
public final class IBGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    private static final String STRUCTURE = IronBackpacks.MODID + ":gametest_area";

    static {
        TESTS.put("variants", IBGameTests::variants);
        TESTS.put("stack_round_trip", IBGameTests::stackRoundTrip);
        TESTS.put("tier_recipe_keeps_contents", IBGameTests::tierRecipe);
        TESTS.put("basic_recipe", IBGameTests::basicRecipe);
        TESTS.put("color_recipes", IBGameTests::colorRecipes);
        TESTS.put("upgrade_recipes", IBGameTests::upgradeRecipes);
        TESTS.put("anvil_rules", IBGameTests::anvilRules);
        TESTS.put("anvil_menu", IBGameTests::anvilMenu);
        TESTS.put("owner_and_lock", IBGameTests::ownerAndLock);
        TESTS.put("damage_bar", IBGameTests::damageBar);
        TESTS.put("everlasting", IBGameTests::everlasting);
        TESTS.put("menu_layout", IBGameTests::menuLayout);
        TESTS.put("menu_blocks_open_backpack", IBGameTests::menuBlocks);
        TESTS.put("menu_saves_on_close", IBGameTests::menuSaves);
        TESTS.put("blacklist", IBGameTests::blacklist);
        TESTS.put("open_key", IBGameTests::openKey);
        TESTS.put("names", IBGameTests::names);
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, body) -> functions.add(new TestFunction(IronBackpacks.MODID, IronBackpacks.MODID + "." + name, STRUCTURE,
                net.minecraft.world.level.block.Rotation.NONE, 100, 0, true, false, 1, 1, false, body)));
        return functions;
    }

    // --- helpers --------------------------------------------------------------------------------------------------

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, message);
    }

    /** A survival mock player that can open menus (GameTestHelper.makeMockServerPlayerInLevel is creative). */
    static ServerPlayer player(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "test-player"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        // a client with every mod's channels, so menus and other mods' sync packets can be sent
        Services.PLATFORM.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 2)).getBottomCenter();
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static ItemStack pack(String type, BackpackSpecialty specialty) {
        return IronBackpacksAPI.getStack(IronBackpacksAPI.getBackpackType(RegistrarIronBackpacks.id(type)), specialty);
    }

    private static BackpackUpgrade upgrade(String name) {
        return IronBackpacksAPI.getUpgrade(RegistrarIronBackpacks.id(name));
    }

    private static ItemStack craft(GameTestHelper helper, int width, int height, ItemStack... grid) {
        CraftingInput input = CraftingInput.of(width, height, List.of(grid));
        var level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    // --- tests ----------------------------------------------------------------------------------------------------

    /** 1.12.2 numbers: basic 9x2 / 4 points; specialties add a row (7 rows: 2 columns) or 5 points. */
    private static void variants(GameTestHelper helper) {
        List<BackpackVariant> variants = IronBackpacksAPI.getBackpackVariantList();
        String expected = "basic none 9x2 4|iron storage 9x4 7|iron upgrade 9x3 12|gold storage 9x6 12|gold upgrade 9x5 17|"
                + "diamond storage 11x7 18|diamond upgrade 9x7 23";
        StringBuilder actual = new StringBuilder();
        for (BackpackVariant v : variants) {
            if (!actual.isEmpty()) actual.append('|');
            actual.append(v.getBackpackType().getIdentifier().getPath()).append(' ').append(v.getBackpackSpecialty().getName()).append(' ')
                    .append(v.getBackpackSize().getColumns()).append('x').append(v.getBackpackSize().getRows()).append(' ')
                    .append(new BackpackInfo(v).getMaxPoints());
        }
        check(helper, actual.toString().equals(expected), "variants: " + actual);
        helper.succeed();
    }

    private static void stackRoundTrip(GameTestHelper helper) {
        ItemStack stack = pack("iron", BackpackSpecialty.STORAGE);
        BackpackInfo info = BackpackInfo.fromStack(stack);
        check(helper, info.getInventory().getSlots() == 36, "slots " + info.getInventory().getSlots());
        info.getInventory().setStackInSlot(35, new ItemStack(Items.DIAMOND, 7));
        info.addUpgrade(upgrade("damage_bar")).setRGBColor(0x123456).setOwner(new UUID(1, 2));
        IronBackpacksAPI.applyPackInfo(stack, info);
        BackpackInfo read = BackpackInfo.fromStack(stack.copy());
        check(helper, read.getInventory().getStackInSlot(35).is(Items.DIAMOND) && read.getInventory().getStackInSlot(35).getCount() == 7, "items");
        check(helper, read.hasUpgrade(upgrade("damage_bar")) && read.getRGBColor() == 0x123456 && new UUID(1, 2).equals(read.getOwner()), "info");
        check(helper, read.getVariant().equals(new BackpackVariant(IronBackpacksAPI.getBackpackType(RegistrarIronBackpacks.id("iron")), BackpackSpecialty.STORAGE)), "variant");
        helper.succeed();
    }

    /** Iron storage pack: ICI / IBI / III around a basic pack; owner, upgrades and items carry over, the colour doesn't (1.12 upgradeTo). */
    private static void tierRecipe(GameTestHelper helper) {
        ItemStack basic = pack("basic", BackpackSpecialty.NONE);
        BackpackInfo info = BackpackInfo.fromStack(basic);
        info.getInventory().setStackInSlot(0, new ItemStack(Items.APPLE, 5));
        info.addUpgrade(upgrade("lock")).setRGBColor(0xFF0000).setOwner(new UUID(3, 4));
        IronBackpacksAPI.applyPackInfo(basic, info);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack chest = new ItemStack(Items.CHEST);
        ItemStack result = craft(helper, 3, 3, iron, chest, iron, iron, basic, iron, iron, iron, iron);
        BackpackInfo out = BackpackInfo.fromStack(result);
        check(helper, out.getVariant().getBackpackType().getIdentifier().getPath().equals("iron") && out.getVariant().getBackpackSpecialty() == BackpackSpecialty.STORAGE,
                "result " + out.getVariant());
        check(helper, out.getInventory().getSlots() == 36 && out.getInventory().getStackInSlot(0).is(Items.APPLE), "items kept");
        check(helper, out.hasUpgrade(upgrade("lock")) && new UUID(3, 4).equals(out.getOwner()) && !out.getIsColored(), "owner/upgrades kept, colour lost");
        // an upgrade instead of the chest makes the upgrade emphasis
        ItemStack upgraded = craft(helper, 3, 3, iron, new ItemStack(RegistrarIronBackpacks.UPGRADE.get()), iron, iron, basic, iron, iron, iron, iron);
        check(helper, BackpackInfo.fromStack(upgraded).getVariant().getBackpackSpecialty() == BackpackSpecialty.UPGRADE, "upgrade emphasis");
        // gold needs the same emphasis below it
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        check(helper, craft(helper, 3, 3, gold, chest, gold, gold, upgraded, gold, gold, gold, gold).isEmpty(), "gold storage from iron upgrade");
        helper.succeed();
    }

    private static void basicRecipe(GameTestHelper helper) {
        ItemStack wool = new ItemStack(Items.BLUE_WOOL);
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack result = craft(helper, 3, 3, wool, leather, wool, leather, new ItemStack(Items.CHEST), leather, wool, leather, wool);
        BackpackInfo info = BackpackInfo.fromStack(result);
        check(helper, result.is(RegistrarIronBackpacks.BACKPACK.get()) && info.getVariant().getBackpackType().getIdentifier().getPath().equals("basic"), "basic " + result);
        check(helper, info.getInventory().getSlots() == 18, "18 slots");
        helper.succeed();
    }

    /** Backpack + dye colours it (dye RGB), + water bucket washes it; every tier. */
    private static void colorRecipes(GameTestHelper helper) {
        ItemStack basic = pack("basic", BackpackSpecialty.NONE);
        BackpackInfo info = BackpackInfo.fromStack(basic);
        info.getInventory().setStackInSlot(3, new ItemStack(Items.STONE, 9));
        IronBackpacksAPI.applyPackInfo(basic, info);
        ItemStack red = craft(helper, 2, 1, basic, new ItemStack(Items.RED_DYE));
        check(helper, BackpackInfo.getColor(red) == (DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF), "red " + BackpackInfo.getColor(red));
        check(helper, BackpackInfo.fromStack(red).getInventory().getStackInSlot(3).getCount() == 9, "items kept");
        ItemStack washed = craft(helper, 2, 1, red, new ItemStack(Items.WATER_BUCKET));
        check(helper, !washed.isEmpty() && BackpackInfo.getColor(washed) == -1, "washed");
        check(helper, !craft(helper, 2, 1, pack("iron", BackpackSpecialty.UPGRADE), new ItemStack(Items.BLUE_DYE)).isEmpty(), "iron can be dyed");
        // 1.12.2 couldn't dye gold / diamond backpacks (PACK_BASIC typo); the port fixes it
        ItemStack gold = craft(helper, 2, 1, pack("gold", BackpackSpecialty.STORAGE), new ItemStack(Items.BLUE_DYE));
        check(helper, BackpackInfo.getColor(gold) == (DyeColor.BLUE.getTextureDiffuseColor() & 0xFFFFFF)
                && BackpackInfo.fromStack(gold).getVariant().getBackpackType().getIdentifier().getPath().equals("gold"), "gold dyed");
        ItemStack diamond = craft(helper, 2, 1, pack("diamond", BackpackSpecialty.UPGRADE), new ItemStack(Items.GREEN_DYE));
        check(helper, BackpackInfo.getColor(diamond) == (DyeColor.GREEN.getTextureDiffuseColor() & 0xFFFFFF)
                && BackpackInfo.fromStack(diamond).getVariant().getBackpackSpecialty() == BackpackSpecialty.UPGRADE, "diamond dyed");
        helper.succeed();
    }

    /** Blank: string / paper / stick; damage bar: bowls; latch: gold; extra point: leather; everlasting has no recipe. */
    private static void upgradeRecipes(GameTestHelper helper) {
        ItemStack s = new ItemStack(Items.STRING), p = new ItemStack(Items.PAPER);
        ItemStack blank = craft(helper, 3, 3, s, p, s, p, new ItemStack(Items.STICK), p, s, p, s);
        check(helper, blank.is(RegistrarIronBackpacks.UPGRADE.get()) && !blank.has(RegistrarIronBackpacks.UPGRADE_ID.get()), "blank " + blank);
        ItemStack[][] cases = {{new ItemStack(Items.BOWL)}, {new ItemStack(Items.GOLD_INGOT)}, {new ItemStack(Items.LEATHER)}};
        String[] names = {"damage_bar", "lock", "extra_upgrade"};
        for (int i = 0; i < names.length; i++) {
            ItemStack m = cases[i][0];
            ItemStack result = craft(helper, 3, 3, m, s, m, s, blank, s, m, s, m);
            check(helper, RegistrarIronBackpacks.id(names[i]).equals(result.get(RegistrarIronBackpacks.UPGRADE_ID.get())), names[i] + " " + result);
        }
        boolean everlastingRecipe = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().anyMatch(r ->
                RegistrarIronBackpacks.id("everlasting").equals(r.value().getResultItem(helper.getLevel().registryAccess()).get(RegistrarIronBackpacks.UPGRADE_ID.get())));
        check(helper, !everlastingRecipe, "everlasting has no recipe in 1.12.2");
        helper.succeed();
    }

    /** Points, minimum tier, duplicates; extra point costs -1; shears take the last upgrade. */
    private static void anvilRules(GameTestHelper helper) {
        ItemStack basic = pack("basic", BackpackSpecialty.NONE);
        ItemStack everlasting = IronBackpacksAPI.getStack(upgrade("everlasting"));
        check(helper, EventHandler.tryUpgradeBackpack(basic, everlasting).isEmpty(), "everlasting needs tier 2");
        ItemStack iron = pack("iron", BackpackSpecialty.STORAGE);   // 7 points
        ItemStack withEverlasting = EventHandler.tryUpgradeBackpack(iron, everlasting);
        check(helper, BackpackInfo.fromStack(withEverlasting).getPointsUsed() == 4, "4 points");
        check(helper, EventHandler.tryUpgradeBackpack(withEverlasting, everlasting).isEmpty(), "no duplicates");
        ItemStack bar = EventHandler.tryUpgradeBackpack(withEverlasting, IronBackpacksAPI.getStack(upgrade("damage_bar")));
        ItemStack lock = EventHandler.tryUpgradeBackpack(bar, IronBackpacksAPI.getStack(upgrade("lock")));
        check(helper, BackpackInfo.fromStack(lock).getPointsUsed() == 6, "6 of 7 points");
        ItemStack extra = EventHandler.tryUpgradeBackpack(lock, IronBackpacksAPI.getStack(upgrade("extra_upgrade")));
        check(helper, !extra.isEmpty() && BackpackInfo.fromStack(extra).getPointsUsed() == 5, "extra point: " + BackpackInfo.fromStack(extra).getPointsUsed());
        check(helper, RegistrarIronBackpacks.id("extra_upgrade").equals(EventHandler.tryRemoveUpgrade(extra).get(RegistrarIronBackpacks.UPGRADE_ID.get())),
                "shears take the last one");
        check(helper, EventHandler.tryUpgradeBackpack(basic, new ItemStack(RegistrarIronBackpacks.UPGRADE.get())).isEmpty(), "blank upgrade");
        helper.succeed();
    }

    /** The real anvil: upgrade in, 1 level; shears out, backpack and shears come back, anvil doesn't break. */
    private static void anvilMenu(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.experienceLevel = 10;
        AnvilMenu anvil = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
        anvil.getSlot(0).set(pack("iron", BackpackSpecialty.STORAGE));
        anvil.getSlot(1).set(IronBackpacksAPI.getStack(upgrade("damage_bar")));
        anvil.createResult();
        ItemStack out = anvil.getSlot(2).getItem();
        check(helper, BackpackInfo.fromStack(out).hasUpgrade(upgrade("damage_bar")) && anvil.getCost() == 1, "upgrade in, cost " + anvil.getCost());
        anvil.getSlot(2).onTake(player, out);
        check(helper, anvil.getSlot(1).getItem().isEmpty() && player.experienceLevel == 9, "upgrade used");

        anvil.getSlot(0).set(out);
        anvil.getSlot(1).set(new ItemStack(Items.SHEARS));
        anvil.createResult();
        ItemStack removed = anvil.getSlot(2).getItem();
        check(helper, upgrade("damage_bar").getIdentifier().equals(removed.get(RegistrarIronBackpacks.UPGRADE_ID.get())), "removed " + removed);
        anvil.getSlot(2).onTake(player, removed);
        boolean packBack = false, shearsBack = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ItemBackpack && BackpackInfo.fromStack(stack).getUpgrades().isEmpty()) packBack = true;
            if (stack.is(Items.SHEARS)) shearsBack = true;
        }
        check(helper, packBack && shearsBack, "backpack and shears returned");
        player.discard();
        helper.succeed();
    }

    /** The first user becomes the owner; with a latch, others can't open it. */
    private static void ownerAndLock(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer other = player(helper);
        ItemStack stack = EventHandler.tryUpgradeBackpack(pack("basic", BackpackSpecialty.NONE), IronBackpacksAPI.getStack(upgrade("lock")));
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        check(helper, owner.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), owner, InteractionHand.MAIN_HAND).getResult().consumesAction(), "owner opens");
        check(helper, owner.getUUID().equals(BackpackInfo.fromStack(owner.getItemInHand(InteractionHand.MAIN_HAND)).getOwner()), "owner set");
        check(helper, owner.containerMenu instanceof ContainerBackpack, "menu open");
        owner.closeContainer();
        ItemStack moved = owner.getItemInHand(InteractionHand.MAIN_HAND).copy();
        other.setItemInHand(InteractionHand.MAIN_HAND, moved);
        check(helper, !moved.use(helper.getLevel(), other, InteractionHand.MAIN_HAND).getResult().consumesAction(), "latched");
        check(helper, !(other.containerMenu instanceof ContainerBackpack), "no menu for others");
        owner.discard();
        other.discard();
        helper.succeed();
    }

    /** Half full by count: bar 13 * 0.5, colour hue 0.5 / 3. */
    private static void damageBar(GameTestHelper helper) {
        ItemStack stack = EventHandler.tryUpgradeBackpack(pack("basic", BackpackSpecialty.NONE), IronBackpacksAPI.getStack(upgrade("damage_bar")));
        ItemBackpack item = RegistrarIronBackpacks.BACKPACK.get();
        check(helper, item.isBarVisible(stack) && item.getBarWidth(stack) == 0, "empty: width " + item.getBarWidth(stack));
        BackpackInfo info = BackpackInfo.fromStack(stack);
        for (int i = 0; i < 9; i++) info.getInventory().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        IronBackpacksAPI.applyPackInfo(stack, info);
        check(helper, Math.abs(item.getDurabilityForDisplay(stack) - 0.5) < 1e-9 && item.getBarWidth(stack) == 7, "half: width " + item.getBarWidth(stack));
        check(helper, item.getBarColor(stack) == net.minecraft.util.Mth.hsvToRgb(0.5F / 3.0F, 1.0F, 1.0F), "colour");
        check(helper, !item.isBarVisible(pack("basic", BackpackSpecialty.NONE)), "no bar without the upgrade");
        helper.succeed();
    }

    /** A dropped everlasting backpack outlives the 6000 tick item lifetime; a plain one despawns. */
    private static void everlasting(GameTestHelper helper) {
        ItemStack stack = EventHandler.tryUpgradeBackpack(pack("iron", BackpackSpecialty.UPGRADE), IronBackpacksAPI.getStack(upgrade("everlasting")));
        ItemEntity kept = expire(helper, stack);
        ItemEntity plain = expire(helper, pack("iron", BackpackSpecialty.UPGRADE));
        check(helper, kept.isAlive() && plain.isRemoved(), "kept " + kept.isAlive() + ", plain removed " + plain.isRemoved());
        kept.discard();
        helper.succeed();
    }

    /** An item entity one tick before it expires, ticked once. */
    private static ItemEntity expire(GameTestHelper helper, ItemStack stack) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1)).getCenter();
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack);
        helper.getLevel().addFreshEntity(entity);
        // through the saved "Age" (field names differ in a release jar)
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        entity.saveWithoutId(tag);
        tag.putShort("Age", (short) 5999);
        entity.load(tag);
        entity.tick();
        return entity;
    }

    /** Slot layout of copygirl's GUI: 11x7 diamond storage pack is wider than the player inventory. */
    private static void menuLayout(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, pack("diamond", BackpackSpecialty.STORAGE));
        ContainerBackpack.open(player, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND);
        ContainerBackpack menu = (ContainerBackpack) player.containerMenu;
        check(helper, menu.slots.size() == 77 + 36, "slots " + menu.slots.size());
        check(helper, menu.getWidth() == 11 * 18 + 14 && menu.getHeight() == 17 + 7 * 18 + 13 + 72 + 4 + 7, "size " + menu.getWidth() + "x" + menu.getHeight());
        check(helper, menu.getSlot(0).x == 8 && menu.getSlot(0).y == 18 && menu.getSlot(77).x == 8 + 18 && menu.getSlot(77).y == 18 + 126 + 13, "positions "
                + menu.getSlot(0).x + "," + menu.getSlot(0).y + " " + menu.getSlot(77).x + "," + menu.getSlot(77).y);
        player.closeContainer();
        player.discard();
        helper.succeed();
    }

    /** The open backpack's slot can't be clicked, number-key swapped or shift-moved; backpacks can't go in. */
    private static void menuBlocks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().selected = 2;
        player.setItemInHand(InteractionHand.MAIN_HAND, pack("basic", BackpackSpecialty.NONE));
        player.getInventory().setItem(0, pack("iron", BackpackSpecialty.STORAGE));
        ContainerBackpack.open(player, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND);
        ContainerBackpack menu = (ContainerBackpack) player.containerMenu;
        int heldSlot = 18 + 27 + 2;
        menu.clicked(heldSlot, 0, ClickType.PICKUP, player);
        check(helper, menu.getCarried().isEmpty() && player.getInventory().getItem(2).getItem() instanceof ItemBackpack, "held backpack can't be picked up");
        menu.clicked(0, 2, ClickType.SWAP, player);
        check(helper, menu.getSlot(0).getItem().isEmpty(), "no swap with the held backpack");
        menu.clicked(18 + 27, 0, ClickType.QUICK_MOVE, player);
        check(helper, menu.getSlot(0).getItem().isEmpty(), "other backpacks can't be shift-moved in");
        menu.clicked(18 + 27, 0, ClickType.PICKUP, player);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(helper, menu.getSlot(0).getItem().isEmpty() && menu.getCarried().getItem() instanceof ItemBackpack, "backpacks can't be put in");
        player.closeContainer();
        player.discard();
        helper.succeed();
    }

    private static void menuSaves(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, pack("basic", BackpackSpecialty.NONE));
        player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 12));
        ContainerBackpack.open(player, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND);
        ContainerBackpack menu = (ContainerBackpack) player.containerMenu;
        menu.clicked(18, 0, ClickType.QUICK_MOVE, player);   // first main inventory slot -> backpack
        check(helper, menu.getSlot(0).getItem().is(Items.EMERALD), "moved in");
        player.closeContainer();
        BackpackInfo info = BackpackInfo.fromStack(player.getItemInHand(InteractionHand.MAIN_HAND));
        check(helper, info.getInventory().getStackInSlot(0).getCount() == 12 && player.getInventory().getItem(9).isEmpty(), "saved on close");
        player.discard();
        helper.succeed();
    }

    private static void blacklist(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        InventoryBlacklist.INSTANCE.blacklist(new ItemStack(Items.TNT));
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, pack("basic", BackpackSpecialty.NONE));
            player.getInventory().setItem(9, new ItemStack(Items.TNT, 4));
            ContainerBackpack.open(player, ContainerBackpack.Mode.HELD, InteractionHand.MAIN_HAND);
            ContainerBackpack menu = (ContainerBackpack) player.containerMenu;
            menu.clicked(18, 0, ClickType.QUICK_MOVE, player);
            check(helper, menu.getSlot(0).getItem().isEmpty(), "blacklisted item stays out");
            player.closeContainer();
        } finally {
            InventoryBlacklist.initBlacklist();
        }
        player.discard();
        helper.succeed();
    }

    /** The open key: the offhand, then the hotbar / inventory; that stack is blocked by equality. */
    private static void openKey(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().setItem(20, pack("gold", BackpackSpecialty.UPGRADE));
        RequestAction.OPEN_BACKPACK.handle(player);
        check(helper, player.containerMenu instanceof ContainerBackpack c && c.getBackpackSize().getTotalSize() == 45, "gold upgrade pack opened");
        ContainerBackpack menu = (ContainerBackpack) player.containerMenu;
        menu.clicked(45 + 11, 0, ClickType.PICKUP, player);   // inventory slot 20 is menu slot 45 + 11
        check(helper, menu.getCarried().isEmpty(), "opened backpack blocked");
        player.closeContainer();
        player.discard();
        helper.succeed();
    }

    private static void names(GameTestHelper helper) {
        check(helper, pack("diamond", BackpackSpecialty.STORAGE).getDescriptionId().equals("item.ironbackpacks.backpack.ironbackpacks.diamond"), "pack key");
        check(helper, IronBackpacksAPI.getStack(upgrade("lock")).getDescriptionId().equals("upgrade.ironbackpacks.lock"), "upgrade key");
        check(helper, new ItemStack(RegistrarIronBackpacks.UPGRADE.get()).getDescriptionId().equals("item.ironbackpacks.upgrade"), "blank key");
        ResourceLocation unknown = RegistrarIronBackpacks.id("nope");
        check(helper, IronBackpacksAPI.getUpgrade(unknown).isNull() && IronBackpacksAPI.getBackpackType(unknown).isNull(), "unknown ids are the null entries");
        helper.succeed();
    }

    /** Public: Fabric creates the fabric-gametest entrypoint. */
    public IBGameTests() {}
}
