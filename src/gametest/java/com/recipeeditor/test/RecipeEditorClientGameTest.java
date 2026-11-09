package com.recipeeditor.test;

import com.recipeeditor.client.gui.RecipeEditorScreen;
import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.config.RecipeTypeEnum;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * End-to-end Client GameTest exercising RecipeEditorScreen both on the title screen
 * (verifying component binding guard with no world loaded) and inside a live singleplayer world.
 */
public class RecipeEditorClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext ctx) {
        TestInput input = ctx.getInput();
        input.resizeWindow(1600, 900);

        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
        });
        ctx.waitTicks(20);

        // -------------------------------------------------------------
        // Phase 1: Title Screen / Main Menu safety check
        // -------------------------------------------------------------
        ctx.runOnClient(mc -> {
            mc.setScreen(new RecipeEditorScreen(mc.screen));
        });
        ctx.waitTicks(15);
        ctx.takeScreenshot("01_title_screen_recipe_editor");

        ctx.runOnClient(mc -> {
            if (!(mc.screen instanceof RecipeEditorScreen)) {
                throw new AssertionError("Expected RecipeEditorScreen on Title Screen, got: " + mc.screen);
            }
            mc.screen.onClose();
        });
        ctx.waitTicks(10);

        // -------------------------------------------------------------
        // Phase 2: In-Game Singleplayer Live Workflow
        // -------------------------------------------------------------
        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setAllowCommands(true))
                .create()) {

            ctx.waitTicks(40);
            sp.getServer().runCommand("time set day");
            sp.getServer().runCommand("difficulty peaceful");
            sp.getServer().runCommand("gamemode creative @a");
            ctx.waitTicks(40);

            // Open Recipe Editor inside the live game
            ctx.runOnClient(mc -> {
                mc.setScreen(new RecipeEditorScreen(null));
            });
            ctx.waitTicks(15);
            ctx.takeScreenshot("02_ingame_recipe_editor");

            // Verify live screen state and inject a custom test recipe
            CustomRecipeData testCraft = new CustomRecipeData();
            testCraft.resultItemId = "minecraft:diamond";
            testCraft.resultCount = 5;
            testCraft.type = RecipeTypeEnum.SHAPED_CRAFTING;
            testCraft.patternSlots[0] = "minecraft:dirt";
            testCraft.patternSlots[1] = "minecraft:dirt";

            RecipeEditorConfig.getInstance().addOrUpdateRecipe(testCraft);
            RecipeEditorConfig.getInstance().save();

            ctx.waitTicks(10);
            ctx.takeScreenshot("03_ingame_recipe_editor_custom_recipe");

            ctx.runOnClient(mc -> {
                if (!RecipeEditorConfig.getInstance().hasCustomRecipe("minecraft:diamond")) {
                    throw new AssertionError("Custom recipe for diamond was not registered in config!");
                }
                if (mc.screen != null) {
                    mc.screen.onClose();
                }
            });
            ctx.waitTicks(20);

            ctx.takeScreenshot("04_ingame_world_after_recipe_save");

            // Clean up custom recipe
            RecipeEditorConfig.getInstance().removeRecipe(testCraft);
            RecipeEditorConfig.getInstance().save();
            ctx.waitTicks(5);
        }

        System.out.println("[RecipeEditorClientGameTest] Complete client gametest workflow PASSED!");
    }
}
