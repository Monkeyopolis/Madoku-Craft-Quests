package madoku.craft.mixin.quests;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Counts eligible items after they enter a player's inventory or hotbar from a block container. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuQuestCollectMixin {
	@Unique
	private static final ThreadLocal<ClickState> ACTIVE_CLICK = new ThreadLocal<>();
	@Unique
	private static final Map<UUID, PendingTransfers> PENDING_TRANSFERS = new HashMap<>();

	@Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
	private void madokuCraft$beginQuestCollectionTracking(
		int slotId,
		int button,
		net.minecraft.world.inventory.ContainerInput input,
		Player player,
		CallbackInfo ci
	) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			ACTIVE_CLICK.remove();
			return;
		}
		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		PendingTransfers pending = PENDING_TRANSFERS.get(serverPlayer.getUUID());
		if (pending != null && pending.menu != menu) {
			PENDING_TRANSFERS.remove(serverPlayer.getUUID());
			pending = null;
		}
		Map<Integer, SourceSlot> sourceSlots = new HashMap<>();
		for (int index = 0; index < menu.slots.size(); index++) {
			Slot slot = menu.slots.get(index);
			if (!madokuCraft$isTrackedContainerSlot(slot, serverPlayer)) continue;
			sourceSlots.put(index, new SourceSlot(slot.getItem().copy(), madokuCraft$isModifiedSlot(slot)));
		}
		ACTIVE_CLICK.set(new ClickState(
			menu,
			serverPlayer,
			madokuCraft$itemCounts(serverPlayer.getInventory()),
			menu.getCarried().copy(),
			sourceSlots
		));
	}

	@Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"))
	private void madokuCraft$finishQuestCollectionTracking(
		int slotId,
		int button,
		net.minecraft.world.inventory.ContainerInput input,
		Player player,
		CallbackInfo ci
	) {
		ClickState click = ACTIVE_CLICK.get();
		ACTIVE_CLICK.remove();
		if (click == null) return;

		ServerPlayer serverPlayer = click.player;
		Map<Item, Integer> inventoryIncrease = madokuCraft$positiveDelta(
			click.inventoryBefore,
			madokuCraft$itemCounts(serverPlayer.getInventory())
		);
		PendingTransfers pending = PENDING_TRANSFERS.get(serverPlayer.getUUID());
		if (pending != null && pending.menu != click.menu) {
			PENDING_TRANSFERS.remove(serverPlayer.getUUID());
			pending = null;
		}

		Map<Item, Integer> pendingConsumed = new HashMap<>();
		for (Map.Entry<Item, Integer> entry : inventoryIncrease.entrySet()) {
			if (pending == null) break;
			int consumed = pending.consume(entry.getKey(), entry.getValue(), serverPlayer);
			if (consumed > 0) pendingConsumed.put(entry.getKey(), consumed);
		}

		Map<Item, Integer> naturalSource = new HashMap<>();
		Map<Item, Integer> modifiedSource = new HashMap<>();
		for (Map.Entry<Integer, SourceSlot> entry : click.sourceSlots.entrySet()) {
			Slot slot = click.menu.slots.get(entry.getKey());
			int decrease = madokuCraft$decrease(entry.getValue().stack, slot.getItem());
			if (decrease <= 0 || entry.getValue().stack.isEmpty()) continue;
			Map<Item, Integer> target = entry.getValue().modified ? modifiedSource : naturalSource;
			target.merge(entry.getValue().stack.getItem(), decrease, Integer::sum);
		}

		for (Map.Entry<Item, Integer> entry : inventoryIncrease.entrySet()) {
			Item item = entry.getKey();
			int available = entry.getValue() - pendingConsumed.getOrDefault(item, 0);
			if (available <= 0) continue;
			int modified = Math.min(available, modifiedSource.getOrDefault(item, 0));
			available -= modified;
			int natural = Math.min(available, naturalSource.getOrDefault(item, 0));
			madokuCraft$emit(serverPlayer, item, modified, natural);
			int usedModified = Math.min(modifiedSource.getOrDefault(item, 0), modified);
			int usedNatural = Math.min(naturalSource.getOrDefault(item, 0), natural);
			modifiedSource.merge(item, -usedModified, Integer::sum);
			naturalSource.merge(item, -usedNatural, Integer::sum);
		}

		if (!naturalSource.isEmpty() || !modifiedSource.isEmpty()) {
			if (pending == null) {
				pending = new PendingTransfers(click.menu);
				PENDING_TRANSFERS.put(serverPlayer.getUUID(), pending);
			}
			for (Map.Entry<Item, Integer> entry : naturalSource.entrySet()) {
				if (entry.getValue() > 0) pending.addNatural(entry.getKey(), entry.getValue());
			}
			for (Map.Entry<Item, Integer> entry : modifiedSource.entrySet()) {
				if (entry.getValue() > 0) pending.addModified(entry.getKey(), entry.getValue());
			}
		}

		if (pending != null && pending.isEmpty()) PENDING_TRANSFERS.remove(serverPlayer.getUUID());
	}

	@Unique
	private static void madokuCraft$emit(ServerPlayer player, Item item, int excluded, int counted) {
		if (excluded <= 0 && counted <= 0) return;
		if (counted > 0) QuestsStateBridge.onItemCollected(player, item, counted);
	}

	@Unique
	private static boolean madokuCraft$isTrackedContainerSlot(Slot slot, ServerPlayer player) {
		return slot != null
			&& slot.container != player.getInventory()
			&& slot.container instanceof BaseContainerBlockEntity;
	}

	@Unique
	private static boolean madokuCraft$isModifiedSlot(Slot slot) {
		if (!(slot.container instanceof BaseContainerBlockEntity container)
			|| !(container.getLevel() instanceof ServerLevel level)) return false;
		return ChunkDataAPIManager.isPlayerModifiedContainerSlot(
			level,
			container.getBlockPos(),
			slot.getContainerSlot()
		);
	}

	@Unique
	private static Map<Item, Integer> madokuCraft$itemCounts(Container container) {
		Map<Item, Integer> counts = new HashMap<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return counts;
	}

	@Unique
	private static Map<Item, Integer> madokuCraft$positiveDelta(Map<Item, Integer> before, Map<Item, Integer> after) {
		Map<Item, Integer> increase = new HashMap<>();
		for (Map.Entry<Item, Integer> entry : after.entrySet()) {
			int delta = entry.getValue() - before.getOrDefault(entry.getKey(), 0);
			if (delta > 0) increase.put(entry.getKey(), delta);
		}
		return increase;
	}

	@Unique
	private static int madokuCraft$decrease(ItemStack before, ItemStack after) {
		if (before == null || before.isEmpty()) return 0;
		if (after == null || after.isEmpty() || after.getItem() != before.getItem()) return before.getCount();
		return Math.max(0, before.getCount() - after.getCount());
	}

	@Unique
	private record ClickState(
		AbstractContainerMenu menu,
		ServerPlayer player,
		Map<Item, Integer> inventoryBefore,
		ItemStack carriedBefore,
		Map<Integer, SourceSlot> sourceSlots
	) { }

	@Unique
	private record SourceSlot(ItemStack stack, boolean modified) { }

	@Unique
	private static final class PendingTransfers {
		private final AbstractContainerMenu menu;
		private final Map<Item, PendingAmount> amounts = new HashMap<>();

		private PendingTransfers(AbstractContainerMenu menu) { this.menu = menu; }

		private void addNatural(Item item, int amount) { amounts.computeIfAbsent(item, ignored -> new PendingAmount()).natural += amount; }
		private void addModified(Item item, int amount) { amounts.computeIfAbsent(item, ignored -> new PendingAmount()).modified += amount; }

		private int consume(Item item, int maximum, ServerPlayer player) {
			PendingAmount pending = amounts.get(item);
			if (pending == null || maximum <= 0) return 0;
			int modified = Math.min(maximum, pending.modified);
			int remaining = maximum - modified;
			int natural = Math.min(remaining, pending.natural);
			pending.modified -= modified;
			pending.natural -= natural;
			if (pending.modified == 0 && pending.natural == 0) amounts.remove(item);
			madokuCraft$emit(player, item, modified, natural);
			return modified + natural;
		}

		private boolean isEmpty() { return amounts.isEmpty(); }
	}

	@Unique
	private static final class PendingAmount {
		private int natural;
		private int modified;
	}
}
