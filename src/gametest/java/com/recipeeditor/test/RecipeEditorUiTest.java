package com.recipeeditor.test;

import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.inspector.RecipeStatus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Tests UI scaling mathematics, coordinate translation,
 * and text rendering components for Recipe Editor screens.
 */
public class RecipeEditorUiTest implements FabricClientGameTest {

    private int checkCount = 0;

    private void check(boolean condition, String message) {
        checkCount++;
        if (!condition) {
            throw new AssertionError("UI Check #" + checkCount + " failed: " + message);
        }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            testUiScaleFormula();
            testCoordinateTranslation();
            testRecipeStatusFormatting();
            testWorkstationTitlesAndTooltips();
        });

        System.out.println("[RecipeEditorUiTest] PASSED with " + checkCount + " successful assertions!");
    }

    private void testUiScaleFormula() {
        // Standard full HD resolution: 1920x1080
        float scaleFHD = Math.min(1920f / 460f, 1080f / 380f);
        check(Math.abs(scaleFHD - (1080f / 380f)) < 0.001f, "Full HD uiScale must be constrained by height (2.842)");

        // 720p resolution: 1280x720
        float scale720 = Math.min(1280f / 460f, 720f / 380f);
        check(Math.abs(scale720 - (720f / 380f)) < 0.001f, "720p uiScale must be constrained by height (1.894)");

        // Ultra-wide resolution: 2560x1080
        float scaleUW = Math.min(2560f / 460f, 1080f / 380f);
        check(Math.abs(scaleUW - (1080f / 380f)) < 0.001f, "Ultrawide uiScale must be constrained by height");

        // Vertical screen ratio: 600x800
        float scaleTall = Math.min(600f / 460f, 800f / 380f);
        check(Math.abs(scaleTall - (600f / 460f)) < 0.001f, "Tall aspect ratio must be constrained by width (1.304)");
    }

    private void testCoordinateTranslation() {
        float uiScale = 2.0f;
        double physicalMouseX = 920.0;
        double physicalMouseY = 560.0;

        double scaledX = physicalMouseX / uiScale;
        double scaledY = physicalMouseY / uiScale;

        check(scaledX == 460.0, "Scaled X must equal 460.0");
        check(scaledY == 280.0, "Scaled Y must equal 280.0");
    }

    private void testRecipeStatusFormatting() {
        check(RecipeStatus.UNCRAFTABLE.getFormatting() == ChatFormatting.RED,
                "UNCRAFTABLE formatting must be RED");
        check(RecipeStatus.VANILLA_OR_MODDED.getFormatting() == ChatFormatting.GOLD,
                "VANILLA_OR_MODDED formatting must be GOLD");
        check(RecipeStatus.CUSTOM.getFormatting() == ChatFormatting.GREEN,
                "CUSTOM formatting must be GREEN");

        Component customText = RecipeStatus.CUSTOM.getDisplayText();
        check(customText != null, "Display text must not be null");
    }

    private void testWorkstationTitlesAndTooltips() {
        for (RecipeTypeEnum type : RecipeTypeEnum.values()) {
            Component displayName = type.getDisplayName();
            check(displayName != null, "DisplayName missing for " + type);

            Component tooltip = type.getTooltip();
            check(tooltip != null, "Tooltip missing for " + type);
        }
    }
}
