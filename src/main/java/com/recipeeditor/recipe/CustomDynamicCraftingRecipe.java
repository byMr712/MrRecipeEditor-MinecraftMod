package com.recipeeditor.recipe;

import com.recipeeditor.RecipeEditorMod;
import com.recipeeditor.config.CustomRecipeData;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.config.RecipeTypeEnum;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

public class CustomDynamicCraftingRecipe extends ShapedRecipe {

    public CustomDynamicCraftingRecipe(CraftingBookCategory category) {
        super("recipeeditor", category, createDefaultPattern(), new ItemStack(Items.DIRT), true);
    }

    private static ShapedRecipePattern createDefaultPattern() {
        Map<Character, Ingredient> key = Map.of('A', Ingredient.of(Items.DIRT));
        return ShapedRecipePattern.of(key, "A");
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    private static volatile CraftingInput lastInput = null;
    private static volatile CustomRecipeData lastMatchedRecipe = null;
    private static volatile int lastInputConfigVersion = -1;

    private static synchronized CustomRecipeData findMatchingRecipe(CraftingInput input) {
        RecipeEditorConfig config = RecipeEditorConfig.getInstance();
        if (config == null || !config.modEnabled || config.recipes == null || config.recipes.isEmpty() || input.isEmpty()) {
            return null;
        }

        int currentVer = config.configVersion;
        CraftingInput prevInput = lastInput;
        if (prevInput != null && lastInputConfigVersion == currentVer && input.equals(prevInput)) {
            return lastMatchedRecipe;
        }

        CustomRecipeData matched = null;
        List<CustomRecipeData> craftingRecipes = config.getSortedCraftingRecipes();
        for (int i = 0; i < craftingRecipes.size(); i++) {
            CustomRecipeData recipeData = craftingRecipes.get(i);
            if (matchesCrafting(input, recipeData)) {
                matched = recipeData;
                break;
            }
        }

        lastInput = input;
        lastMatchedRecipe = matched;
        lastInputConfigVersion = currentVer;
        return matched;
    }

    public static boolean matchesCrafting(CraftingInput input, CustomRecipeData recipeData) {
        if (recipeData == null || recipeData.type != RecipeTypeEnum.SHAPED_CRAFTING) return false;
        if (recipeData.isShapeless) {
            return matchesShapeless(input, recipeData);
        } else {
            return matchesShaped(input, recipeData);
        }
    }

    public static boolean matchesShapeless(CraftingInput input, CustomRecipeData recipeData) {
        if (input.isEmpty()) return false;

        List<Ingredient> ingredients = recipeData.getNonEmptyIngredients();
        int expectedCount = ingredients.size();
        if (expectedCount == 0 || input.ingredientCount() != expectedCount) {
            return false;
        }

        ItemStack[] inputStacks = new ItemStack[expectedCount];
        int count = 0;
        int inputSize = input.size();
        for (int i = 0; i < inputSize; i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (count >= expectedCount) return false;
                inputStacks[count++] = stack;
            }
        }
        if (count != expectedCount) return false;

        return matchShapelessBacktrack(inputStacks, ingredients, 0, new boolean[expectedCount]);
    }

    private static boolean matchShapelessBacktrack(ItemStack[] stacks, List<Ingredient> ingredients,
                                             int stackIdx, boolean[] usedIngs) {
        if (stackIdx >= stacks.length) {
            return true;
        }

        ItemStack currentStack = stacks[stackIdx];
        for (int i = 0; i < ingredients.size(); i++) {
            if (!usedIngs[i] && ingredients.get(i).test(currentStack)) {
                usedIngs[i] = true;
                if (matchShapelessBacktrack(stacks, ingredients, stackIdx + 1, usedIngs)) {
                    return true;
                }
                usedIngs[i] = false;
            }
        }
        return false;
    }

    public static boolean matchesShaped(CraftingInput input, CustomRecipeData recipeData) {
        if (input.isEmpty()) return false;

        int patternW = recipeData.getPatternWidth();
        int patternH = recipeData.getPatternHeight();
        if (patternW == 0 || patternH == 0) {
            return false;
        }

        int inputW = input.width();
        int inputH = input.height();

        if (inputW < patternW || inputH < patternH) {
            return false;
        }

        int minRow = recipeData.getMinRow();
        int minCol = recipeData.getMinCol();
        int maxCol = recipeData.getMaxCol();

        for (int dx = 0; dx <= inputW - patternW; dx++) {
            for (int dy = 0; dy <= inputH - patternH; dy++) {
                if (checkMatchAt(input, recipeData, minRow, minCol, maxCol, patternW, patternH, dx, dy, false)) {
                    return true;
                }
                if (checkMatchAt(input, recipeData, minRow, minCol, maxCol, patternW, patternH, dx, dy, true)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkMatchAt(CraftingInput input, CustomRecipeData recipeData,
                                 int minRow, int minCol, int maxCol, int patternW, int patternH,
                                 int dx, int dy, boolean mirrored) {
        int inputW = input.width();
        int inputH = input.height();

        for (int ir = 0; ir < inputH; ir++) {
            for (int ic = 0; ic < inputW; ic++) {
                int pr = ir - dy;
                int pc = ic - dx;

                Ingredient expected = null;
                if (pr >= 0 && pr < patternH && pc >= 0 && pc < patternW) {
                    int slotCol = mirrored ? (maxCol - pc) : (minCol + pc);
                    int slotIdx = (minRow + pr) * 3 + slotCol;
                    expected = recipeData.getIngredientAt(slotIdx);
                }

                ItemStack actual = input.getItem(ic, ir);
                if (expected == null) {
                    if (!actual.isEmpty()) {
                        return false;
                    }
                } else {
                    if (!expected.test(actual)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMatchingRecipe(input) != null;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remainders.size(); i++) {
            ItemStack stack = input.getItem(i);
            Item item = stack.getItem();
            if (item.hasCraftingRemainingItem()) {
                remainders.set(i, new ItemStack(item.getCraftingRemainingItem()));
            }
        }
        return remainders;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        CustomRecipeData matched = findMatchingRecipe(input);
        if (matched != null) {
            Item resultItem = matched.getResultItem();
            if (resultItem != Items.AIR) {
                int safeCount = Math.min(resultItem.getDefaultInstance().getMaxStackSize(), Math.max(1, matched.getResultCountForType(matched.type)));
                return new ItemStack(resultItem, safeCount);
            }
        }
        return ItemStack.EMPTY;
    }

    private static final java.util.concurrent.atomic.AtomicInteger DISPLAY_VERSION = new java.util.concurrent.atomic.AtomicInteger(0);

    public static void invalidateDisplayCache() {
        DISPLAY_VERSION.incrementAndGet();
        lastInput = null;
        lastMatchedRecipe = null;
        lastInputConfigVersion = -1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeEditorMod.CUSTOM_CRAFTING_SERIALIZER;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomDynamicCraftingRecipe;
    }

    @Override
    public int hashCode() {
        return CustomDynamicCraftingRecipe.class.hashCode();
    }
}
