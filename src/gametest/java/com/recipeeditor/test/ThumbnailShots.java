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
 * Stages clean high-resolution presentation screenshots of the Recipe Editor GUI
 * with various workstation recipes (Crafting Table, Furnace, Smithing Table).
 */
public class ThumbnailShots implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext ctx) {
        TestInput input = ctx.getInput();
        input.resizeWindow(1920, 1080);

        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(4);
            mc.resizeGui();
        });
        ctx.waitTicks(20);

        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setAllowCommands(true))
                .create()) {

            sp.getConnection().waitForChunksRender();
            sp.getServer().runCommand("time set day");
            sp.getServer().runCommand("difficulty peaceful");
            ctx.waitTicks(30);

            // 1. Crafting studio showcase
            CustomRecipeData craft = new CustomRecipeData();
            craft.resultItemId = "minecraft:netherite_sword";
            craft.resultCount = 1;
            craft.type = RecipeTypeEnum.SHAPED_CRAFTING;
            craft.patternSlots[1] = "minecraft:netherite_ingot";
            craft.patternSlots[4] = "minecraft:netherite_ingot";
            craft.patternSlots[7] = "minecraft:stick";
            RecipeEditorConfig.getInstance().addOrUpdateRecipe(craft);

            ctx.runOnClient(mc -> mc.setScreenAndShow(new RecipeEditorScreen(null)));
            ctx.waitTicks(25);
            ctx.takeScreenshot("thumbnail_crafting_studio");

            // 2. Furnace cooking showcase
            CustomRecipeData smelt = new CustomRecipeData();
            smelt.resultItemId = "minecraft:diamond";
            smelt.resultCount = 2;
            smelt.type = RecipeTypeEnum.SMELTING;
            smelt.cookingTime = 160;
            smelt.experience = 2.5f;
            smelt.patternSlots[0] = "minecraft:coal_block";
            RecipeEditorConfig.getInstance().addOrUpdateRecipe(smelt);

            ctx.waitTicks(15);
            ctx.takeScreenshot("thumbnail_furnace_studio");

            // Close and clean up
            ctx.runOnClient(mc -> {
                if (mc.gui.screen() != null) mc.gui.screen().onClose();
            });
            RecipeEditorConfig.getInstance().removeRecipe(craft);
            RecipeEditorConfig.getInstance().removeRecipe(smelt);
            RecipeEditorConfig.getInstance().save();
            ctx.waitTicks(10);
        }

        System.out.println("[ThumbnailShots] Presentation screenshots saved successfully!");
    }
}
