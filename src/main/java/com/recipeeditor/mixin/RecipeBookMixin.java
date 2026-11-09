package com.recipeeditor.mixin;

import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBook.class)
public class RecipeBookMixin {

    @Inject(method = "contains(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("HEAD"), cancellable = true)
    private void onContains(RecipeHolder<?> entry, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        if (entry != null && entry.id() != null) {
            if ("recipeeditor".equals(entry.id().getNamespace())) {
                cir.setReturnValue(CustomRecipeDispatcher.isCustomRecipeActive(entry.id()));
                return;
            }
            if (CustomRecipeDispatcher.isRecipeOverridden(entry)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "contains(Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("HEAD"), cancellable = true)
    private void onContainsId(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        if (id != null) {
            if ("recipeeditor".equals(id.getNamespace())) {
                cir.setReturnValue(CustomRecipeDispatcher.isCustomRecipeActive(id));
                return;
            }
            if (CustomRecipeDispatcher.isIdentifierOverridden(id)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
