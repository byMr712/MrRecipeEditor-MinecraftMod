package com.recipeeditor.mixin;

import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mixin(value = RecipeManager.class, priority = 500)
public class RecipeManagerMixin {

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeFor(
            RecipeType<T> type,
            I input,
            Level world,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> custom = CustomRecipeDispatcher.getCustomMatch(type, input, world);
        if (custom.isPresent()) {
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeForReturn(
            RecipeType<T> type,
            I input,
            Level world,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> res = cir.getReturnValue();
        if (res != null && res.isPresent() && CustomRecipeDispatcher.isRecipeOverridden(res.get())) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeForWithLast(
            RecipeType<T> type,
            I input,
            Level world,
            RecipeHolder<T> last,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> custom = CustomRecipeDispatcher.getCustomMatch(type, input, world);
        if (custom.isPresent()) {
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeForWithLastReturn(
            RecipeType<T> type,
            I input,
            Level world,
            RecipeHolder<T> last,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> res = cir.getReturnValue();
        if (res != null && res.isPresent() && CustomRecipeDispatcher.isRecipeOverridden(res.get())) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeForWithId(
            RecipeType<T> type,
            I input,
            Level world,
            ResourceLocation id,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> custom = CustomRecipeDispatcher.getCustomMatch(type, input, world);
        if (custom.isPresent()) {
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipeForWithIdReturn(
            RecipeType<T> type,
            I input,
            Level world,
            ResourceLocation id,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<T>> res = cir.getReturnValue();
        if (res != null && res.isPresent() && CustomRecipeDispatcher.isRecipeOverridden(res.get())) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private Collection<RecipeHolder<?>> recipeeditor$cachedValues = null;
    @org.spongepowered.asm.mixin.Unique
    private Collection<RecipeHolder<?>> recipeeditor$lastOriginalValues = null;
    @org.spongepowered.asm.mixin.Unique
    private int recipeeditor$lastConfigVer = -1;

    @Inject(method = "getRecipes", at = @At("RETURN"), cancellable = true)
    private void onGetRecipes(CallbackInfoReturnable<Collection<RecipeHolder<?>>> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Collection<RecipeHolder<?>> original = cir.getReturnValue();
        com.recipeeditor.config.RecipeEditorConfig config = com.recipeeditor.config.RecipeEditorConfig.getInstance();
        int currentVer = config != null ? config.configVersion : 0;
        if (original == recipeeditor$lastOriginalValues && recipeeditor$cachedValues != null && recipeeditor$lastConfigVer == currentVer) {
            cir.setReturnValue(recipeeditor$cachedValues);
            return;
        }

        List<RecipeHolder<?>> filtered = new ArrayList<>();
        if (original != null) {
            for (RecipeHolder<?> entry : original) {
                if (entry.id() != null && "recipeeditor".equals(entry.id().getNamespace())) {
                    continue;
                }
                if (!CustomRecipeDispatcher.isRecipeOverridden(entry)) {
                    filtered.add(entry);
                }
            }
        }
        filtered.addAll(CustomRecipeDispatcher.getAllCustomRecipes());
        recipeeditor$lastOriginalValues = original;
        recipeeditor$lastConfigVer = currentVer;
        recipeeditor$cachedValues = java.util.Collections.unmodifiableList(filtered);
        cir.setReturnValue(recipeeditor$cachedValues);
    }

    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetRecipesFor(
            RecipeType<T> type,
            I input,
            Level world,
            CallbackInfoReturnable<List<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        List<RecipeHolder<T>> original = cir.getReturnValue();
        List<RecipeHolder<T>> combined = new ArrayList<>();
        List<RecipeHolder<T>> custom = CustomRecipeDispatcher.getCustomMatches(type, input, world);
        if (custom != null && !custom.isEmpty()) {
            combined.addAll(custom);
        }
        if (original != null) {
            for (RecipeHolder<T> entry : original) {
                if (entry.id() != null && "recipeeditor".equals(entry.id().getNamespace())) {
                    continue;
                }
                if (!CustomRecipeDispatcher.isRecipeOverridden(entry)) {
                    combined.add(entry);
                }
            }
        }
        cir.setReturnValue(combined);
    }

    @Inject(method = "getAllRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void onGetAllRecipesFor(
            RecipeType<T> type,
            CallbackInfoReturnable<List<RecipeHolder<T>>> cir
    ) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        List<RecipeHolder<T>> original = cir.getReturnValue();
        List<RecipeHolder<T>> combined = new ArrayList<>();
        List<RecipeHolder<T>> custom = CustomRecipeDispatcher.getAllCustomRecipesOfType(type);
        if (custom != null && !custom.isEmpty()) {
            combined.addAll(custom);
        }
        if (original != null) {
            for (RecipeHolder<T> entry : original) {
                if (entry.id() != null && "recipeeditor".equals(entry.id().getNamespace())) {
                    continue;
                }
                if (!CustomRecipeDispatcher.isRecipeOverridden(entry)) {
                    combined.add(entry);
                }
            }
        }
        cir.setReturnValue(combined);
    }

    @Inject(method = "getOrderedRecipes", at = @At("RETURN"), cancellable = true)
    private void onGetOrderedRecipes(CallbackInfoReturnable<Collection<RecipeHolder<?>>> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        cir.setReturnValue(((RecipeManager) (Object) this).getRecipes());
    }

    @Inject(method = "byKey", at = @At("RETURN"), cancellable = true)
    private void onByKey(ResourceLocation id, CallbackInfoReturnable<Optional<RecipeHolder<?>>> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) return;
        Optional<RecipeHolder<?>> original = cir.getReturnValue();
        if (original != null && original.isPresent()) {
            return;
        }
        Optional<RecipeHolder<?>> custom = CustomRecipeDispatcher.getCustomRecipeEntryById(id);
        if (custom != null && custom.isPresent()) {
            cir.setReturnValue(custom);
        }
    }
}
