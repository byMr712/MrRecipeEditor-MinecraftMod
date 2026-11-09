package com.recipeeditor.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBook.class)
public interface RecipeBookInvoker {
    @Invoker("remove")
    void recipeeditor$invokeRemove(ResourceLocation id);
}
