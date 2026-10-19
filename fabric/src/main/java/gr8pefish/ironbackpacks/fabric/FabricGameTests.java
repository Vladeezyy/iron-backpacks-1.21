package gr8pefish.ironbackpacks.fabric;

import gr8pefish.ironbackpacks.gametest.IBGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The common game tests (gametest/IBGameTests) for Fabric's game test API, which finds tests by this annotation
 * (26.x; the fabric-gametest entrypoint). One method per test, each runs the common one of the same name.
 */
public class FabricGameTests {
    private static final String AREA = "ironbackpacks:gametest_area";

    @GameTest(structure = AREA) public void variants(GameTestHelper h) { IBGameTests.run("variants", h); }
    @GameTest(structure = AREA) public void stack_round_trip(GameTestHelper h) { IBGameTests.run("stack_round_trip", h); }
    @GameTest(structure = AREA) public void tier_recipe_keeps_contents(GameTestHelper h) { IBGameTests.run("tier_recipe_keeps_contents", h); }
    @GameTest(structure = AREA) public void basic_recipe(GameTestHelper h) { IBGameTests.run("basic_recipe", h); }
    @GameTest(structure = AREA) public void color_recipes(GameTestHelper h) { IBGameTests.run("color_recipes", h); }
    @GameTest(structure = AREA) public void upgrade_recipes(GameTestHelper h) { IBGameTests.run("upgrade_recipes", h); }
    @GameTest(structure = AREA) public void anvil_rules(GameTestHelper h) { IBGameTests.run("anvil_rules", h); }
    @GameTest(structure = AREA) public void anvil_menu(GameTestHelper h) { IBGameTests.run("anvil_menu", h); }
    @GameTest(structure = AREA) public void owner_and_lock(GameTestHelper h) { IBGameTests.run("owner_and_lock", h); }
    @GameTest(structure = AREA) public void damage_bar(GameTestHelper h) { IBGameTests.run("damage_bar", h); }
    @GameTest(structure = AREA) public void everlasting(GameTestHelper h) { IBGameTests.run("everlasting", h); }
    @GameTest(structure = AREA) public void menu_layout(GameTestHelper h) { IBGameTests.run("menu_layout", h); }
    @GameTest(structure = AREA) public void menu_blocks_open_backpack(GameTestHelper h) { IBGameTests.run("menu_blocks_open_backpack", h); }
    @GameTest(structure = AREA) public void menu_saves_on_close(GameTestHelper h) { IBGameTests.run("menu_saves_on_close", h); }
    @GameTest(structure = AREA) public void blacklist(GameTestHelper h) { IBGameTests.run("blacklist", h); }
    @GameTest(structure = AREA) public void open_key(GameTestHelper h) { IBGameTests.run("open_key", h); }
    @GameTest(structure = AREA) public void names(GameTestHelper h) { IBGameTests.run("names", h); }
    @GameTest(structure = AREA) public void no_dupe_dropped_while_open(GameTestHelper h) { IBGameTests.run("no_dupe_dropped_while_open", h); }
    @GameTest(structure = AREA) public void copy_while_open_keeps_contents(GameTestHelper h) { IBGameTests.run("copy_while_open_keeps_contents", h); }
    @GameTest(structure = AREA) public void open_key_respects_latch(GameTestHelper h) { IBGameTests.run("open_key_respects_latch", h); }
}
