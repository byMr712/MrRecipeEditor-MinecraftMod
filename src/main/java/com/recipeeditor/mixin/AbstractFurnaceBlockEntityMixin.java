package com.recipeeditor.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onCanBurn(RegistryAccess registryManager, RecipeHolder<? extends AbstractCookingRecipe> recipe, NonNullList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;

        if (slots == null || slots.isEmpty() || recipe == null) {
            return;
        }

        ItemStack input = slots.get(0);
        if (input.isEmpty()) {
            return;
        }

        ItemStack result = recipe.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input), registryManager);
        if (result.isEmpty()) {
            return;
        }

        ItemStack output = slots.get(2);
        if (output.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }

        if (!ItemStack.isSameItemSameComponents(output, result)) {
            cir.setReturnValue(false);
            return;
        }

        int maxAllowed = Math.min(output.getMaxStackSize(), count);
        cir.setReturnValue(output.getCount() + result.getCount() <= maxAllowed);
    }

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onBurn(RegistryAccess registryManager, RecipeHolder<? extends AbstractCookingRecipe> recipe, NonNullList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;

        if (slots == null || slots.isEmpty() || recipe == null) {
            return;
        }

        ItemStack inputStack = slots.get(0);
        if (inputStack.isEmpty()) {
            return;
        }

        ItemStack result = recipe.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack), registryManager);
        if (result.isEmpty()) {
            return;
        }

        ItemStack output = slots.get(2);
        if (output.isEmpty()) {
            slots.set(2, result.copy());
            inputStack.shrink(1);
            cir.setReturnValue(true);
            return;
        }

        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return;
        }

        int maxAllowed = Math.min(output.getMaxStackSize(), count);
        int targetCount = output.getCount() + result.getCount();
        if (targetCount > maxAllowed) {
            return;
        }

        output.grow(result.getCount());
        inputStack.shrink(1);
        cir.setReturnValue(true);
    }

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onCanBurnNeoForge(RegistryAccess registryManager, RecipeHolder<? extends AbstractCookingRecipe> recipe, NonNullList<ItemStack> slots, int count, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Boolean> cir) {
        onCanBurn(registryManager, recipe, slots, count, cir);
    }

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onBurnNeoForge(RegistryAccess registryManager, RecipeHolder<? extends AbstractCookingRecipe> recipe, NonNullList<ItemStack> slots, int count, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Boolean> cir) {
        onBurn(registryManager, recipe, slots, count, cir);
    }
}
