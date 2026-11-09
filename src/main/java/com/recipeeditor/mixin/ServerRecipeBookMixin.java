package com.recipeeditor.mixin;

import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {

    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"))
    private void onSendInitialRecipeBookHead(ServerPlayer player, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        try {
            ServerRecipeBook book = (ServerRecipeBook) (Object) this;
            for (ResourceLocation remId : CustomRecipeDispatcher.getRecentlyRemovedIds()) {
                RecipeHolder<?> remEntry = CustomRecipeDispatcher.getRecentlyRemovedEntry(remId);
                if (remEntry != null) {
                    book.remove(remEntry);
                }
                try {
                    ((com.recipeeditor.mixin.RecipeBookInvoker) book).recipeeditor$invokeRemove(remId);
                } catch (Throwable ignored) {}
            }
            for (RecipeHolder<?> entry : CustomRecipeDispatcher.getAllCustomRecipes()) {
                book.add(entry);
                book.addHighlight(entry);
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("RETURN"))
    private void onSendInitialRecipeBook(ServerPlayer player, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        CustomRecipeDispatcher.sendCustomRecipeBookEntries(player);
    }

    @Redirect(
            method = "loadRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V"
            )
    )
    private void recipeeditor$silenceUnrecognizedRecipeLog(Logger logger, String message, Object arg) {
        if (arg instanceof ResourceLocation id && "recipeeditor".equals(id.getNamespace())) {
            return;
        }
        logger.error(message, arg);
    }
}
