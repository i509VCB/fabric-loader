package me.i509.spihacks.launch;

import net.fabricmc.loader.api.ModContainer;
import org.spongepowered.plugin.PluginCandidate;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.util.jar.Manifest;

public final class FabricPluginContainer implements PluginContainer {
	private final ModContainer modContainer;
	private final PluginCandidate artifact;

	private FabricPluginContainer(ModContainer modContainer) {
		this.modContainer = modContainer;
		final PluginMetadata pluginMetadata = FabricMetadataUtil.createSpongeMetadata(modContainer.getMetadata());
		final Manifest manifest = new Manifest();
		// TODO: Populate the manifest
		this.artifact = PluginCandidate.of(pluginMetadata, modContainer.getRootPath(), manifest);
	}

	public static FabricPluginContainer of(ModContainer modContainer) {
		return new FabricPluginContainer(modContainer);
	}

	@Override
	public PluginCandidate getCandidate() {
		return this.artifact;
	}

	@Override
	public Object getHandle() {
		return null; // Fabric has no handles. Unless Fabric's own `ModContainer` could count?
	}

	public ModContainer getModContainer() {
		return this.modContainer;
	}
}
