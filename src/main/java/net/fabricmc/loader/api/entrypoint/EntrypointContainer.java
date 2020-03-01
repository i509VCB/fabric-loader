package net.fabricmc.loader.api.entrypoint;

import java.util.Collection;
import net.fabricmc.loader.api.ModContainer;

public interface EntrypointContainer<T> {
	ModContainer getModContainer();

	Collection<T> getEntrypoints();
}
