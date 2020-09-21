package net.fabricmc.loader.api.minecraft.metadata;

import net.fabricmc.loader.api.metadata.GameMetadata;
import net.fabricmc.loader.api.metadata.ModEnvironment;

public interface MinecraftMetadata extends GameMetadata {
	ModEnvironment getModEnvironment();
}
