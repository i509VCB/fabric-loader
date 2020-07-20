package net.fabricmc.loader;

import net.fabricmc.loader.metadata.LoaderModMetadata;

import java.nio.file.Path;

public class SimpleModContainer extends AbstractModContainer {
	private final Path rootPath;

	public SimpleModContainer(LoaderModMetadata metadata, Path rootPath) {
		super(metadata);
		this.rootPath = rootPath;
	}

	@Override
	public Path getRootPath() {
		return this.rootPath;
	}

	@Override
	void setupRootPath() {
		// TODO: Do we need to setup?
	}
}
