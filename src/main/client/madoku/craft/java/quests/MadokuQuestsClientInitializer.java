package madoku.craft.java.quests;

import madoku.craft.java.core.module.MadokuClientRuntime;
import madoku.craft.java.core.module.MadokuModule;
import madoku.craft.java.core.module.MadokuModuleIds;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint for the standalone Quests module. */
public final class MadokuQuestsClientInitializer implements ClientModInitializer, MadokuModule {
	@Override
	public net.minecraft.resources.Identifier id() {
		return MadokuModuleIds.QUESTS;
	}

	@Override
	public void onInitializeClient() {
		MadokuClientRuntime.initialize(this);
	}

	@Override
	public void initialize() {
		MadokuQuestsClient.initialize();
	}
}
