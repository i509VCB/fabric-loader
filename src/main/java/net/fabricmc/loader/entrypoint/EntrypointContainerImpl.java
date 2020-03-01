package net.fabricmc.loader.entrypoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public class EntrypointContainerImpl<T> implements EntrypointContainer<T> {
	private final ModContainer container;
	private List<T> entrypoints = new ArrayList<>();
	private boolean frozen;

	public EntrypointContainerImpl(ModContainer container) {
		this.container = container;
	}

	@Override
	public ModContainer getModContainer() {
		return container;
	}

	@Override
	public Collection<T> getEntrypoints() {
		return entrypoints;
	}

	public void add(T entrypoint) {
		if (frozen) {
			return;
		}

		entrypoints.add(entrypoint);
	}

	public void freeze() {
		this.frozen = true;
		entrypoints = Collections.unmodifiableList(entrypoints);
	}
}
