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

	public record TurnInPayload(String categoryId, int questIndex) implements CustomPacketPayload {
		public static final Type<TurnInPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("madoku-craft", "quests_turn_in"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TurnInPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, TurnInPayload::categoryId,
			ByteBufCodecs.VAR_INT, TurnInPayload::questIndex,
			TurnInPayload::new
		);

		public TurnInPayload {
			if (categoryId == null) categoryId = "";
		}

		@Override
		public Type<TurnInPayload> type() {
			return TYPE;
		}
	}
}
