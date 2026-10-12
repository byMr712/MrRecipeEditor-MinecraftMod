package com.recipeeditor.client;

import com.recipeeditor.inspector.RecipeInspector;
import net.fabricmc.api.ClientModInitializer;

public class RecipeEditorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize recipe viewer hooks (REI visibility predicate, etc.)
        com.recipeeditor.integration.RecipeViewerIntegration.init();

        // Asynchronously scan Fabric Mod JARs and tags in background at startup
        RecipeInspector.startJarScanAsync();
    }
}
