package com.recipeeditor;

import com.recipeeditor.client.gui.RecipeEditorScreen;
import com.recipeeditor.config.RecipeEditorConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("recipeeditor")
public class RecipeEditorNeoForge {
    public static final String MOD_ID = "recipeeditor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RecipeEditorNeoForge(ModContainer modContainer) {
        RecipeEditorMod.init();
        if (RecipeEditorMod.isClient()) {
            com.recipeeditor.integration.RecipeViewerIntegration.init();
            com.recipeeditor.inspector.RecipeInspector.startJarScanAsync();
        }
        LOGGER.info("[MR] Recipe Editor (NeoForge) initialized successfully!");
        if (modContainer != null) {
            try {
                modContainer.registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (container, modListScreen) -> new RecipeEditorScreen(modListScreen)
                );
            } catch (Throwable t) {
                LOGGER.warn("Could not register NeoForge config screen: {}", t.getMessage());
            }
        }
    }
}
