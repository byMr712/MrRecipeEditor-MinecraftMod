package net.fabricmc.loader.api;

import net.fabricmc.api.EnvType;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface FabricLoader {
    static FabricLoader getInstance() {
        return Holder.INSTANCE;
    }

    EnvType getEnvironmentType();

    Path getConfigDir();

    default File getConfigDirectory() {
        return getConfigDir().toFile();
    }

    default Path getGameDir() {
        return Paths.get(".");
    }

    default File getGameDirectory() {
        return new File(".");
    }

    Collection<ModContainer> getAllMods();

    Optional<ModContainer> getModContainer(String id);

    default boolean isModLoaded(String id) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, id);
        } catch (Throwable ignored) {
            return false;
        }
    }

    default boolean isDevelopmentEnvironment() {
        return false;
    }

    class Holder {
        private static final FabricLoader INSTANCE = new FabricLoader() {
            @Override
            public EnvType getEnvironmentType() {
                return EnvType.CLIENT;
            }

            @Override
            public Path getConfigDir() {
                return Paths.get("config");
            }

            @Override
            public Collection<ModContainer> getAllMods() {
                return Collections.emptyList();
            }

            @Override
            public Optional<ModContainer> getModContainer(String id) {
                try {
                    Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
                    Object modList = modListClass.getMethod("get").invoke(null);
                    @SuppressWarnings("unchecked")
                    Optional<?> opt = (Optional<?>) modListClass.getMethod("getModContainerById", String.class).invoke(modList, id);
                    if (opt != null && opt.isPresent()) {
                        Object container = opt.get();
                        Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
                        String name = (String) modInfo.getClass().getMethod("getDisplayName").invoke(modInfo);
                        return Optional.of(new ModContainer() {
                            @Override
                            public net.fabricmc.loader.api.metadata.ModMetadata getMetadata() {
                                return new net.fabricmc.loader.api.metadata.ModMetadata() {
                                    @Override
                                    public String getId() { return id; }
                                    @Override
                                    public String getName() { return name != null ? name : id; }
                                };
                            }
                            @Override
                            public Optional<Path> findPath(String path) {
                                return Optional.empty();
                            }
                        });
                    }
                } catch (Throwable ignored) {}
                return Optional.empty();
            }
        };
    }
}
