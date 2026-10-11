package net.fabricmc.loader.api;

import net.fabricmc.api.EnvType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public EnvType getEnvironmentType() {
        return EnvType.CLIENT;
    }

    public Path getConfigDir() {
        return Paths.get("config");
    }

    public Iterable<ModContainer> getAllMods() {
        return Collections.emptyList();
    }

    public Optional<ModContainer> getModContainer(String id) {
        return Optional.empty();
    }
}
