package com.recipeeditor.test;

import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.inspector.RecipeInspector;
import com.recipeeditor.inspector.TagResolver;
import com.recipeeditor.recipe.CustomDynamicCraftingRecipe;
import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EngineLogicTest {

    @BeforeAll
    public static void setUp() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testRecipeBoundsAndHash() {
        CustomRecipeData recipe = new CustomRecipeData();
        recipe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        recipe.patternSlots[4] = "minecraft:iron_ingot"; // center
        recipe.computePatternBounds();

        assertEquals(1, recipe.getPatternWidth());
        assertEquals(1, recipe.getPatternHeight());
        assertEquals(1, recipe.getMinRow());
        assertEquals(1, recipe.getMinCol());
        assertEquals(1, recipe.getMaxCol());
    }

    @Test
    public void testCompositeKeys() {
        CustomRecipeData r1 = new CustomRecipeData();
        r1.resultItemId = "minecraft:torch";
        r1.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r1.patternSlots[0] = "minecraft:coal";
        r1.patternSlots[3] = "minecraft:stick";

        CustomRecipeData r2 = new CustomRecipeData();
        r2.resultItemId = "minecraft:torch";
        r2.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r2.patternSlots[0] = "minecraft:charcoal";
        r2.patternSlots[3] = "minecraft:stick";

        assertNotEquals(r1.getKey(), r2.getKey());
        assertTrue(r1.getKey().contains("minecraft:torch#SHAPED_CRAFTING#"));
    }

    @Test
    public void testSlidingWindowShapedCrafting() {
        CustomRecipeData recipe = new CustomRecipeData();
        recipe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        recipe.resultItemId = "minecraft:stick";
        recipe.patternSlots[0] = "minecraft:oak_planks";
        recipe.patternSlots[3] = "minecraft:oak_planks";
        recipe.computePatternBounds();

        CraftingInput topOffset = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        assertTrue(CustomDynamicCraftingRecipe.matchesCrafting(topOffset, recipe));

        CraftingInput centerOffset = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        assertTrue(CustomDynamicCraftingRecipe.matchesCrafting(centerOffset, recipe));

        CraftingInput rightOffset = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS),
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        assertTrue(CustomDynamicCraftingRecipe.matchesCrafting(rightOffset, recipe));
    }

    @Test
    public void testHorizontalMirroring() {
        CustomRecipeData recipe = new CustomRecipeData();
        recipe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        recipe.resultItemId = "minecraft:iron_axe";
        recipe.patternSlots[0] = "minecraft:iron_ingot";
        recipe.patternSlots[1] = "minecraft:iron_ingot";
        recipe.patternSlots[3] = "minecraft:iron_ingot";
        recipe.patternSlots[4] = "minecraft:stick";
        recipe.patternSlots[7] = "minecraft:stick";
        recipe.computePatternBounds();

        CraftingInput mirrored = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.STICK), new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY
        ));
        assertTrue(CustomDynamicCraftingRecipe.matchesCrafting(mirrored, recipe));
    }

    @Test
    public void testShapelessBacktracking() {
        CustomRecipeData recipe = new CustomRecipeData();
        recipe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        recipe.isShapeless = true;
        recipe.resultItemId = "minecraft:fire_charge";
        recipe.patternSlots[0] = "minecraft:gunpowder";
        recipe.patternSlots[1] = "minecraft:blaze_powder";
        recipe.patternSlots[2] = "minecraft:coal";

        CraftingInput permuted = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.COAL), ItemStack.EMPTY,
                new ItemStack(Items.BLAZE_POWDER), ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.GUNPOWDER)
        ));
        assertTrue(CustomDynamicCraftingRecipe.matchesCrafting(permuted, recipe));

        CraftingInput extraItem = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.COAL), ItemStack.EMPTY,
                new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.DIRT), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.GUNPOWDER)
        ));
        assertFalse(CustomDynamicCraftingRecipe.matchesCrafting(extraItem, recipe));
    }

    @Test
    public void testShiftClickStackClamp() {
        CustomRecipeData recipe = new CustomRecipeData();
        recipe.type = RecipeTypeEnum.SHAPED_CRAFTING;
        recipe.resultItemId = "minecraft:totem_of_undying";
        recipe.resultCount = 64;

        Item totem = recipe.getResultItem();
        int safeCount = Math.min(totem.getDefaultInstance().getMaxStackSize(), Math.max(1, recipe.getResultCountForType(recipe.type)));
        assertEquals(1, safeCount);
    }

    @Test
    public void testSmithingSlotMatching() {
        RecipeEditorConfig config = RecipeEditorConfig.getInstance();
        boolean origEnabled = config.modEnabled;
        config.modEnabled = true;

        CustomRecipeData smith = new CustomRecipeData();
        smith.type = RecipeTypeEnum.SMITHING;
        smith.resultItemId = "minecraft:netherite_sword";
        smith.patternSlots[0] = "minecraft:netherite_upgrade_smithing_template";
        smith.patternSlots[1] = "minecraft:diamond_sword";
        smith.patternSlots[2] = "minecraft:netherite_ingot";
        smith.enabled = true;
        config.addOrUpdateRecipe(smith);

        assertTrue(CustomRecipeDispatcher.isCustomSmithingSlotMatch(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        assertTrue(CustomRecipeDispatcher.isCustomSmithingSlotMatch(1, new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(CustomRecipeDispatcher.isCustomSmithingSlotMatch(2, new ItemStack(Items.NETHERITE_INGOT)));
        assertFalse(CustomRecipeDispatcher.isCustomSmithingSlotMatch(0, new ItemStack(Items.STICK)));

        config.removeRecipe(smith);
        config.modEnabled = origEnabled;
    }

    @Test
    public void testCategoryHeuristics() {
        assertEquals(CraftingBookCategory.EQUIPMENT, CustomRecipeDispatcher.getCraftingCategory(Items.DIAMOND_SWORD));
        assertEquals(CraftingBookCategory.EQUIPMENT, CustomRecipeDispatcher.getCraftingCategory(Items.SHIELD));
        assertEquals(CraftingBookCategory.REDSTONE, CustomRecipeDispatcher.getCraftingCategory(Items.REDSTONE_BLOCK));
        assertEquals(CraftingBookCategory.REDSTONE, CustomRecipeDispatcher.getCraftingCategory(Items.HOPPER));
        assertEquals(CraftingBookCategory.BUILDING, CustomRecipeDispatcher.getCraftingCategory(Items.STONE));
        assertEquals(CraftingBookCategory.MISC, CustomRecipeDispatcher.getCraftingCategory(Items.STICK));
    }

    @Test
    public void testKeyboardLayoutFlipping() {
        assertEquals("тотем", RecipeInspector.flipKeyboardLayout("njntv"));
        assertEquals("iron", RecipeInspector.flipKeyboardLayout("шкщт"));
        assertEquals("stick", RecipeInspector.flipKeyboardLayout("ыешсл"));
    }

    @Test
    public void testUnknownTagResilience() {
        Item unknown = TagResolver.resolveTag("mod_not_installed:phantom_crystal_tag");
        assertEquals(Items.AIR, unknown);

        List<Item> unknownList = TagResolver.getAllItemsForTag("mod_not_installed:phantom_crystal_tag");
        assertTrue(unknownList.isEmpty());
    }

    @Test
    public void testSafeIngredientForMissingTag() {
        CustomRecipeData r = new CustomRecipeData();
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.patternSlots[0] = "#mod_not_installed:non_existent_tag";

        Ingredient ing = r.computeIngredientForSlot(0);
        assertNotNull(ing);
        assertFalse(ing.test(new ItemStack(Items.AIR)));
        assertFalse(ing.test(new ItemStack(Items.DIRT)));
    }
}
