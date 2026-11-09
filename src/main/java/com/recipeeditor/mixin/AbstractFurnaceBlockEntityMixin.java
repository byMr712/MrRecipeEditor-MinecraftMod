package com.recipeeditor.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onCanBurn(RegistryAccess registryAccess, RecipeHolder<? extends AbstractCookingRecipe> recipe, SingleRecipeInput input, NonNullList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;

        if (slots.get(0).isEmpty() || recipe == null) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack result = recipe.value().assemble(input, registryAccess);
        if (result.isEmpty()) {
            cir.setReturnValue(false);
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

        int maxAllowed = Math.min(count, Math.min(output.getMaxStackSize(), result.getMaxStackSize()));
        cir.setReturnValue(output.getCount() + result.getCount() <= maxAllowed);
    }

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onBurn(RegistryAccess registryAccess, RecipeHolder<? extends AbstractCookingRecipe> recipe, SingleRecipeInput input, NonNullList<ItemStack> slots, int count, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;

        if (recipe == null || !canBurn(registryAccess, recipe, input, slots, count)) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack inputStack = slots.get(0);
        ItemStack result = recipe.value().assemble(input, registryAccess);
        ItemStack output = slots.get(2);

        if (output.isEmpty()) {
            slots.set(2, result.copy());
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            output.grow(result.getCount());
        }

        if (inputStack.is(net.minecraft.world.level.block.Blocks.WET_SPONGE.asItem()) && !slots.get(1).isEmpty() && slots.get(1).is(net.minecraft.world.item.Items.BUCKET)) {
            slots.set(1, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        }

        inputStack.shrink(1);
        cir.setReturnValue(true);
    }

    @Shadow
    private static boolean canBurn(RegistryAccess registryAccess, RecipeHolder<? extends AbstractCookingRecipe> recipe, SingleRecipeInput input, NonNullList<ItemStack> slots, int count) {
        throw new AssertionError();
    }
}
