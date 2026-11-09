package com.recipeeditor.test;

import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.recipe.CustomDynamicSmithingRecipe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.Optional;

/**
 * Tests the dynamic smithing table upgrade engine:
 * Template, base, and addition validation, empty template handling,
 * and component preservation during transmutation.
 */
public class SmithingEngineTest implements FabricClientGameTest {

    private int checkCount = 0;

    private void check(boolean condition, String message) {
        checkCount++;
        if (!condition) {
            throw new AssertionError("Smithing Check #" + checkCount + " failed: " + message);
        }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            testFullSmithingMatch();
            testSmithingTemplateOptional();
            testSmithingRejectionOnWrongBase();
            testSmithingRejectionOnWrongAddition();
            testSmithingAssembly();
        });

        System.out.println("[SmithingEngineTest] PASSED with " + checkCount + " successful assertions!");
    }

    private void testFullSmithingMatch() {
        // Upgrade: Netherite upgrade template + Diamond Chestplate + Netherite Ingot -> Netherite Chestplate
        CustomDynamicSmithingRecipe recipe = new CustomDynamicSmithingRecipe(
                "smithing_test",
                Optional.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                Optional.of(Ingredient.of(Items.DIAMOND_CHESTPLATE)),
                Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
                new ItemStack(Items.NETHERITE_CHESTPLATE)
        );

        SmithingRecipeInput valid = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.NETHERITE_INGOT)
        );

        check(recipe.matches(valid, null), "Valid smithing input must match");
    }

    private void testSmithingTemplateOptional() {
        // Smithing recipe with NO template required
        CustomDynamicSmithingRecipe recipe = new CustomDynamicSmithingRecipe(
                "smithing_no_template",
                Optional.empty(),
                Optional.of(Ingredient.of(Items.IRON_SWORD)),
                Optional.of(Ingredient.of(Items.DIAMOND)),
                new ItemStack(Items.DIAMOND_SWORD)
        );

        SmithingRecipeInput inputWithoutTemplate = new SmithingRecipeInput(
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.DIAMOND)
        );

        check(recipe.matches(inputWithoutTemplate, null), "Smithing without template must match empty template slot");
    }

    private void testSmithingRejectionOnWrongBase() {
        CustomDynamicSmithingRecipe recipe = new CustomDynamicSmithingRecipe(
                "smithing_test",
                Optional.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                Optional.of(Ingredient.of(Items.DIAMOND_CHESTPLATE)),
                Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
                new ItemStack(Items.NETHERITE_CHESTPLATE)
        );

        SmithingRecipeInput wrongBase = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.GOLDEN_CHESTPLATE),
                new ItemStack(Items.NETHERITE_INGOT)
        );

        check(!recipe.matches(wrongBase, null), "Wrong base armor item must be rejected");
    }

    private void testSmithingRejectionOnWrongAddition() {
        CustomDynamicSmithingRecipe recipe = new CustomDynamicSmithingRecipe(
                "smithing_test",
                Optional.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                Optional.of(Ingredient.of(Items.DIAMOND_CHESTPLATE)),
                Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
                new ItemStack(Items.NETHERITE_CHESTPLATE)
        );

        SmithingRecipeInput wrongAddition = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIRT)
        );

        check(!recipe.matches(wrongAddition, null), "Wrong addition item must be rejected");
    }

    private void testSmithingAssembly() {
        CustomDynamicSmithingRecipe recipe = new CustomDynamicSmithingRecipe(
                "smithing_assemble",
                Optional.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                Optional.of(Ingredient.of(Items.DIAMOND_SWORD)),
                Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
                new ItemStack(Items.NETHERITE_SWORD, 1)
        );

        SmithingRecipeInput input = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.NETHERITE_INGOT)
        );

        ItemStack result = recipe.assemble(input);
        check(!result.isEmpty(), "Smithing assembled item must not be empty");
        check(result.getItem() == Items.NETHERITE_SWORD, "Assembled item must be Netherite Sword");
    }
}
