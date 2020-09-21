package net.fabricmc.loader.api.entrypoint;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;

/**
 * A mod initializer.
 *
 * <p>In {@code fabric.mod.json}, the entrypoint is defined with {@code main} key.</p>
 *
 * @see ClientModInitializer
 * @see DedicatedServerModInitializer
 * @see net.fabricmc.loader.api.FabricLoader#getEntrypointContainers(String, Class)
 */
@FunctionalInterface
public interface ModInitializer {
	/**
	 * Runs the mod initializer.
	 */
	void onInitialize();
}
