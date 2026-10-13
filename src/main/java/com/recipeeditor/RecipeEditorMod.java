package com.recipeeditor;

import com.mojang.serialization.MapCodec;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.recipe.CustomDynamicCraftingRecipe;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RecipeEditorMod {
    public static final String MOD_ID = "recipeeditor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Boolean cachedDedicatedServer = null;

    public static boolean isDedicatedServer() {
        if (cachedDedicatedServer != null) return cachedDedicatedServer;
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = flClass.getMethod("getInstance").invoke(null);
            Object env = flClass.getMethod("getEnvironmentType").invoke(instance);
            cachedDedicatedServer = "SERVER".equals(String.valueOf(env));
            return cachedDedicatedServer;
        } catch (Throwable ignored) {}
        try {
            Class<?> fmlClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object dist = fmlClass.getMethod("getDist").invoke(null);
            cachedDedicatedServer = dist != null && dist.toString().contains("SERVER");
            return cachedDedicatedServer;
        } catch (Throwable ignored) {}
        cachedDedicatedServer = false;
        return false;
    }

    public static boolean isClient() {
        return !isDedicatedServer();
    }

    public static boolean isModLoaded(String modId) {
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = flClass.getMethod("getInstance").invoke(null);
            Object res = flClass.getMethod("isModLoaded", String.class).invoke(instance, modId);
            if (res instanceof Boolean b) return b;
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                Object res = modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
                if (res instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static Path getConfigDir() {
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = flClass.getMethod("getInstance").invoke(null);
            Object res = flClass.getMethod("getConfigDir").invoke(instance);
            if (res instanceof Path p) return p;
        } catch (Throwable ignored) {}
        try {
            Class<?> fmlPaths = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object configDirEnum = Enum.valueOf((Class<Enum>) fmlPaths.asSubclass(Enum.class), "CONFIGDIR");
            Object res = fmlPaths.getMethod("get").invoke(configDirEnum);
            if (res instanceof Path p) return p;
        } catch (Throwable ignored) {}
        return Paths.get("config");
    }

    public static Optional<String> getModDisplayName(String namespace) {
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = flClass.getMethod("getInstance").invoke(null);
            Object optContainer = flClass.getMethod("getModContainer", String.class).invoke(instance, namespace);
            if (optContainer instanceof Optional<?> opt && opt.isPresent()) {
                Object container = opt.get();
                Object meta = container.getClass().getMethod("getMetadata").invoke(container);
                Object name = meta.getClass().getMethod("getName").invoke(meta);
                if (name instanceof String s && !s.isEmpty()) return Optional.of(s);
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                Object optContainer = modListClass.getMethod("getModContainerById", String.class).invoke(modList, namespace);
                if (optContainer instanceof Optional<?> opt && opt.isPresent()) {
                    Object container = opt.get();
                    Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
                    Object displayName = modInfo.getClass().getMethod("getDisplayName").invoke(modInfo);
                    if (displayName instanceof String s && !s.isEmpty()) return Optional.of(s);
                }
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    public static List<Path> getAllModSubPaths(String subFolder) {
        List<Path> result = new ArrayList<>();
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = flClass.getMethod("getInstance").invoke(null);
            Object modsObj = flClass.getMethod("getAllMods").invoke(instance);
            if (modsObj instanceof Collection<?> mods) {
                for (Object mod : mods) {
                    try {
                        Object optPath = mod.getClass().getMethod("findPath", String.class).invoke(mod, subFolder);
                        if (optPath instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof Path p) {
                            result.add(p);
                        }
                    } catch (Throwable ignored) {}
                }
                return result;
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                Object modFilesObj = modListClass.getMethod("getModFiles").invoke(modList);
                if (modFilesObj instanceof Iterable<?> modFiles) {
                    for (Object mfInfo : modFiles) {
                        try {
                            Object file = mfInfo.getClass().getMethod("getFile").invoke(mfInfo);
                            Object pObj = file.getClass().getMethod("findResource", String[].class).invoke(file, (Object) new String[]{subFolder});
                            if (pObj instanceof Path p && java.nio.file.Files.exists(p)) {
                                result.add(p);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
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
                        private final PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> PACKET_CODEC = PacketCodec
                                .unit(new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC));

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
                private final PacketCodec<RegistryByteBuf, CustomDynamicCraftingRecipe> PACKET_CODEC = PacketCodec
                        .unit(new CustomDynamicCraftingRecipe(CraftingRecipeCategory.MISC));

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

    public static void init() {
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
