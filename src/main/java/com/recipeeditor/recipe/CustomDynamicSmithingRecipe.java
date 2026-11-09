package com.recipeeditor.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class CustomDynamicSmithingRecipe extends SmithingTransformRecipe {
    private final Ingredient templateIngredient;
    private final Ingredient baseIngredient;
    private final Ingredient additionIngredient;
    private final ItemStack result;

    public CustomDynamicSmithingRecipe(String group, Optional<Ingredient> template, Optional<Ingredient> base, Optional<Ingredient> addition, ItemStack resultStack) {
        super(
            template.orElse(Ingredient.EMPTY),
            base.orElse(Ingredient.EMPTY),
            addition.orElse(Ingredient.EMPTY),
            resultStack
        );
        this.templateIngredient = template.orElse(Ingredient.EMPTY);
        this.baseIngredient = base.orElse(Ingredient.EMPTY);
        this.additionIngredient = addition.orElse(Ingredient.EMPTY);
        this.result = resultStack;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level world) {
        if (!testSlot(this.templateIngredient, input.template())) return false;
        if (!testSlot(this.baseIngredient, input.base())) return false;
        if (!testSlot(this.additionIngredient, input.addition())) return false;
        return true;
    }

    private static boolean testSlot(Ingredient ingredient, ItemStack stack) {
        if (ingredient == null || ingredient.isEmpty()) {
            return stack == null || stack.isEmpty();
        }
        return stack != null && ingredient.test(stack);
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider lookup) {
        return this.result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return this.result;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return this.templateIngredient != null && !this.templateIngredient.isEmpty() && this.templateIngredient.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return this.baseIngredient != null && !this.baseIngredient.isEmpty() && this.baseIngredient.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return this.additionIngredient != null && !this.additionIngredient.isEmpty() && this.additionIngredient.test(stack);
    }
}
