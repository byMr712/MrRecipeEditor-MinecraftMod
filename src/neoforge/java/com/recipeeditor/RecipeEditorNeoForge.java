package com.recipeeditor;

import com.recipeeditor.config.RecipeEditorConfig;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("recipeeditor")
public class RecipeEditorNeoForge {
    public static final String MOD_ID = "recipeeditor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RecipeEditorNeoForge() {
        RecipeEditorConfig.getInstance();
        LOGGER.info("[MR] Recipe Editor (NeoForge) initialized successfully!");
    }
}
