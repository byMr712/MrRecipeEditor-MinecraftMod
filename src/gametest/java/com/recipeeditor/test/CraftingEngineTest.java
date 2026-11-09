package com.recipeeditor.test;

import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.recipe.CustomDynamicCraftingRecipe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rigorous tests for the dynamic crafting engine:
 * Sliding window pattern matching across all grid displacements, horizontal mirroring,
 * bipartite shapeless backtracking, and shift-click stack loss prevention.
 */
public class CraftingEngineTest implements FabricClientGameTest {

    private int checkCount = 0;

    private void check(boolean condition, String message) {
        checkCount++;
        if (!condition) {
            throw new AssertionError("Engine Check #" + checkCount + " failed: " + message);
        }
    }

    private CraftingInput createInput3x3(ItemStack... slots) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i < slots.length && slots[i] != null) {
                list.add(slots[i]);
            } else {
                list.add(ItemStack.EMPTY);
            }
        }
        return CraftingInput.of(3, 3, list);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            testSlidingWindowSingleItem();
            testSlidingWindowTwoByTwoDisplacements();
            testHorizontalMirroring();
            testStrayItemsOutsidePatternWindow();
            testShapelessBacktrackPermutations();
            testShapelessCountMismatch();
            testShiftClickStackClamping();
        });

        System.out.println("[CraftingEngineTest] PASSED with " + checkCount + " successful assertions!");
    }

    private void testSlidingWindowSingleItem() {
        CustomRecipeData r = new CustomRecipeData();
        r.resultItemId = "minecraft:gold_nugget";
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.isShapeless = false;
        r.patternSlots[0] = "minecraft:gold_ingot";

        for (int slot = 0; slot < 9; slot++) {
            ItemStack[] grid = new ItemStack[9];
            Arrays.fill(grid, ItemStack.EMPTY);
            grid[slot] = new ItemStack(Items.GOLD_INGOT);
            CraftingInput input = createInput3x3(grid);

            check(CustomDynamicCraftingRecipe.matchesShaped(input, r),
                    "1x1 shaped recipe should match at grid slot " + slot);
        }
    }

    private void testSlidingWindowTwoByTwoDisplacements() {
        CustomRecipeData r = new CustomRecipeData();
        r.resultItemId = "minecraft:stone_bricks";
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.isShapeless = false;
        r.patternSlots[0] = "minecraft:stone";
        r.patternSlots[1] = "minecraft:stone";
        r.patternSlots[3] = "minecraft:stone";
        r.patternSlots[4] = "minecraft:stone";

        // Corner 1: Top-Left (slots 0, 1, 3, 4)
        ItemStack[] tl = new ItemStack[9];
        Arrays.fill(tl, ItemStack.EMPTY);
        tl[0] = new ItemStack(Items.STONE);
        tl[1] = new ItemStack(Items.STONE);
        tl[3] = new ItemStack(Items.STONE);
        tl[4] = new ItemStack(Items.STONE);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(tl), r),
                "2x2 pattern should match in top-left corner");

        // Corner 2: Top-Right (slots 1, 2, 4, 5)
        ItemStack[] tr = new ItemStack[9];
        Arrays.fill(tr, ItemStack.EMPTY);
        tr[1] = new ItemStack(Items.STONE);
        tr[2] = new ItemStack(Items.STONE);
        tr[4] = new ItemStack(Items.STONE);
        tr[5] = new ItemStack(Items.STONE);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(tr), r),
                "2x2 pattern should match in top-right corner (dx=1, dy=0)");

        // Corner 3: Bottom-Left (slots 3, 4, 6, 7)
        ItemStack[] bl = new ItemStack[9];
        Arrays.fill(bl, ItemStack.EMPTY);
        bl[3] = new ItemStack(Items.STONE);
        bl[4] = new ItemStack(Items.STONE);
        bl[6] = new ItemStack(Items.STONE);
        bl[7] = new ItemStack(Items.STONE);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(bl), r),
                "2x2 pattern should match in bottom-left corner (dx=0, dy=1)");

        // Corner 4: Bottom-Right (slots 4, 5, 7, 8)
        ItemStack[] br = new ItemStack[9];
        Arrays.fill(br, ItemStack.EMPTY);
        br[4] = new ItemStack(Items.STONE);
        br[5] = new ItemStack(Items.STONE);
        br[7] = new ItemStack(Items.STONE);
        br[8] = new ItemStack(Items.STONE);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(br), r),
                "2x2 pattern should match in bottom-right corner (dx=1, dy=1)");
    }

    private void testHorizontalMirroring() {
        CustomRecipeData axe = new CustomRecipeData();
        axe.resultItemId = "minecraft:iron_axe";
        axe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        axe.isShapeless = false;
        axe.patternSlots[0] = "minecraft:iron_ingot";
        axe.patternSlots[1] = "minecraft:iron_ingot";
        axe.patternSlots[3] = "minecraft:iron_ingot";
        axe.patternSlots[4] = "minecraft:stick";
        axe.patternSlots[7] = "minecraft:stick";

        // 1. Direct orientation
        ItemStack[] direct = new ItemStack[9];
        Arrays.fill(direct, ItemStack.EMPTY);
        direct[0] = new ItemStack(Items.IRON_INGOT);
        direct[1] = new ItemStack(Items.IRON_INGOT);
        direct[3] = new ItemStack(Items.IRON_INGOT);
        direct[4] = new ItemStack(Items.STICK);
        direct[7] = new ItemStack(Items.STICK);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(direct), axe),
                "Axe pattern should match in direct orientation");

        // 2. Horizontally mirrored orientation
        ItemStack[] mirrored = new ItemStack[9];
        Arrays.fill(mirrored, ItemStack.EMPTY);
        mirrored[1] = new ItemStack(Items.IRON_INGOT);
        mirrored[2] = new ItemStack(Items.IRON_INGOT);
        mirrored[4] = new ItemStack(Items.STICK);
        mirrored[5] = new ItemStack(Items.IRON_INGOT);
        mirrored[7] = new ItemStack(Items.STICK);
        check(CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(mirrored), axe),
                "Axe pattern should match when horizontally mirrored");
    }

    private void testStrayItemsOutsidePatternWindow() {
        CustomRecipeData r = new CustomRecipeData();
        r.resultItemId = "minecraft:iron_block";
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.patternSlots[0] = "minecraft:iron_ingot";

        ItemStack[] stray = new ItemStack[9];
        Arrays.fill(stray, ItemStack.EMPTY);
        stray[0] = new ItemStack(Items.IRON_INGOT);
        stray[8] = new ItemStack(Items.DIRT);

        check(!CustomDynamicCraftingRecipe.matchesShaped(createInput3x3(stray), r),
                "Shaped craft must fail when stray item exists outside window");
    }

    private void testShapelessBacktrackPermutations() {
        CustomRecipeData shapeless = new CustomRecipeData();
        shapeless.resultItemId = "minecraft:emerald";
        shapeless.type = RecipeTypeEnum.SHAPED_CRAFTING;
        shapeless.isShapeless = true;
        shapeless.patternSlots[0] = "minecraft:gold_ingot";
        shapeless.patternSlots[1] = "minecraft:iron_ingot";
        shapeless.patternSlots[2] = "minecraft:diamond";

        // Permutation 1: slots 0, 4, 8
        ItemStack[] p1 = new ItemStack[9];
        Arrays.fill(p1, ItemStack.EMPTY);
        p1[0] = new ItemStack(Items.GOLD_INGOT);
        p1[4] = new ItemStack(Items.IRON_INGOT);
        p1[8] = new ItemStack(Items.DIAMOND);
        check(CustomDynamicCraftingRecipe.matchesShapeless(createInput3x3(p1), shapeless),
                "Shapeless craft should match across arbitrary slots (0, 4, 8)");

        // Permutation 2: Reverse order (slots 2, 3, 6)
        ItemStack[] p2 = new ItemStack[9];
        Arrays.fill(p2, ItemStack.EMPTY);
        p2[2] = new ItemStack(Items.DIAMOND);
        p2[3] = new ItemStack(Items.GOLD_INGOT);
        p2[6] = new ItemStack(Items.IRON_INGOT);
        check(CustomDynamicCraftingRecipe.matchesShapeless(createInput3x3(p2), shapeless),
                "Shapeless craft should match in arbitrary order (2, 3, 6)");
    }

    private void testShapelessCountMismatch() {
        CustomRecipeData sl = new CustomRecipeData();
        sl.resultItemId = "minecraft:emerald";
        sl.type = RecipeTypeEnum.SHAPED_CRAFTING;
        sl.isShapeless = true;
        sl.patternSlots[0] = "minecraft:dirt";
        sl.patternSlots[1] = "minecraft:sand";

        // 1. Missing an ingredient
        ItemStack[] missing = new ItemStack[9];
        Arrays.fill(missing, ItemStack.EMPTY);
        missing[0] = new ItemStack(Items.DIRT);
        check(!CustomDynamicCraftingRecipe.matchesShapeless(createInput3x3(missing), sl),
                "Shapeless craft must fail if count < required count");

        // 2. Extra ingredient present
        ItemStack[] extra = new ItemStack[9];
        Arrays.fill(extra, ItemStack.EMPTY);
        extra[0] = new ItemStack(Items.DIRT);
        extra[1] = new ItemStack(Items.SAND);
        extra[2] = new ItemStack(Items.COBBLESTONE);
        check(!CustomDynamicCraftingRecipe.matchesShapeless(createInput3x3(extra), sl),
                "Shapeless craft must fail if extra ingredient is present");
    }

    private void testShiftClickStackClamping() {
        CustomDynamicCraftingRecipe recipe = new CustomDynamicCraftingRecipe(CraftingBookCategory.MISC);

        CustomRecipeData r = new CustomRecipeData();
        r.resultItemId = "minecraft:ender_pearl";
        r.resultCount = 100;
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.patternSlots[0] = "minecraft:ender_pearl";

        RecipeEditorConfig config = RecipeEditorConfig.getInstance();
        config.addOrUpdateRecipe(r);

        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        grid[0] = new ItemStack(Items.ENDER_PEARL);
        CraftingInput input = createInput3x3(grid);

        ItemStack crafted = recipe.assemble(input);
        check(!crafted.isEmpty(), "Crafted item must not be empty");
        check(crafted.getItem() == Items.ENDER_PEARL, "Crafted item must be ender pearl");
        check(crafted.getCount() == 16, "Crafted count must be clamped to item's maxCount (16), got " + crafted.getCount());

        config.removeRecipe(r);
    }
}
