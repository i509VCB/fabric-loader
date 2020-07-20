package net.fabricmc.loader;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.metadata.LoaderModMetadata;

public abstract class AbstractModContainer implements ModContainer {
	private final LoaderModMetadata metadata;

	protected AbstractModContainer(LoaderModMetadata metadata) {
		this.metadata = metadata;
	}

	abstract void setupRootPath();

	@Override
	public final LoaderModMetadata getMetadata() {
		return this.metadata;
	}
}
