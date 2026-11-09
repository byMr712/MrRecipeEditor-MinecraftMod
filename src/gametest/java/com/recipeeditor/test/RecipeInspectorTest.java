package com.recipeeditor.test;

import com.recipeeditor.RecipeEditorMod;
import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeTypeEnum;
import com.recipeeditor.inspector.TagResolver;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Tests tag resolution tiers, static fallbacks, tag expansion,
 * and component binding safety on client/title-screen.
 */
public class RecipeInspectorTest implements FabricClientGameTest {

    private int checkCount = 0;

    private void check(boolean condition, String message) {
        checkCount++;
        if (!condition) {
            throw new AssertionError("Inspector Check #" + checkCount + " failed: " + message);
        }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            testTitleScreenComponentBindingGuard();
            testStaticTagFallbacks();
            testStaticTagExpansions();
            testUnknownTagResilience();
            testSafeIngredientForMissingTag();
        });

        System.out.println("[RecipeInspectorTest] PASSED with " + checkCount + " successful assertions!");
    }

    private void testTitleScreenComponentBindingGuard() {
        RecipeEditorMod.ensureItemComponentsBound();

        check(Items.DIAMOND_SWORD.components() != null,
                "Diamond sword components must be bound after ensureItemComponentsBound");
        check(Items.NETHERITE_CHESTPLATE.components() != null,
                "Netherite chestplate components must be bound after ensureItemComponentsBound");
    }

    private void testStaticTagFallbacks() {
        Item planks = TagResolver.resolveTag("minecraft:planks");
        check(planks != Items.AIR, "minecraft:planks should resolve to a valid wood plank item");
        check(planks == Items.OAK_PLANKS, "minecraft:planks fallback should be oak planks");

        Item ironIngot = TagResolver.resolveTag("c:iron_ingots");
        check(ironIngot == Items.IRON_INGOT, "c:iron_ingots should resolve to iron ingot");

        Item diamond = TagResolver.resolveTag("c:diamonds");
        check(diamond == Items.DIAMOND, "c:diamonds should resolve to diamond");

        Item stick = TagResolver.resolveTag("c:wooden_rods");
        check(stick == Items.STICK, "c:wooden_rods should resolve to stick");
    }

    private void testStaticTagExpansions() {
        List<Item> expandedPlanks = TagResolver.getAllItemsForTag("minecraft:planks");
        check(!expandedPlanks.isEmpty(), "Expanded planks must not be empty");
        check(expandedPlanks.contains(Items.OAK_PLANKS), "Expanded planks must contain oak planks");
        check(expandedPlanks.contains(Items.SPRUCE_PLANKS), "Expanded planks must contain spruce planks");
        check(expandedPlanks.contains(Items.BIRCH_PLANKS), "Expanded planks must contain birch planks");

        List<Item> expandedLogs = TagResolver.getAllItemsForTag("minecraft:logs");
        check(!expandedLogs.isEmpty(), "Expanded logs must not be empty");
        check(expandedLogs.contains(Items.OAK_LOG), "Expanded logs must contain oak log");
    }

    private void testUnknownTagResilience() {
        Item unknown = TagResolver.resolveTag("mod_not_installed:phantom_crystal_tag");
        check(unknown == Items.AIR, "Non-existent tag must resolve to AIR without exceptions");

        List<Item> unknownList = TagResolver.getAllItemsForTag("mod_not_installed:phantom_crystal_tag");
        check(unknownList.isEmpty(), "Non-existent tag expansion must return empty list");
    }

    private void testSafeIngredientForMissingTag() {
        CustomRecipeData r = new CustomRecipeData();
        r.type = RecipeTypeEnum.SHAPED_CRAFTING;
        r.patternSlots[0] = "#mod_not_installed:non_existent_tag";

        Ingredient ing = r.computeIngredientForSlot(0);
        check(ing != null, "computeIngredientForSlot must NEVER return null for unknown tag");
        check(ing.isEmpty(), "computeIngredientForSlot for unknown tag must return an empty ingredient");
        check(!ing.test(new ItemStack(Items.AIR)), "Empty tag ingredient must NOT match AIR!");
        check(!ing.test(new ItemStack(Items.DIRT)), "Empty tag ingredient must NOT match any arbitrary item!");
    }
}
