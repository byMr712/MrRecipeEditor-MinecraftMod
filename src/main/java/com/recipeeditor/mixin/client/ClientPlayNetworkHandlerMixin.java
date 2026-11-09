package com.recipeeditor.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
    private void onHandleUpdateRecipesReturn(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null && client.player.getRecipeBook() != null) {
                ClientRecipeBook book = client.player.getRecipeBook();
                book.getCollections().forEach(r -> r.updateKnownRecipes(book));
                if (client.screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener provider) {
                    provider.recipesUpdated();
                }
            }
        } catch (Throwable ignored) {}
    }
}
