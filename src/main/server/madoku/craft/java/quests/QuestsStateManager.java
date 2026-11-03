package madoku.craft.java.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.java.core.data.PlayerDataAPIManager;
import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.module.MadokuModuleLifecycleManager;
import madoku.craft.java.core.sync.SyncPlayerAPIManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Owns persisted player quest inventories and the once-per-day quest award. */
final class QuestsStateManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestsStateManager.class);
	private static final String SYSTEM_ID = "quests";
	private static final String LAST_ASSIGNED_DAY = "last-assigned-day";
	private static final String QUESTS = "quests";
	private static final String CATEGORY = "category";
	private static final String DIFFICULTY = "difficulty";
	private static final int MAX_QUESTS_PER_CATEGORY = 7;
	private static final long MINECRAFT_TICKS_PER_DAY = 24_000L;
	private static final String[] DIFFICULTIES = {"easy", "medium", "hard"};
	private static final List<CategoryDefinition> CATEGORY_DEFINITIONS = List.of(
		new CategoryDefinition("core", MadokuModuleIds.CORE),
		new CategoryDefinition("farming", MadokuModuleIds.FARMING),
		new CategoryDefinition("mobs", MadokuModuleIds.MOBS),
		new CategoryDefinition("pets", MadokuModuleIds.PETS)
	);
	private static final Map<UUID, PlayerQuestState> STATES_BY_PLAYER = new HashMap<>();
	private static long lastProcessedDay = Long.MIN_VALUE;
	private static boolean initialized;

	private QuestsStateManager() {
	}

	static void initialize() {
		if (initialized) return;
		PayloadTypeRegistry.clientboundPlay().register(
			QuestsPayloadAPIManager.TYPE,
			QuestsPayloadAPIManager.CODEC
		);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> prepareAndSync(handler.player, server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler != null && handler.player != null) {
				STATES_BY_PLAYER.remove(handler.player.getUUID());
			}
		});
		initialized = true;
	}

	static void reset() {
		STATES_BY_PLAYER.clear();
		lastProcessedDay = Long.MIN_VALUE;
	}

	static void onServerStarted(MinecraftServer server) {
		STATES_BY_PLAYER.clear();
		lastProcessedDay = server == null || server.overworld() == null ? Long.MIN_VALUE : currentDay(server);
		if (server == null) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			prepareAndSync(player, server);
		}
	}

	static void onServerTick(MinecraftServer server) {
		if (server == null || server.overworld() == null) return;
		long currentDay = currentDay(server);
		if (currentDay == lastProcessedDay) return;
		lastProcessedDay = currentDay;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerQuestState state = STATES_BY_PLAYER.get(player.getUUID());
			if (state == null) {
				state = loadState(player);
				STATES_BY_PLAYER.put(player.getUUID(), state);
			}
			if (updateForCurrentDay(player, state, currentDay, server) || state.needsInitialSync()) {
				persistState(player, state);
				state.markSynchronized();
				send(player, state, currentDay);
			}
		}
	}

	static void onServerStopped() {
		STATES_BY_PLAYER.clear();
		lastProcessedDay = Long.MIN_VALUE;
	}

	private static void prepareAndSync(ServerPlayer player, MinecraftServer server) {
		if (player == null || server == null || server.overworld() == null) return;
		long currentDay = currentDay(server);
		PlayerQuestState state = STATES_BY_PLAYER.computeIfAbsent(player.getUUID(), ignored -> loadState(player));
		if (updateForCurrentDay(player, state, currentDay, server)) {
			persistState(player, state);
		}
		state.markSynchronized();
		send(player, state, currentDay);
	}

	private static boolean updateForCurrentDay(
		ServerPlayer player,
		PlayerQuestState state,
		long currentDay,
		MinecraftServer server
	) {
		boolean firstQuestChanged = ensureFirstQuestIsMain(state);
		if (state.lastAssignedDay() == Long.MIN_VALUE) {
			if (state.quests().isEmpty()) awardInitialMainQuest(player, state, currentDay, server);
			state.lastAssignedDay(currentDay);
			return true;
		}
		if (currentDay <= state.lastAssignedDay()) return firstQuestChanged;

		long firstMissedDay = state.lastAssignedDay() + 1L;
		for (long day = firstMissedDay; day <= currentDay; day++) {
			awardQuest(player, state, day, server);
			if (isFull(state)) break;
		}
		state.lastAssignedDay(currentDay);
		return true;
	}

	private static boolean ensureFirstQuestIsMain(PlayerQuestState state) {
		if (state.quests().isEmpty()) return false;
		CategoryDefinition core = CATEGORY_DEFINITIONS.get(0);
		if (!MadokuModuleLifecycleManager.server().isRunning(core.moduleId())
			|| core.id().equals(state.quests().get(0).categoryId())) {
			return false;
		}
		QuestRecord firstQuest = state.quests().get(0);
		state.quests().set(0, new QuestRecord(core.id(), firstQuest.difficulty()));
		return true;
	}

	private static boolean awardInitialMainQuest(
		ServerPlayer player,
		PlayerQuestState state,
		long day,
		MinecraftServer server
	) {
		CategoryDefinition core = CATEGORY_DEFINITIONS.get(0);
		if (!MadokuModuleLifecycleManager.server().isRunning(core.moduleId())
			|| count(state, core.id()) >= MAX_QUESTS_PER_CATEGORY) {
			return false;
		}
		Random random = new Random(questSeed(player, day, server, 0x4D41494EL));
		state.quests().add(new QuestRecord(core.id(), DIFFICULTIES[random.nextInt(DIFFICULTIES.length)]));
		return true;
	}

	private static boolean awardQuest(ServerPlayer player, PlayerQuestState state, long day, MinecraftServer server) {
		List<CategoryDefinition> available = activeCategoriesWithRoom(state);
		if (available.isEmpty()) return false;
		Random random = new Random(questSeed(player, day, server, 0L));
		CategoryDefinition category = available.get(random.nextInt(available.size()));
		String difficulty = DIFFICULTIES[random.nextInt(DIFFICULTIES.length)];
		state.quests().add(new QuestRecord(category.id(), difficulty));
		return true;
	}

	private static long questSeed(ServerPlayer player, long day, MinecraftServer server, long salt) {
		return player.getUUID().getMostSignificantBits()
			^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17)
			^ Long.rotateLeft(server.overworld().getSeed(), 29)
			^ day * 0x9E3779B97F4A7C15L
			^ salt;
	}

	private static List<CategoryDefinition> activeCategoriesWithRoom(PlayerQuestState state) {
		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.server();
		List<CategoryDefinition> available = new ArrayList<>();
		for (CategoryDefinition definition : CATEGORY_DEFINITIONS) {
			if (lifecycle.isRunning(definition.moduleId()) && count(state, definition.id()) < MAX_QUESTS_PER_CATEGORY) {
				available.add(definition);
			}
		}
		return available;
	}

	private static int count(PlayerQuestState state, String categoryId) {
		int count = 0;
		for (QuestRecord quest : state.quests()) {
			if (categoryId.equals(quest.categoryId())) count++;
		}
		return count;
	}

	private static boolean isFull(PlayerQuestState state) {
		return activeCategoriesWithRoom(state).isEmpty();
	}

	private static PlayerQuestState loadState(ServerPlayer player) {
		long lastAssignedDay = Long.MIN_VALUE;
		List<QuestRecord> quests = new ArrayList<>();
		try {
			JsonObject aggregate = PlayerDataAPIManager.getSystemDataForPlayer(player, SYSTEM_ID, "players", "uuid");
			JsonArray entries = aggregate.getAsJsonArray("players");
			if (entries != null && !entries.isEmpty() && entries.get(0).isJsonObject()) {
				JsonObject data = entries.get(0).getAsJsonObject();
				lastAssignedDay = readLong(data, LAST_ASSIGNED_DAY, Long.MIN_VALUE);
				JsonElement questElement = data.get(QUESTS);
				if (questElement != null && questElement.isJsonArray()) {
					for (JsonElement element : questElement.getAsJsonArray()) {
						if (!element.isJsonObject() || quests.size() >= MAX_QUESTS_PER_CATEGORY * CATEGORY_DEFINITIONS.size()) continue;
						JsonObject quest = element.getAsJsonObject();
						String category = readString(quest, CATEGORY, "");
						String difficulty = readString(quest, DIFFICULTY, DIFFICULTIES[0]);
						if (!isKnownCategory(category) || !isKnownDifficulty(difficulty)) continue;
						if (count(quests, category) >= MAX_QUESTS_PER_CATEGORY) continue;
						quests.add(new QuestRecord(category, difficulty));
					}
				}
			}
		} catch (RuntimeException exception) {
			LOGGER.warn("Failed to load the persisted quest state for {}.", player.getUUID(), exception);
		}
		return new PlayerQuestState(lastAssignedDay, quests);
	}

	private static void persistState(ServerPlayer player, PlayerQuestState state) {
		JsonObject data = new JsonObject();
		data.addProperty(LAST_ASSIGNED_DAY, state.lastAssignedDay());
		JsonArray quests = new JsonArray();
		for (QuestRecord quest : state.quests()) {
			JsonObject entry = new JsonObject();
			entry.addProperty(CATEGORY, quest.categoryId());
			entry.addProperty(DIFFICULTY, quest.difficulty());
			quests.add(entry);
		}
		data.add(QUESTS, quests);
		PlayerDataAPIManager.setSystemDataForPlayer(player, SYSTEM_ID, data);
	}

	private static void send(ServerPlayer player, PlayerQuestState state, long currentDay) {
		SyncPlayerAPIManager.send(player, new QuestsPayloadAPIManager(currentDay, encodeQuests(state.quests())));
	}

	private static String encodeQuests(List<QuestRecord> quests) {
		StringBuilder encoded = new StringBuilder();
		for (QuestRecord quest : quests) {
			if (encoded.length() > 0) encoded.append(';');
			encoded.append(quest.categoryId()).append('=').append(quest.difficulty());
		}
		return encoded.toString();
	}

	private static long currentDay(MinecraftServer server) {
		return Math.floorDiv(server.overworld().getOverworldClockTime(), MINECRAFT_TICKS_PER_DAY);
	}

	private static boolean isKnownCategory(String categoryId) {
		return CATEGORY_DEFINITIONS.stream().anyMatch(definition -> definition.id().equals(categoryId));
	}

	private static boolean isKnownDifficulty(String difficulty) {
		for (String value : DIFFICULTIES) if (value.equals(difficulty)) return true;
		return false;
	}

	private static int count(List<QuestRecord> quests, String categoryId) {
		int count = 0;
		for (QuestRecord quest : quests) if (categoryId.equals(quest.categoryId())) count++;
		return count;
	}

	private static String readString(JsonObject source, String key, String fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsString() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static long readLong(JsonObject source, String key, long fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsLong() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private record CategoryDefinition(String id, net.minecraft.resources.Identifier moduleId) {
	}

	private record QuestRecord(String categoryId, String difficulty) {
	}

	private static final class PlayerQuestState {
		private long lastAssignedDay;
		private final List<QuestRecord> quests;
		private boolean synchronizedState;

		private PlayerQuestState(long lastAssignedDay, List<QuestRecord> quests) {
			this.lastAssignedDay = lastAssignedDay;
			this.quests = quests;
		}

		private long lastAssignedDay() { return lastAssignedDay; }
		private void lastAssignedDay(long value) { lastAssignedDay = value; }
		private List<QuestRecord> quests() { return quests; }
		private boolean needsInitialSync() { return !synchronizedState; }
		private void markSynchronized() { synchronizedState = true; }
	}
}
