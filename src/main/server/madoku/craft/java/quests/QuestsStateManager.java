package madoku.craft.java.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.java.core.data.DataSaveParticipant;
import madoku.craft.java.core.data.DataSaveParticipantAPIManager;
import madoku.craft.java.core.data.PlayerDataAPIManager;
import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.module.MadokuModuleLifecycleManager;
import madoku.craft.java.core.essence.EssenceManager;
import madoku.craft.java.core.loot.LootTableAPIManager;
import madoku.craft.java.core.quest.QuestRewardAPIManager;
import madoku.craft.java.core.sync.SyncPlayerAPIManager;
import madoku.craft.java.core.time.TimeAPIManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Owns persisted player quest inventories, assignment, and server-authoritative progress. */
final class QuestsStateManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestsStateManager.class);
	private static final String SYSTEM_ID = "quests";
	private static final String LAST_ASSIGNED_WORLD_TIME = "last-assigned-world-time";
	private static final String QUESTS = "quests";
	private static final String ID = "id";
	private static final String CATEGORY = "category";
	private static final String DIFFICULTY = "difficulty";
	private static final String TITLE_KEY = "title-key";
	private static final String DESCRIPTION_KEY = "description-key";
	private static final String PROGRESS = "progress";
	private static final int MAX_QUESTS_PER_CATEGORY = 7;
	private static final long QUEST_INTERVAL_WORLD_TICKS = TimeAPIManager.MINECRAFT_TICKS_PER_CYCLE;
	private static final long PROGRESS_SYNC_INTERVAL_TICKS = 5L;
	private static final List<CategoryDefinition> CATEGORY_DEFINITIONS = List.of(
		new CategoryDefinition(QuestRegistry.CORE_CATEGORY, MadokuModuleIds.CORE),
		new CategoryDefinition(QuestRegistry.FARMING_CATEGORY, MadokuModuleIds.FARMING),
		new CategoryDefinition(QuestRegistry.MOBS_CATEGORY, MadokuModuleIds.MOBS),
		new CategoryDefinition(QuestRegistry.PETS_CATEGORY, MadokuModuleIds.PETS)
	);
	private static final Map<UUID, PlayerQuestState> STATES_BY_PLAYER = new HashMap<>();
	private static final Set<UUID> DIRTY_SYNCS = new HashSet<>();
	private static final Set<UUID> DIRTY_PERSISTENCE = new HashSet<>();
	private static final PriorityQueue<QuestScheduleEntry> ASSIGNMENT_SCHEDULE = new PriorityQueue<>(
		Comparator.comparingLong(QuestScheduleEntry::dueWorldTime)
	);
	private static final Map<UUID, QuestScheduleEntry> SCHEDULE_BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Long> NEXT_PROGRESS_SYNC_TICKS = new HashMap<>();
	private static long lastObservedWorldTime = Long.MIN_VALUE;
	private static boolean initialized;
	private static final DataSaveParticipant DATA_SAVE_PARTICIPANT = new DataSaveParticipant() {
		@Override public String id() { return "quests.players"; }
		@Override public void autosavePersistedData(MinecraftServer server) { QuestsStateManager.autosavePersistedData(server); }
		@Override public void savePersistedData(MinecraftServer server) { QuestsStateManager.savePersistedData(server); }
	};
	private static long lastAutosaveBucket = Long.MIN_VALUE;

	private QuestsStateManager() {
	}

	static void initialize() {
		if (initialized) return;
		PayloadTypeRegistry.clientboundPlay().register(
			QuestsPayloadAPIManager.TYPE,
			QuestsPayloadAPIManager.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
			QuestsPayloadAPIManager.TurnInPayload.TYPE,
			QuestsPayloadAPIManager.TurnInPayload.CODEC
		);
		DataSaveParticipantAPIManager.register(DATA_SAVE_PARTICIPANT);
		ServerPlayNetworking.registerGlobalReceiver(
			QuestsPayloadAPIManager.TurnInPayload.TYPE,
			(payload, context) -> handleTurnIn(context.player(), payload.questId())
		);
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer) onBlockBroken(serverPlayer, state);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> prepareAndSync(handler.player, server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler != null && handler.player != null) {
				UUID playerId = handler.player.getUUID();
				PlayerQuestState state = STATES_BY_PLAYER.get(playerId);
				if (state != null && DIRTY_PERSISTENCE.remove(playerId)) persistState(handler.player, state);
				STATES_BY_PLAYER.remove(playerId);
				DIRTY_SYNCS.remove(playerId);
				removeScheduledAssignment(playerId);
				NEXT_PROGRESS_SYNC_TICKS.remove(playerId);
			}
		});
		initialized = true;
	}

	static void reset() {
		STATES_BY_PLAYER.clear();
		DIRTY_SYNCS.clear();
		DIRTY_PERSISTENCE.clear();
		ASSIGNMENT_SCHEDULE.clear();
		SCHEDULE_BY_PLAYER.clear();
		NEXT_PROGRESS_SYNC_TICKS.clear();
		lastObservedWorldTime = Long.MIN_VALUE;
		lastAutosaveBucket = Long.MIN_VALUE;
	}

	static void loadPersistedData(MinecraftServer server) {
		lastAutosaveBucket = server == null || server.overworld() == null
			? Long.MIN_VALUE
			: autosaveBucket(server);
	}

	static void autosavePersistedData(MinecraftServer server) {
		if (server == null || server.overworld() == null) return;
		long bucket = autosaveBucket(server);
		if (bucket == lastAutosaveBucket) return;
		lastAutosaveBucket = bucket;
		flushDirtyPlayerStates(server);
	}

	static void savePersistedData(MinecraftServer server) {
		if (server == null) return;
		flushDirtyPlayerStates(server);
	}

	static void onServerStarted(MinecraftServer server) {
		STATES_BY_PLAYER.clear();
		DIRTY_SYNCS.clear();
		DIRTY_PERSISTENCE.clear();
		ASSIGNMENT_SCHEDULE.clear();
		SCHEDULE_BY_PLAYER.clear();
		NEXT_PROGRESS_SYNC_TICKS.clear();
		lastObservedWorldTime = server == null || server.overworld() == null ? Long.MIN_VALUE : currentWorldTime(server);
		lastAutosaveBucket = server == null || server.overworld() == null ? Long.MIN_VALUE : autosaveBucket(server);
		if (server == null) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			prepareAndSync(player, server);
		}
	}

	static void onServerTick(MinecraftServer server) {
		if (server == null || server.overworld() == null) return;
		long currentWorldTime = currentWorldTime(server);
		boolean worldTimeRolledBack = lastObservedWorldTime != Long.MIN_VALUE && currentWorldTime < lastObservedWorldTime;
		lastObservedWorldTime = currentWorldTime;
		if (worldTimeRolledBack) {
			rebaseForWorldTimeRollback(server, currentWorldTime);
		}
		processDueAssignments(server, currentWorldTime);
		flushDirtySyncs(server);
	}

	static void onServerStopped(MinecraftServer server) {
		flushDirtyPlayerStates(server);
		STATES_BY_PLAYER.clear();
		DIRTY_SYNCS.clear();
		DIRTY_PERSISTENCE.clear();
		ASSIGNMENT_SCHEDULE.clear();
		SCHEDULE_BY_PLAYER.clear();
		NEXT_PROGRESS_SYNC_TICKS.clear();
		lastObservedWorldTime = Long.MIN_VALUE;
		lastAutosaveBucket = Long.MIN_VALUE;
	}

	static void onItemCollected(ServerPlayer player, Item item, int amount) {
		if (player == null) {
			LOGGER.info("[Quests] Item collection hook received without a player.");
			return;
		}
		if (item == null || amount <= 0) {
			LOGGER.info(
				"[Quests] Item collection hook received invalid data player={} item={} amount={}",
				player.getGameProfile().name(),
				item,
				amount
			);
			return;
		}
		LOGGER.info(
			"[Quests] Item collected player={} item={} amount={}",
			player.getGameProfile().name(),
			BuiltInRegistries.ITEM.getKey(item),
			amount
		);
		PlayerQuestState state = stateFor(player);
		boolean changed = state.addCollected(item, amount);
		LOGGER.info(
			"[Quests] Item collection objective result player={} item={} changed={} state={}",
			player.getGameProfile().name(),
			BuiltInRegistries.ITEM.getKey(item),
			changed,
			describeState(state)
		);
		if (changed) markProgressChanged(player);
	}

	private static void onBlockBroken(ServerPlayer player, BlockState state) {
		if (player == null || state == null) {
			LOGGER.info("[Quests] Block break hook received incomplete data player={} state={}", player, state);
			return;
		}
		LOGGER.info(
			"[Quests] Block break received player={} block={} creative={}",
			player.getGameProfile().name(),
			BuiltInRegistries.BLOCK.getKey(state.getBlock()),
			player.isCreative()
		);
		if (player.isCreative()) {
			LOGGER.info("[Quests] Block break ignored because player={} is creative.", player.getGameProfile().name());
			return;
		}
		PlayerQuestState questState = stateFor(player);
		boolean changed = questState.addBroken(state);
		LOGGER.info(
			"[Quests] Block objective result player={} block={} changed={} state={}",
			player.getGameProfile().name(),
			BuiltInRegistries.BLOCK.getKey(state.getBlock()),
			changed,
			describeState(questState)
		);
		if (changed) markProgressChanged(player);
	}

	private static void handleTurnIn(ServerPlayer player, String questId) {
		String normalizedQuestId = questId == null ? "" : questId.trim().toLowerCase(Locale.ROOT);
		if (player == null || normalizedQuestId.isBlank()) return;
		PlayerQuestState state = stateFor(player);
		QuestRecord quest = state.quest(normalizedQuestId);
		if (quest == null || !quest.isComplete()) return;
		awardRewards(player, quest.difficulty());
		player.level().playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			SoundEvents.PLAYER_LEVELUP,
			SoundSource.PLAYERS,
			1.0F,
			1.0F
		);
		state.removeQuest(quest.definitionId());
		markProgressChanged(player);
		sendImmediately(player, state);
	}

	private static void awardRewards(ServerPlayer player, QuestDifficulty difficulty) {
		if (player == null || difficulty == null) return;
		player.giveExperiencePoints(difficulty.experienceReward());
		QuestRewardAPIManager.addMadokuLevelsXp(player, difficulty.levelsXpReward());
		int essence = difficulty.essenceReward();
		if (essence > 0) {
			giveItemOrDrop(player, new ItemStack(EssenceManager.ESSENCE, essence));
		}

		List<ItemStack> itemRewards = LootTableAPIManager.generateManagedLootForTable(
			"quest-" + difficulty.id() + "-difficulty",
			RandomSource.create()
		);
		for (ItemStack reward : itemRewards) {
			if (reward != null && !reward.isEmpty()) {
				giveItemOrDrop(player, reward);
			}
		}
	}

	private static void giveItemOrDrop(ServerPlayer player, ItemStack reward) {
		if (player == null || reward == null || reward.isEmpty()) return;
		ItemStack remaining = reward.copy();
		if (!player.getInventory().add(remaining) && !remaining.isEmpty()) {
			player.drop(remaining, false, Prediction.SERVER_ONLY);
		}
	}

	private static void prepareAndSync(ServerPlayer player, MinecraftServer server) {
		if (player == null || server == null || server.overworld() == null) return;
		long currentWorldTime = currentWorldTime(server);
		PlayerQuestState state = stateFor(player);
		if (updateForCurrentWorldTime(player, state, currentWorldTime, server)) {
			persistState(player, state);
			DIRTY_PERSISTENCE.remove(player.getUUID());
		}
		state.markSynchronized();
		DIRTY_SYNCS.remove(player.getUUID());
		scheduleNextAssignment(player, state);
		send(player, state);
	}

	private static PlayerQuestState stateFor(ServerPlayer player) {
		return STATES_BY_PLAYER.computeIfAbsent(player.getUUID(), ignored -> loadState(player));
	}

	private static void flushDirtySyncs(MinecraftServer server) {
		if (DIRTY_SYNCS.isEmpty()) return;
		long currentGameTime = server.overworld().getGameTime();
		for (UUID playerId : List.copyOf(DIRTY_SYNCS)) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null) {
				DIRTY_SYNCS.remove(playerId);
				continue;
			}
			long nextSyncTick = NEXT_PROGRESS_SYNC_TICKS.getOrDefault(playerId, Long.MIN_VALUE);
			if (currentGameTime < nextSyncTick) continue;
			PlayerQuestState state = STATES_BY_PLAYER.get(playerId);
			if (state != null) {
				send(player, state);
				NEXT_PROGRESS_SYNC_TICKS.put(playerId, currentGameTime + PROGRESS_SYNC_INTERVAL_TICKS);
			}
			DIRTY_SYNCS.remove(playerId);
		}
	}

	private static void rebaseForWorldTimeRollback(MinecraftServer server, long currentWorldTime) {
		ASSIGNMENT_SCHEDULE.clear();
		SCHEDULE_BY_PLAYER.clear();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerQuestState state = stateFor(player);
			if (updateForCurrentWorldTime(player, state, currentWorldTime, server)) {
				persistState(player, state);
				DIRTY_PERSISTENCE.remove(player.getUUID());
				state.markSynchronized();
				send(player, state);
				DIRTY_SYNCS.remove(player.getUUID());
			}
			scheduleNextAssignment(player, state);
		}
	}

	private static void processDueAssignments(MinecraftServer server, long currentWorldTime) {
		while (!ASSIGNMENT_SCHEDULE.isEmpty()
			&& ASSIGNMENT_SCHEDULE.peek().dueWorldTime() <= currentWorldTime) {
			QuestScheduleEntry schedule = ASSIGNMENT_SCHEDULE.poll();
			if (!schedule.equals(SCHEDULE_BY_PLAYER.get(schedule.playerId()))) continue;
			SCHEDULE_BY_PLAYER.remove(schedule.playerId());
			ServerPlayer player = server.getPlayerList().getPlayer(schedule.playerId());
			if (player == null) continue;
			PlayerQuestState state = STATES_BY_PLAYER.get(schedule.playerId());
			if (state == null || state.lastAssignedWorldTime() == Long.MIN_VALUE) continue;
			if (schedule.cursorWorldTime() != state.lastAssignedWorldTime()
				|| schedule.dueWorldTime() != nextAssignmentWorldTime(state.lastAssignedWorldTime())) {
				continue;
			}
			if (updateForCurrentWorldTime(player, state, currentWorldTime, server)) {
				persistState(player, state);
				DIRTY_PERSISTENCE.remove(player.getUUID());
				state.markSynchronized();
				send(player, state);
				DIRTY_SYNCS.remove(player.getUUID());
			}
			scheduleNextAssignment(player, state);
		}
	}

	private static boolean updateForCurrentWorldTime(
		ServerPlayer player,
		PlayerQuestState state,
		long currentWorldTime,
		MinecraftServer server
	) {
		if (state.lastAssignedWorldTime() == Long.MIN_VALUE) {
			if (state.quests().isEmpty()) awardInitialMainQuest(player, state);
			state.lastAssignedWorldTime(currentWorldTime);
			return true;
		}
		if (currentWorldTime < state.lastAssignedWorldTime()) {
			state.lastAssignedWorldTime(currentWorldTime);
			return true;
		}
		long elapsedWorldTime = currentWorldTime - state.lastAssignedWorldTime();
		long completedWorldDays = elapsedWorldTime / QUEST_INTERVAL_WORLD_TICKS;
		if (completedWorldDays <= 0L) return false;
		for (long day = 1L; day <= completedWorldDays && !isFull(state); day++) {
			long assignmentWorldTime = state.lastAssignedWorldTime() + day * QUEST_INTERVAL_WORLD_TICKS;
			if (!awardQuest(state, assignmentWorldTime, server, player)) break;
		}
		state.lastAssignedWorldTime(currentWorldTime - elapsedWorldTime % QUEST_INTERVAL_WORLD_TICKS);
		return true;
	}

	private static boolean awardInitialMainQuest(ServerPlayer player, PlayerQuestState state) {
		CategoryDefinition core = CATEGORY_DEFINITIONS.get(0);
		QuestDefinition firstMainQuest = QuestRegistry.firstMainQuest();
		if (!MadokuModuleLifecycleManager.server().isRunning(core.moduleId())
			|| firstMainQuest == null
			|| count(state, core.id()) >= MAX_QUESTS_PER_CATEGORY) {
			return false;
		}
		if (state.containsDefinition(firstMainQuest.id())) return false;
		state.addQuest(QuestRecord.create(firstMainQuest, QuestDifficulty.EASY));
		return true;
	}

	private static boolean awardQuest(PlayerQuestState state, long assignmentWorldTime, MinecraftServer server, ServerPlayer player) {
		Set<String> assignedQuestIds = state.definitionIds();
		List<CategoryDefinition> available = activeCategoriesWithRoom(state, assignedQuestIds);
		if (available.isEmpty()) return false;
		Random random = new Random(questSeed(player, assignmentWorldTime, server, 0L));
		CategoryDefinition category = available.get(random.nextInt(available.size()));
		QuestAssignment assignment = QuestRegistry.choose(random, category.id(), assignedQuestIds);
		if (assignment == null) return false;
		state.addQuest(QuestRecord.create(assignment.definition(), assignment.difficulty()));
		return true;
	}

	private static long questSeed(ServerPlayer player, long assignmentWorldTime, MinecraftServer server, long salt) {
		return player.getUUID().getMostSignificantBits()
			^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17)
			^ Long.rotateLeft(server.overworld().getSeed(), 29)
			^ assignmentWorldTime * 0x9E3779B97F4A7C15L
			^ salt;
	}

	private static List<CategoryDefinition> activeCategoriesWithRoom(PlayerQuestState state, Set<String> assignedQuestIds) {
		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.server();
		List<CategoryDefinition> available = new ArrayList<>();
		for (CategoryDefinition definition : CATEGORY_DEFINITIONS) {
			if (lifecycle.isRunning(definition.moduleId())
				&& QuestRegistry.hasAvailable(definition.id(), assignedQuestIds)
				&& count(state, definition.id()) < MAX_QUESTS_PER_CATEGORY) {
				available.add(definition);
			}
		}
		return available;
	}

	private static int count(PlayerQuestState state, String categoryId) {
		int count = 0;
		for (QuestRecord quest : state.quests()) {
			QuestDefinition definition = quest.definition();
			if (definition != null && categoryId.equals(definition.categoryId())) count++;
		}
		return count;
	}

	private static int count(List<QuestRecord> quests, String categoryId) {
		int count = 0;
		for (QuestRecord quest : quests) {
			QuestDefinition definition = quest.definition();
			if (definition != null && categoryId.equals(definition.categoryId())) count++;
		}
		return count;
	}

	private static boolean isFull(PlayerQuestState state) {
		return activeCategoriesWithRoom(state, state.definitionIds()).isEmpty();
	}

	private static PlayerQuestState loadState(ServerPlayer player) {
		long lastAssignedWorldTime = Long.MIN_VALUE;
		List<QuestRecord> quests = new ArrayList<>();
		try {
			JsonObject aggregate = PlayerDataAPIManager.getSystemDataForPlayer(player, SYSTEM_ID, "players", "uuid");
			JsonArray entries = aggregate.getAsJsonArray("players");
			if (entries != null && !entries.isEmpty() && entries.get(0).isJsonObject()) {
				JsonObject data = entries.get(0).getAsJsonObject();
				lastAssignedWorldTime = readLong(data, LAST_ASSIGNED_WORLD_TIME, Long.MIN_VALUE);
				JsonElement questElement = data.get(QUESTS);
				if (questElement != null && questElement.isJsonArray()) {
					for (JsonElement element : questElement.getAsJsonArray()) {
						if (!element.isJsonObject() || quests.size() >= MAX_QUESTS_PER_CATEGORY * CATEGORY_DEFINITIONS.size()) continue;
						QuestRecord quest = readQuest(element.getAsJsonObject());
						if (quest == null
							|| containsDefinition(quests, quest.definitionId())
							|| count(quests, quest.definition().categoryId()) >= MAX_QUESTS_PER_CATEGORY) continue;
						quests.add(quest);
					}
				}
			}
		} catch (RuntimeException exception) {
			LOGGER.warn("Failed to load the persisted quest state for {}.", player.getUUID(), exception);
		}
		return new PlayerQuestState(lastAssignedWorldTime, quests);
	}

	private static QuestRecord readQuest(JsonObject source) {
		String id = readString(source, ID, "");
		QuestDefinition definition = QuestRegistry.get(id);
		QuestDifficulty difficulty = QuestDifficulty.fromId(readString(source, DIFFICULTY, ""));
		if (definition == null || difficulty == null) return null;
		int[] progress = new int[definition.objectives().size()];
		JsonElement progressElement = source.get(PROGRESS);
		if (progressElement != null && progressElement.isJsonArray()) {
			JsonArray values = progressElement.getAsJsonArray();
			for (int index = 0; index < progress.length && index < values.size(); index++) {
				progress[index] = clampProgress(values.get(index), difficulty.scaledCount(definition.objectives().get(index).requiredCount()));
			}
		}
		return new QuestRecord(definition.id(), difficulty, progress);
	}

	private static boolean containsDefinition(List<QuestRecord> quests, String definitionId) {
		for (QuestRecord quest : quests) {
			if (definitionId.equals(quest.definitionId())) return true;
		}
		return false;
	}

	private static int clampProgress(JsonElement element, int requiredCount) {
		try {
			return Math.max(0, Math.min(requiredCount, element.getAsInt()));
		} catch (RuntimeException exception) {
			return 0;
		}
	}

	private static void persistState(ServerPlayer player, PlayerQuestState state) {
		JsonObject data = new JsonObject();
		data.addProperty(LAST_ASSIGNED_WORLD_TIME, state.lastAssignedWorldTime());
		JsonArray quests = new JsonArray();
		for (QuestRecord quest : state.quests()) {
			QuestDefinition definition = quest.definition();
			if (definition == null) continue;
			JsonObject entry = new JsonObject();
			entry.addProperty(ID, definition.id());
			entry.addProperty(CATEGORY, definition.categoryId());
			entry.addProperty(DIFFICULTY, quest.difficulty().id());
			JsonArray progress = new JsonArray();
			for (int value : quest.progress()) progress.add(value);
			entry.add(PROGRESS, progress);
			quests.add(entry);
		}
		data.add(QUESTS, quests);
		PlayerDataAPIManager.setSystemDataForPlayer(player, SYSTEM_ID, data);
	}

	private static void markProgressChanged(ServerPlayer player) {
		DIRTY_PERSISTENCE.add(player.getUUID());
		DIRTY_SYNCS.add(player.getUUID());
	}

	private static void flushDirtyPlayerStates(MinecraftServer server) {
		if (server == null) return;
		for (UUID playerId : List.copyOf(DIRTY_PERSISTENCE)) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			PlayerQuestState state = STATES_BY_PLAYER.get(playerId);
			if (player != null && state != null) persistState(player, state);
			DIRTY_PERSISTENCE.remove(playerId);
		}
	}

	private static long autosaveBucket(MinecraftServer server) {
		long interval = Math.max(1L, PlayerDataAPIManager.getAutoSaveIntervalTicks());
		return Math.floorDiv(server.overworld().getGameTime(), interval);
	}

	private static void send(ServerPlayer player, PlayerQuestState state) {
		SyncPlayerAPIManager.send(
			player,
			new QuestsPayloadAPIManager(nextAssignmentWorldTime(state.lastAssignedWorldTime()), encodeQuests(state.quests()))
		);
	}

	private static void sendImmediately(ServerPlayer player, PlayerQuestState state) {
		if (player == null || state == null) return;
		send(player, state);
		UUID playerId = player.getUUID();
		DIRTY_SYNCS.remove(playerId);
		NEXT_PROGRESS_SYNC_TICKS.put(playerId, player.level().getGameTime() + PROGRESS_SYNC_INTERVAL_TICKS);
	}

	private static String encodeQuests(List<QuestRecord> quests) {
		JsonArray encoded = new JsonArray();
		for (QuestRecord quest : quests) {
			QuestDefinition definition = quest.definition();
			if (definition == null) continue;
			JsonObject entry = new JsonObject();
			entry.addProperty(ID, definition.id());
			entry.addProperty(CATEGORY, definition.categoryId());
			entry.addProperty(DIFFICULTY, quest.difficulty().id());
			entry.addProperty(TITLE_KEY, definition.titleKey());
			entry.addProperty(DESCRIPTION_KEY, definition.descriptionKey());
			entry.addProperty("display-progress", quest.displayProgress());
			entry.addProperty("display-required", quest.displayRequired());
			entry.addProperty("complete", quest.isComplete());
			encoded.add(entry);
		}
		return encoded.toString();
	}

	private static String describeState(PlayerQuestState state) {
		if (state == null) return "null";
		List<String> quests = new ArrayList<>();
		for (QuestRecord quest : state.quests()) quests.add(quest.describe());
		return quests.toString();
	}

	private static boolean matchesBlock(QuestTarget target, BlockState state) {
		Identifier identifier = Identifier.tryParse(target.id());
		if (identifier == null) return false;
		if (target.kind() == QuestTarget.Kind.TAG) {
			return state.is(TagKey.create(Registries.BLOCK, identifier));
		}
		return identifier.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
	}

	private static boolean matchesItem(QuestTarget target, Item item) {
		Identifier identifier = Identifier.tryParse(target.id());
		if (identifier == null || item == null) return false;
		if (target.kind() == QuestTarget.Kind.TAG) {
			return BuiltInRegistries.ITEM.wrapAsHolder(item).is(TagKey.create(Registries.ITEM, identifier));
		}
		return identifier.equals(BuiltInRegistries.ITEM.getKey(item));
	}

	private static void scheduleNextAssignment(ServerPlayer player, PlayerQuestState state) {
		if (player == null || state == null || state.lastAssignedWorldTime() == Long.MIN_VALUE) return;
		long dueWorldTime = nextAssignmentWorldTime(state.lastAssignedWorldTime());
		QuestScheduleEntry previous = SCHEDULE_BY_PLAYER.remove(player.getUUID());
		if (previous != null) ASSIGNMENT_SCHEDULE.remove(previous);
		if (dueWorldTime == Long.MAX_VALUE) return;
		QuestScheduleEntry next = new QuestScheduleEntry(
			player.getUUID(),
			dueWorldTime,
			state.lastAssignedWorldTime()
		);
		SCHEDULE_BY_PLAYER.put(player.getUUID(), next);
		ASSIGNMENT_SCHEDULE.add(next);
	}

	private static void removeScheduledAssignment(UUID playerId) {
		if (playerId == null) return;
		QuestScheduleEntry scheduled = SCHEDULE_BY_PLAYER.remove(playerId);
		if (scheduled != null) ASSIGNMENT_SCHEDULE.remove(scheduled);
	}

	private static long nextAssignmentWorldTime(long lastAssignedWorldTime) {
		if (lastAssignedWorldTime == Long.MIN_VALUE || lastAssignedWorldTime >= Long.MAX_VALUE - QUEST_INTERVAL_WORLD_TICKS) {
			return Long.MAX_VALUE;
		}
		return lastAssignedWorldTime + QUEST_INTERVAL_WORLD_TICKS;
	}

	private static long currentWorldTime(MinecraftServer server) {
		return Math.max(0L, TimeAPIManager.getCurrentAbsoluteDayTime(server == null ? null : server.overworld()));
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

	private record CategoryDefinition(String id, Identifier moduleId) {
	}

	private static final class QuestRecord {
		private final String definitionId;
		private final QuestDifficulty difficulty;
		private final int[] progress;

		private QuestRecord(String definitionId, QuestDifficulty difficulty, int[] progress) {
			this.definitionId = definitionId;
			this.difficulty = difficulty;
			this.progress = progress;
		}

		private static QuestRecord create(QuestDefinition definition, QuestDifficulty difficulty) {
			return new QuestRecord(definition.id(), difficulty, new int[definition.objectives().size()]);
		}

		private QuestDefinition definition() {
			return QuestRegistry.get(definitionId);
		}

		private String definitionId() {
			return definitionId;
		}

		private String describe() {
			return definitionId + "[" + difficulty.id() + "]=" + displayProgress() + "/" + displayRequired()
				+ " objectives=" + java.util.Arrays.toString(progress);
		}

		private int[] progress() {
			return progress;
		}

		private QuestDifficulty difficulty() {
			return difficulty;
		}

		private int requiredCount(QuestDefinition definition, int index) {
			return difficulty.scaledCount(definition.objectives().get(index).requiredCount());
		}

		private boolean addBroken(BlockState state) {
			QuestDefinition definition = definition();
			if (definition == null) return false;
			boolean changed = false;
			for (int index = 0; index < definition.objectives().size(); index++) {
				QuestObjective objective = definition.objectives().get(index);
				if (objective.subcategory() != QuestSubcategory.BREAK) continue;
				int required = requiredCount(definition, index);
				boolean matches = matchesBlock(objective.target(), state);
				LOGGER.info(
					"[Quests] Break objective check quest={} difficulty={} target={} block={} matched={} progress={}/{}",
					definitionId,
					difficulty.id(),
					objective.target().id(),
					BuiltInRegistries.BLOCK.getKey(state.getBlock()),
					matches,
					progress[index],
					required
				);
				if (progress[index] >= required || !matches) continue;
				progress[index]++;
				changed = true;
			}
			return changed;
		}

		private boolean addCollected(Item item, int amount) {
			QuestDefinition definition = definition();
			if (definition == null || amount <= 0) return false;
			boolean changed = false;
			for (int index = 0; index < definition.objectives().size(); index++) {
				QuestObjective objective = definition.objectives().get(index);
				if (objective.subcategory() != QuestSubcategory.COLLECT) continue;
				int required = requiredCount(definition, index);
				boolean matches = matchesItem(objective.target(), item);
				LOGGER.info(
					"[Quests] Collect objective check quest={} difficulty={} target={} item={} matched={} progress={}/{} amount={}",
					definitionId,
					difficulty.id(),
					objective.target().id(),
					BuiltInRegistries.ITEM.getKey(item),
					matches,
					progress[index],
					required,
					amount
				);
				if (progress[index] >= required || !matches) continue;
				int next = (int) Math.min(required, (long) progress[index] + amount);
				changed |= next != progress[index];
				progress[index] = next;
			}
			return changed;
		}

		private int displayProgress() {
			QuestDefinition definition = definition();
			if (definition == null || progress.length == 0) return 0;
			int displayRequired = displayRequired();
			if (displayRequired <= 0) return 0;
			double lowestRatio = 1.0D;
			for (int index = 0; index < progress.length; index++) {
				int required = requiredCount(definition, index);
				if (required <= 0) continue;
				int objectiveProgress = Math.min(progress[index], required);
				lowestRatio = Math.min(lowestRatio, (double) objectiveProgress / required);
			}
			return Math.max(0, Math.min(displayRequired, (int) Math.floor(lowestRatio * displayRequired)));
		}

		private int displayRequired() {
			QuestDefinition definition = definition();
			if (definition == null || definition.objectives().isEmpty()) return 0;
			int maximum = 0;
			for (int index = 0; index < definition.objectives().size(); index++) {
				maximum = Math.max(maximum, requiredCount(definition, index));
			}
			return maximum;
		}

		private boolean isComplete() {
			QuestDefinition definition = definition();
			if (definition == null || progress.length != definition.objectives().size()) return false;
			for (int index = 0; index < progress.length; index++) {
				if (progress[index] < requiredCount(definition, index)) return false;
			}
			return true;
		}
	}

	private record QuestScheduleEntry(UUID playerId, long dueWorldTime, long cursorWorldTime) {
	}

	private static final class PlayerQuestState {
		private long lastAssignedWorldTime;
		private final List<QuestRecord> quests;
		private final Map<String, QuestRecord> questsById;
		private boolean synchronizedState;

		private PlayerQuestState(long lastAssignedWorldTime, List<QuestRecord> quests) {
			this.lastAssignedWorldTime = lastAssignedWorldTime;
			this.quests = quests;
			this.questsById = new HashMap<>();
			for (QuestRecord quest : quests) this.questsById.put(quest.definitionId(), quest);
		}

		private long lastAssignedWorldTime() { return lastAssignedWorldTime; }
		private void lastAssignedWorldTime(long value) { lastAssignedWorldTime = value; }
		private List<QuestRecord> quests() { return quests; }
		private void addQuest(QuestRecord quest) {
			if (quest == null || questsById.containsKey(quest.definitionId())) return;
			quests.add(quest);
			questsById.put(quest.definitionId(), quest);
		}
		private QuestRecord quest(String definitionId) { return questsById.get(definitionId); }
		private void removeQuest(String definitionId) {
			QuestRecord quest = questsById.remove(definitionId);
			if (quest != null) quests.remove(quest);
		}
		private boolean containsDefinition(String definitionId) { return questsById.containsKey(definitionId); }
		private Set<String> definitionIds() {
			return new HashSet<>(questsById.keySet());
		}
		private boolean needsInitialSync() { return !synchronizedState; }
		private void markSynchronized() { synchronizedState = true; }

		private boolean addBroken(BlockState state) {
			boolean changed = false;
			for (QuestRecord quest : quests) changed |= quest.addBroken(state);
			return changed;
		}

		private boolean addCollected(Item item, int amount) {
			boolean changed = false;
			for (QuestRecord quest : quests) changed |= quest.addCollected(item, amount);
			return changed;
		}
	}
}
