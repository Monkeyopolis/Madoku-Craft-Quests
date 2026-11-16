package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records successful player-attributed hits for environmental kill attribution. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityQuestDamageMixin {
	@Inject(
		method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
		at = @At("RETURN")
	)
	private void madokuCraft$recordQuestDamage(
		ServerLevel level,
		DamageSource damageSource,
		float amount,
		CallbackInfoReturnable<Boolean> callbackInfo
	) {
		if (callbackInfo.getReturnValueZ()) {
			QuestsStateBridge.onEntityDamaged((LivingEntity) (Object) this, damageSource);
		}
	}
}
