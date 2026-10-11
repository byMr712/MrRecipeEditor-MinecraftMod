package net.fabricmc.loader.api;

import net.fabricmc.loader.api.metadata.ModMetadata;
import java.nio.file.Path;
import java.util.Optional;

public interface ModContainer {
    ModMetadata getMetadata();
    Optional<Path> findPath(String path);
}
