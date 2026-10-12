package com.recipeeditor;

import com.mojang.serialization.MapCodec;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.recipe.CustomDynamicCraftingRecipe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeEditorMod implements ModInitializer {
    public static final String MOD_ID = "recipeeditor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isDedicatedServer() {
        try {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        } catch (Throwable t) {
            return false;
        }
    }

    public static final RecipeSerializer<CustomDynamicCraftingRecipe> RECIPE_SERIALIZER;
    static {
        RecipeSerializer<CustomDynamicCraftingRecipe> ser;
        try {
            ser = Registry.register(
                    Registries.RECIPE_SERIALIZER,
                    Identifier.of(MOD_ID, "custom_crafting"),
                    new RecipeSerializer<CustomDynamicCraftingRecipe>() {
                        private final MapCodec<CustomDynamicCraftingRecipe> CODEC = MapCodec
                                .unit(() -> new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC));
                        private final PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> PACKET_CODEC = new PacketCodec<>() {
                            @Override
                            public CustomDynamicCraftingRecipe decode(RegistryByteBuf buf) {
                                return new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC);
                            }

                            @Override
                            public void encode(RegistryByteBuf buf, CustomDynamicCraftingRecipe value) {
                            }
                        };

                        @Override
                        public MapCodec<CustomDynamicCraftingRecipe> codec() {
                            return CODEC;
                        }

                        @Override
                        public PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> packetCodec() {
                            return PACKET_CODEC;
                        }
                    });
        } catch (Throwable t) {
            ser = new RecipeSerializer<CustomDynamicCraftingRecipe>() {
                private final MapCodec<CustomDynamicCraftingRecipe> CODEC = MapCodec
                        .unit(() -> new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC));
                private final PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> PACKET_CODEC = new PacketCodec<>() {
                    @Override
                    public CustomDynamicCraftingRecipe decode(RegistryByteBuf buf) {
                        return new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC);
                    }

                    @Override
                    public void encode(RegistryByteBuf buf, CustomDynamicCraftingRecipe value) {
                    }
                };

                @Override
                public MapCodec<CustomDynamicCraftingRecipe> codec() {
                    return CODEC;
                }

                @Override
                public PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> packetCodec() {
                    return PACKET_CODEC;
                }
            };
        }
        RECIPE_SERIALIZER = ser;
    }

    @Override
    public void onInitialize() {
        if (isDedicatedServer()) {
            System.out.println(" ==========[MR Recipe Editor]============");
            System.out.println("Notice: MR Recipe Editor has detected that it is running on a server; unfortunately, this is not supported.");
            System.out.println("The mod will not work, but this will not affect your server's startup — enjoy the game!");
            System.out.println(" ==========[MR Recipe Editor]============");
            LOGGER.info("==========[MR Recipe Editor]============");
            LOGGER.info("Notice: MR Recipe Editor has detected that it is running on a server; unfortunately, this is not supported.");
            LOGGER.info("The mod will not work, but this will not affect your server's startup — enjoy the game!");
            LOGGER.info("==========[MR Recipe Editor]============");
            return;
        }

        RecipeEditorConfig.getInstance();
    }
}
