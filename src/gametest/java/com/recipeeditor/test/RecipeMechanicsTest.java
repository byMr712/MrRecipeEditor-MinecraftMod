package com.recipeeditor.test;

import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.inspector.RecipeConflictInfo;
import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Comprehensive mechanics test suite for [MR] Recipe Editor data models,
 * config storage, bounding boxes, signatures, workstation types, and override dispatch.
 */
public class RecipeMechanicsTest implements FabricClientGameTest {

    private int checkCount = 0;

    private void check(boolean condition, String message) {
        checkCount++;
        if (!condition) {
            throw new AssertionError("Check #" + checkCount + " failed: " + message);
        }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            testRecipeDataDefaults();
            testBoundingBoxCalculations();
            testCompositeKeysAndSignatures();
            testRecipeTypeEnumBidirectional();
            testConfigLifecycleAndSorting();
            testNetworkRecipeIdPool();
            testOverridesMechanism();
            testCategoryHeuristics();
            testConflictInfoModel();
        });

        System.out.println("[RecipeMechanicsTest] PASSED with " + checkCount + " successful assertions!");
    }

    private void testRecipeDataDefaults() {
        CustomRecipeData data = new CustomRecipeData();
        check(data.patternSlots != null && data.patternSlots.length == 9, "Pattern slots must initialize to 9 slots");
        for (int i = 0; i < 9; i++) {
            check("minecraft:air".equals(data.patternSlots[i]), "Slot " + i + " must default to minecraft:air");
        }
        check(data.enabled, "Recipe must be enabled by default");
        check(!data.isShapeless, "Recipe must default to shaped crafting");
        check(data.type == RecipeTypeEnum.SHAPED_CRAFTING, "Default type must be SHAPED_CRAFTING");
        check(data.cookingTime == 200, "Default cooking time must be 200 ticks");
    }

    private void testBoundingBoxCalculations() {
        // 1. Empty pattern
        CustomRecipeData empty = new CustomRecipeData();
        empty.computePatternBounds();
        check(empty.getPatternWidth() == 0 && empty.getPatternHeight() == 0, "Empty pattern bounds must be 0x0");

        // 2. 1x1 recipe in top-left (slot 0)
        CustomRecipeData singleTopLeft = new CustomRecipeData();
        singleTopLeft.patternSlots[0] = "minecraft:iron_ingot";
        singleTopLeft.computePatternBounds();
        check(singleTopLeft.getPatternWidth() == 1 && singleTopLeft.getPatternHeight() == 1, "Single slot 0 bounds must be 1x1");
        check(singleTopLeft.getMinCol() == 0 && singleTopLeft.getMinRow() == 0, "Single slot 0 min coords must be (0,0)");

        // 3. 1x1 recipe in bottom-right (slot 8)
        CustomRecipeData singleBottomRight = new CustomRecipeData();
        singleBottomRight.patternSlots[8] = "minecraft:iron_ingot";
        singleBottomRight.computePatternBounds();
        check(singleBottomRight.getPatternWidth() == 1 && singleBottomRight.getPatternHeight() == 1, "Single slot 8 bounds must be 1x1");
        check(singleBottomRight.getMinCol() == 2 && singleBottomRight.getMinRow() == 2, "Single slot 8 coords must be (2,2)");

        // 4. 2x2 pattern in bottom-right (slots 4, 5, 7, 8)
        CustomRecipeData twoByTwo = new CustomRecipeData();
        twoByTwo.patternSlots[4] = "minecraft:iron_ingot";
        twoByTwo.patternSlots[5] = "minecraft:iron_ingot";
        twoByTwo.patternSlots[7] = "minecraft:iron_ingot";
        twoByTwo.patternSlots[8] = "minecraft:iron_ingot";
        twoByTwo.computePatternBounds();
        check(twoByTwo.getPatternWidth() == 2 && twoByTwo.getPatternHeight() == 2, "2x2 bounds must be 2x2");
        check(twoByTwo.getMinCol() == 1 && twoByTwo.getMinRow() == 1, "2x2 bottom-right min coords must be (1,1)");

        // 5. Vertical column (slots 1, 4, 7) -> 1x3
        CustomRecipeData col = new CustomRecipeData();
        col.patternSlots[1] = "minecraft:stick";
        col.patternSlots[4] = "minecraft:stick";
        col.patternSlots[7] = "minecraft:stick";
        col.computePatternBounds();
        check(col.getPatternWidth() == 1 && col.getPatternHeight() == 3, "Vertical stick bounds must be 1x3");

        // 6. Horizontal row (slots 3, 4, 5) -> 3x1
        CustomRecipeData row = new CustomRecipeData();
        row.patternSlots[3] = "minecraft:iron_ingot";
        row.patternSlots[4] = "minecraft:iron_ingot";
        row.patternSlots[5] = "minecraft:iron_ingot";
        row.computePatternBounds();
        check(row.getPatternWidth() == 3 && row.getPatternHeight() == 1, "Horizontal row bounds must be 3x1");

        // 7. Cache invalidation check
        row.patternSlots[0] = "minecraft:diamond";
        row.invalidateCache();
        row.computePatternBounds();
        check(row.getPatternWidth() == 3 && row.getPatternHeight() == 2, "After adding diamond, bounds must be 3x2");
    }

    private void testCompositeKeysAndSignatures() {
        CustomRecipeData r1 = new CustomRecipeData();
        r1.resultItemId = "minecraft:diamond_sword";
        r1.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r1.patternSlots[1] = "minecraft:diamond";
        r1.patternSlots[4] = "minecraft:diamond";
        r1.patternSlots[7] = "minecraft:stick";

        String sig1 = r1.getPatternSignature();
        check(sig1.contains("minecraft:diamond"), "Signature must include ingredients");
        check(sig1.split(",").length == 9, "Signature must contain 9 comma-separated entries");

        String key1 = r1.getKey();
        check(key1.startsWith("minecraft:diamond_sword#SHAPED_CRAFTING#"), "Key must start with item and type");
        check(!key1.contains("#SL#"), "Shaped recipe key must not contain shapeless marker #SL#");

        r1.isShapeless = true;
        String keyShapeless = r1.getKey();
        check(keyShapeless.contains("#SL#"), "Shapeless recipe key must contain #SL# marker");
        check(!key1.equals(keyShapeless), "Shaped and shapeless keys for same item must differ");
    }

    private void testRecipeTypeEnumBidirectional() {
        for (RecipeTypeEnum type : RecipeTypeEnum.values()) {
            check(type.getDisplayName() != null, "Display name missing for " + type);
            check(type.getTooltip() != null, "Tooltip missing for " + type);

            RecipeType<?> vanillaType = type.toRecipeType();
            check(vanillaType != null, "toRecipeType must return valid RecipeType for " + type);

            RecipeTypeEnum roundTrip = RecipeTypeEnum.fromRecipeType(vanillaType);
            check(roundTrip == type, "Round trip mapping failed for " + type + " -> " + roundTrip);
        }
    }

    private void testConfigLifecycleAndSorting() {
        RecipeEditorConfig config = new RecipeEditorConfig();
        check(config.configVersion == 0, "Initial configVersion must be 0");
        check(config.recipes.isEmpty(), "Initial recipes must be empty");

        // 1. Add shaped recipe
        CustomRecipeData shaped = new CustomRecipeData();
        shaped.resultItemId = "minecraft:gold_block";
        shaped.type = RecipeTypeEnum.SHAPED_CRAFTING;
        for (int i = 0; i < 9; i++) shaped.patternSlots[i] = "minecraft:gold_ingot";
        config.addOrUpdateRecipe(shaped);

        check(config.configVersion == 1, "configVersion must increment after adding recipe");
        check(config.hasCustomRecipe("minecraft:gold_block"), "Fast check hasCustomRecipe must return true");
        check(!config.hasCustomRecipe("minecraft:diamond"), "Fast check for unadded item must return false");

        // 2. Add shapeless recipe with 2 ingredients
        CustomRecipeData sl2 = new CustomRecipeData();
        sl2.resultItemId = "minecraft:emerald";
        sl2.type = RecipeTypeEnum.SHAPED_CRAFTING;
        sl2.isShapeless = true;
        sl2.patternSlots[0] = "minecraft:dirt";
        sl2.patternSlots[1] = "minecraft:sand";
        config.addOrUpdateRecipe(sl2);

        // 3. Add shapeless recipe with 4 ingredients
        CustomRecipeData sl4 = new CustomRecipeData();
        sl4.resultItemId = "minecraft:cake";
        sl4.type = RecipeTypeEnum.SHAPED_CRAFTING;
        sl4.isShapeless = true;
        sl4.patternSlots[0] = "minecraft:wheat";
        sl4.patternSlots[1] = "minecraft:sugar";
        sl4.patternSlots[2] = "minecraft:egg";
        sl4.patternSlots[3] = "minecraft:milk_bucket";
        config.addOrUpdateRecipe(sl4);

        // Verify deterministic sorting: Shaped first, then shapeless with more ingredients
        List<CustomRecipeData> sorted = config.getSortedCraftingRecipes();
        check(sorted.size() == 3, "Sorted recipes size must match added count");
        check(!sorted.get(0).isShapeless, "First recipe in sorted list must be shaped");
        check(sorted.get(1).isShapeless && sorted.get(1).getNonEmptyIngredients().size() == 4,
                "Second recipe must be shapeless with 4 ingredients (more ingredients prioritized)");
        check(sorted.get(2).isShapeless && sorted.get(2).getNonEmptyIngredients().size() == 2,
                "Third recipe must be shapeless with 2 ingredients");

        // 4. Remove recipe
        config.removeRecipe(shaped);
        check(config.configVersion > 1, "configVersion must increment after removal");
        check(!config.hasCustomRecipe("minecraft:gold_block"), "Removed recipe should not be found in fast check");
    }

    private void testNetworkRecipeIdPool() {
        for (int i = 0; i < 50; i++) {
            String key = "test_recipe_" + i + "#SHAPED_CRAFTING#minecraft:iron_ingot";
            int netId = CustomRecipeDispatcher.getBaseNetworkId(key);
            check(netId >= 1_000_000, "Network ID must be >= 1,000,000 to avoid vanilla collision (got " + netId + ")");
            check(netId < 900_000_000, "Network ID must be within safe bounded pool (got " + netId + ")");

            int netId2 = CustomRecipeDispatcher.getBaseNetworkId(key);
            check(netId == netId2, "Network ID must be deterministic for identical key");
        }
    }

    private void testOverridesMechanism() {
        RecipeEditorConfig config = RecipeEditorConfig.getInstance();
        boolean origEnabled = config.modEnabled;
        config.modEnabled = true;

        CustomRecipeData override = new CustomRecipeData();
        override.resultItemId = "minecraft:diamond_sword";
        override.type = RecipeTypeEnum.SHAPED_CRAFTING;
        override.enabled = true;
        override.overrideExisting = true;
        override.overriddenId = "minecraft:diamond_sword";
        config.addOrUpdateRecipe(override);

        check(CustomRecipeDispatcher.isIdOverridden("minecraft:diamond_sword"),
                "isIdOverridden should return true for exact ID");
        check(CustomRecipeDispatcher.isIdOverridden("diamond_sword"),
                "isIdOverridden should return true for short path without namespace");
        check(!CustomRecipeDispatcher.isIdOverridden("minecraft:iron_sword"),
                "isIdOverridden should return false for unoverridden item");

        config.removeRecipe(override);
        config.modEnabled = origEnabled;
    }

    private void testCategoryHeuristics() {
        check(CustomRecipeDispatcher.getCraftingCategory(Items.DIAMOND_SWORD) == CraftingBookCategory.EQUIPMENT,
                "Sword should be classified as EQUIPMENT");
        check(CustomRecipeDispatcher.getCraftingCategory(Items.SHIELD) == CraftingBookCategory.EQUIPMENT,
                "Shield should be classified as EQUIPMENT");
        check(CustomRecipeDispatcher.getCraftingCategory(Items.REDSTONE_BLOCK) == CraftingBookCategory.REDSTONE,
                "Redstone Block should be classified as REDSTONE");
        check(CustomRecipeDispatcher.getCraftingCategory(Items.HOPPER) == CraftingBookCategory.REDSTONE,
                "Hopper should be classified as REDSTONE");
        check(CustomRecipeDispatcher.getCraftingCategory(Items.STONE) == CraftingBookCategory.BUILDING,
                "Stone block should be classified as BUILDING");
        check(CustomRecipeDispatcher.getCraftingCategory(Items.STICK) == CraftingBookCategory.MISC,
                "Stick should be classified as MISC");
    }

    private void testConflictInfoModel() {
        RecipeConflictInfo info = new RecipeConflictInfo(
                null,
                Items.IRON_SWORD,
                "vanilla",
                RecipeTypeEnum.SHAPED_CRAFTING,
                "minecraft:iron_sword",
                null
        );
        check(info.conflictingItem == Items.IRON_SWORD, "Conflicting item matches");
        check(info.workstationType == RecipeTypeEnum.SHAPED_CRAFTING, "Type matches SHAPED_CRAFTING");
        check("vanilla".equals(info.sourceName), "Source name matches");
    }
}
