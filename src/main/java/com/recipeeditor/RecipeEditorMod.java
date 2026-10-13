package com.recipeeditor;

import com.mojang.serialization.MapCodec;
import com.recipeeditor.config.RecipeEditorConfig;
import com.recipeeditor.recipe.CustomDynamicCraftingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeEditorMod {
    public static final String MOD_ID = "recipeeditor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static volatile Boolean cachedDedicatedServer;

    public static boolean isDedicatedServer() {
        Boolean cached = cachedDedicatedServer;
        if (cached != null) return cached;
        boolean result = false;
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fl = flClass.getMethod("getInstance").invoke(null);
            Object env = flClass.getMethod("getEnvironmentType").invoke(fl);
            result = env != null && "SERVER".equals(env.toString());
            cachedDedicatedServer = result;
            return result;
        } catch (Throwable ignored) {}
        try {
            Class<?> fmlClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object dist = fmlClass.getMethod("getDist").invoke(null);
            result = dist != null && "DEDICATED_SERVER".equals(dist.toString());
            cachedDedicatedServer = result;
            return result;
        } catch (Throwable ignored) {}
        cachedDedicatedServer = false;
        return false;
    }

    public static boolean isClient() {
        return !isDedicatedServer();
    }

    public static boolean isModLoaded(String modId) {
        if (modId == null || modId.isEmpty()) return false;
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fl = flClass.getMethod("getInstance").invoke(null);
            return (Boolean) flClass.getMethod("isModLoaded", String.class).invoke(fl, modId);
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                return (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.nio.file.Path getConfigDir() {
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fl = flClass.getMethod("getInstance").invoke(null);
            Object dir = flClass.getMethod("getConfigDir").invoke(fl);
            if (dir instanceof java.nio.file.Path p) return p;
        } catch (Throwable ignored) {}
        try {
            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object configDirEnum = Enum.valueOf((Class<Enum>) fmlPathsClass.asSubclass(Enum.class), "CONFIGDIR");
            Object dir = fmlPathsClass.getMethod("get").invoke(configDirEnum);
            if (dir instanceof java.nio.file.Path p) return p;
        } catch (Throwable ignored) {}
        return java.nio.file.Paths.get("config");
    }

    public static java.util.Optional<String> getModDisplayName(String namespace) {
        if (namespace == null || namespace.isEmpty()) return java.util.Optional.empty();
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fl = flClass.getMethod("getInstance").invoke(null);
            java.util.Optional<?> opt = (java.util.Optional<?>) flClass.getMethod("getModContainer", String.class).invoke(fl, namespace);
            if (opt != null && opt.isPresent()) {
                Object container = opt.get();
                Object meta = container.getClass().getMethod("getMetadata").invoke(container);
                String name = (String) meta.getClass().getMethod("getName").invoke(meta);
                if (name != null && !name.isEmpty()) return java.util.Optional.of(name);
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                java.util.Optional<?> opt = (java.util.Optional<?>) modListClass.getMethod("getModContainerById", String.class).invoke(modList, namespace);
                if (opt != null && opt.isPresent()) {
                    Object container = opt.get();
                    Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
                    String name = (String) modInfo.getClass().getMethod("getDisplayName").invoke(modInfo);
                    if (name != null && !name.isEmpty()) return java.util.Optional.of(name);
                }
            }
        } catch (Throwable ignored) {}
        return java.util.Optional.empty();
    }

    public static java.util.List<java.nio.file.Path> getAllModSubPaths(String subFolder) {
        java.util.List<java.nio.file.Path> paths = new java.util.ArrayList<>();
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fl = flClass.getMethod("getInstance").invoke(null);
            java.util.Collection<?> mods = (java.util.Collection<?>) flClass.getMethod("getAllMods").invoke(fl);
            if (mods != null) {
                for (Object mod : mods) {
                    try {
                        java.util.Optional<?> opt = (java.util.Optional<?>) mod.getClass().getMethod("findPath", String.class).invoke(mod, subFolder);
                        if (opt != null && opt.isPresent() && opt.get() instanceof java.nio.file.Path p) {
                            paths.add(p);
                        }
                    } catch (Throwable ignored) {}
                }
            }
            return paths;
        } catch (Throwable ignored) {}
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                java.util.List<?> modFiles = (java.util.List<?>) modListClass.getMethod("getModFiles").invoke(modList);
                if (modFiles != null) {
                    for (Object mfInfo : modFiles) {
                        try {
                            Object modFile = mfInfo.getClass().getMethod("getFile").invoke(mfInfo);
                            Object res = modFile.getClass().getMethod("findResource", String[].class).invoke(modFile, (Object) new String[]{subFolder});
                            if (res instanceof java.nio.file.Path p && java.nio.file.Files.exists(p)) {
                                paths.add(p);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
        return paths;
    }

    public static final RecipeSerializer<CustomDynamicCraftingRecipe> CUSTOM_CRAFTING_SERIALIZER;
    static {
        RecipeSerializer<CustomDynamicCraftingRecipe> serializer;
        try {
            serializer = Registry.register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    Identifier.fromNamespaceAndPath(MOD_ID, "custom_crafting"),
                    new RecipeSerializer<>(
                            MapCodec.unit(() -> new CustomDynamicCraftingRecipe(CraftingBookCategory.MISC)),
                            StreamCodec.unit(new CustomDynamicCraftingRecipe(CraftingBookCategory.MISC))
                    )
            );
        } catch (Throwable t) {
            serializer = new RecipeSerializer<>(
                    MapCodec.unit(() -> new CustomDynamicCraftingRecipe(CraftingBookCategory.MISC)),
                    StreamCodec.unit(new CustomDynamicCraftingRecipe(CraftingBookCategory.MISC))
            );
        }
        CUSTOM_CRAFTING_SERIALIZER = serializer;
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
        ensureItemComponentsBound();
    }

    public static void ensureItemComponentsBound() {
        try {
            if (!net.minecraft.world.item.Items.STONE.builtInRegistryHolder().areComponentsBound()) {
                net.minecraft.core.HolderLookup.Provider baseProvider = net.minecraft.core.HolderLookup.Provider.create(
                        BuiltInRegistries.REGISTRY.stream().map(r -> (net.minecraft.core.HolderLookup.RegistryLookup<?>) r)
                );
                net.minecraft.core.HolderLookup.Provider safeProvider = new net.minecraft.core.HolderLookup.Provider() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> java.util.Optional<net.minecraft.core.HolderLookup.RegistryLookup<T>> lookup(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> key) {
                        java.util.Optional<net.minecraft.core.HolderLookup.RegistryLookup<T>> res =
                                (java.util.Optional<net.minecraft.core.HolderLookup.RegistryLookup<T>>) (java.util.Optional<?>) baseProvider.lookup(key);
                        if (res.isPresent()) return res;
                        return java.util.Optional.of(new net.minecraft.core.HolderLookup.RegistryLookup<T>() {
                            @Override
                            public net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> key() {
                                return key;
                            }

                            @Override
                            public com.mojang.serialization.Lifecycle registryLifecycle() {
                                return com.mojang.serialization.Lifecycle.stable();
                            }

                            @Override
                            public java.util.stream.Stream<net.minecraft.core.HolderSet.Named<T>> listTags() {
                                return java.util.stream.Stream.empty();
                            }

                            @Override
                            public java.util.stream.Stream<net.minecraft.core.Holder.Reference<T>> listElements() {
                                return java.util.stream.Stream.empty();
                            }

                            @Override
                            public java.util.Optional<net.minecraft.core.Holder.Reference<T>> get(net.minecraft.resources.ResourceKey<T> resourceKey) {
                                return java.util.Optional.of(net.minecraft.core.Holder.Reference.createStandAlone(new net.minecraft.core.HolderOwner<T>() {}, resourceKey));
                            }

                            @Override
                            public java.util.Optional<net.minecraft.core.HolderSet.Named<T>> get(net.minecraft.tags.TagKey<T> tagKey) {
                                return java.util.Optional.of(net.minecraft.core.HolderSet.emptyNamed(new net.minecraft.core.HolderOwner<T>() {}, tagKey));
                            }
                        });
                    }

                    @Override
                    public java.util.stream.Stream<net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>>> listRegistryKeys() {
                        return baseProvider.listRegistryKeys();
                    }

                    @Override
                    public <T> java.util.Optional<net.minecraft.core.HolderSet.Named<T>> get(net.minecraft.tags.TagKey<T> tagKey) {
                        java.util.Optional<net.minecraft.core.HolderSet.Named<T>> res = baseProvider.get(tagKey);
                        if (res.isPresent()) return res;
                        return java.util.Optional.of(net.minecraft.core.HolderSet.emptyNamed(new net.minecraft.core.HolderOwner<T>() {}, tagKey));
                    }

                    @Override
                    public <T> net.minecraft.core.HolderSet.Named<T> getOrThrow(net.minecraft.tags.TagKey<T> tagKey) {
                        return get(tagKey).get();
                    }

                    @Override
                    public <T> java.util.Optional<net.minecraft.core.Holder.Reference<T>> get(net.minecraft.resources.ResourceKey<T> resourceKey) {
                        java.util.Optional<net.minecraft.core.Holder.Reference<T>> res = baseProvider.get(resourceKey);
                        if (res.isPresent()) return res;
                        return java.util.Optional.of(net.minecraft.core.Holder.Reference.createStandAlone(new net.minecraft.core.HolderOwner<T>() {}, resourceKey));
                    }

                    @Override
                    public <T> net.minecraft.core.Holder.Reference<T> getOrThrow(net.minecraft.resources.ResourceKey<T> resourceKey) {
                        return get(resourceKey).get();
                    }
                };
                java.util.List<net.minecraft.core.component.DataComponentInitializers.PendingComponents<?>> pending =
                        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(safeProvider);
                for (net.minecraft.core.component.DataComponentInitializers.PendingComponents<?> p : pending) {
                    p.apply();
                }
                LOGGER.info("Successfully bound default item data components for title screen menu");
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to bind item components: " + t.getMessage(), t);
        }
    }
}
