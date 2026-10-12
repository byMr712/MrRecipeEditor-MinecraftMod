package com.recipeeditor.mixin;

import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {

    @Shadow @Final protected Set<ResourceKey<Recipe<?>>> known;
    @Shadow @Final protected Set<ResourceKey<Recipe<?>>> highlight;

    @Inject(method = "contains", at = @At("HEAD"), cancellable = true)
    private void onContains(ResourceKey<Recipe<?>> key, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        if (key != null && key.identifier() != null) {
            if ("recipeeditor".equals(key.identifier().getNamespace())) {
                cir.setReturnValue(true);
                return;
            }
            if (CustomRecipeDispatcher.isIdentifierOverridden(key.identifier())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("RETURN"))
    private void onSendInitialRecipeBook(ServerPlayer player, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        CustomRecipeDispatcher.sendCustomRecipeBookEntries(player);
    }

    @Inject(method = "loadUntrusted", at = @At("HEAD"), cancellable = true)
    private void onLoadUntrusted(ServerRecipeBook.Packed packed, Predicate<ResourceKey<Recipe<?>>> predicate, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        ci.cancel();
        ((ServerRecipeBook) (Object) this).getBookSettings().replaceFrom(packed.settings());
        loadRecipesFiltered(packed.known(), this.known::add, predicate);
        loadRecipesFiltered(packed.highlight(), this.highlight::add, predicate);
    }

    @Unique
    private static void loadRecipesFiltered(
            List<ResourceKey<Recipe<?>>> list,
            Consumer<ResourceKey<Recipe<?>>> consumer,
            Predicate<ResourceKey<Recipe<?>>> predicate
    ) {
        for (ResourceKey<Recipe<?>> key : list) {
            if (key != null && key.identifier() != null && "recipeeditor".equals(key.identifier().getNamespace())) {
                if (predicate.test(key)) {
                    consumer.accept(key);
                }
                // Silently skip removed/outdated recipeeditor recipes without logging error
                continue;
            }
            if (!predicate.test(key)) {
                org.slf4j.LoggerFactory.getLogger(ServerRecipeBook.class).error("Tried to load unrecognized recipe: {} removed now.", key);
            } else {
                consumer.accept(key);
            }
        }
    }
}
