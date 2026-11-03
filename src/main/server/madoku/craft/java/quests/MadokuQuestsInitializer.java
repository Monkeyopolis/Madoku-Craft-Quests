package madoku.craft.java.quests;

import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.module.MadokuStandaloneModule;
import madoku.craft.java.core.module.MadokuStandaloneRuntime;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint for the standalone Quests module. */
public final class MadokuQuestsInitializer implements ModInitializer, MadokuStandaloneModule {
	@Override
	public net.minecraft.resources.Identifier id() {
		return MadokuModuleIds.QUESTS;
	}

	@Override
	public void onInitialize() {
		MadokuStandaloneRuntime.initialize(this);
	}

	@Override
	public void initialize() {
		QuestsStateManager.initialize();
	}

	@Override
	public void reset() {
		QuestsStateManager.reset();
	}

	@Override
	public void onServerStarted(net.minecraft.server.MinecraftServer server) {
		QuestsStateManager.onServerStarted(server);
	}

	@Override
	public void onServerTick(net.minecraft.server.MinecraftServer server) {
		QuestsStateManager.onServerTick(server);
	}

	@Override
	public void onServerStopped(net.minecraft.server.MinecraftServer server) {
		QuestsStateManager.onServerStopped();
	}
}
