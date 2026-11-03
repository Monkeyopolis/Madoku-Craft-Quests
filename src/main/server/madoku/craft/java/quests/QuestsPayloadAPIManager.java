package madoku.craft.java.quests;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Clientbound snapshot of a player's current quest inventory. */
public record QuestsPayloadAPIManager(long day, String quests) implements CustomPacketPayload {
	public static final Type<QuestsPayloadAPIManager> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath("madoku-craft", "quests_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuestsPayloadAPIManager> CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_LONG,
			QuestsPayloadAPIManager::day,
			ByteBufCodecs.STRING_UTF8,
			QuestsPayloadAPIManager::quests,
			QuestsPayloadAPIManager::new
		);

	public QuestsPayloadAPIManager {
		if (quests == null) {
			quests = "";
		}
	}

	@Override
	public Type<QuestsPayloadAPIManager> type() {
		return TYPE;
	}
}
