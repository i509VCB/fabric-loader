package net.fabricmc.loader.api.entrypoint.server;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;

/**
 * A mod initializer ran only on {@link EnvType#SERVER}.
 *
 * <p>In {@code fabric.mod.json}, the entrypoint is defined with {@code server} key.</p>
 *
 * @see ModInitializer
 * @see ClientModInitializer
 * @see net.fabricmc.loader.api.FabricLoader#getEntrypointContainers(String, Class)
 */
@FunctionalInterface
public interface DedicatedServerModInitializer {
	/**
	 * Runs the mod initializer on the server environment.
	 */
	void onInitializeServer();
}
