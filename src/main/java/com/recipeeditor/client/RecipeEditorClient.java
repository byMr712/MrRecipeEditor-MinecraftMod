package com.recipeeditor.client;

import com.recipeeditor.RecipeEditorMod;
import com.recipeeditor.inspector.RecipeInspector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class RecipeEditorClient implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        RecipeEditorMod.init();
    }

    @Override
    public void onInitializeClient() {
        // Initialize recipe viewer hooks (REI visibility predicate, etc.)
        com.recipeeditor.integration.RecipeViewerIntegration.init();

        // Asynchronously scan Fabric Mod JARs and tags in background at startup
        RecipeInspector.startJarScanAsync();
    }
}
