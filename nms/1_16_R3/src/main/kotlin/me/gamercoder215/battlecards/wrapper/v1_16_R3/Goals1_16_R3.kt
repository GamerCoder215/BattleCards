package me.gamercoder215.battlecards.wrapper.v1_16_R3

import me.gamercoder215.battlecards.impl.cards.IBattleCard
import net.minecraft.server.v1_16_R3.*
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftCreature
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.event.entity.EntityTargetEvent

class FollowCardOwner1_16_R3(
    private val creature: EntityInsentient,
    card: IBattleCard<*>
) : PathfinderGoal() {

    companion object {
        const val STOP_DISTANCE = 4
    }

    private val player: EntityPlayer = (card.p as CraftPlayer).handle
    private var timeToRecalcPath: Int = 0
    private var oldWaterCost: Float = 0F

    override fun a(): Boolean = when {
        player.isSpectator || player.bukkitEntity.isFlying -> false
        creature.isLeashed || creature.h(player) < STOP_DISTANCE.times(STOP_DISTANCE) -> false
        creature.goalTarget != null -> false
        else -> true
    }

    override fun c() {
        timeToRecalcPath = 0
        oldWaterCost = creature.a(PathType.WATER)
        creature.a(PathType.WATER, 0.0F)
    }

    override fun d() {
        creature.navigation.o()
        creature.a(PathType.WATER, oldWaterCost)
    }

    override fun e() {
        creature.controllerLook.a(player, 10.0F, creature.O().toFloat())
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10

            val x = creature.locX() - player.locX()
            val y = creature.locY() - player.locY()
            val z = creature.locZ() - player.locZ()
            val distance = x * x + y * y + z * z

            if (distance > STOP_DISTANCE.times(STOP_DISTANCE))
                creature.navigation.a(player, 1.2)
            else {
                creature.navigation.o()
                val sight = player.bukkitEntity.hasLineOfSight(creature.bukkitEntity)

                if (distance <= STOP_DISTANCE || sight) {
                    val dx = player.locX() - creature.locX()
                    val dz = player.locZ() - creature.locZ()
                    creature.navigation.a(creature.locX() - dx, creature.locY(), creature.locZ() - dz, 1.2)
                }
            }
        }
    }

}

class CardOwnerHurtTargetGoal1_16_R3(
    private val creature: EntityCreature,
    card: IBattleCard<*>
) : PathfinderGoalTarget(creature, true, true) {

    private val nms = (card.p as CraftPlayer).handle
    private var timestamp: Int = 0
    private var lastHurt = nms.db()

    override fun a(): Boolean {
        lastHurt = nms.db()
        return timestamp != nms.dc() && a(lastHurt, PathfinderTargetCondition.a)
    }

    override fun c() {
        creature.setGoalTarget(lastHurt, EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true)
        timestamp = nms.dc()

        super.c()
    }

}

class CardOwnerHurtByTargetGoal1_16_R3(
    private val creature: EntityCreature,
    card: IBattleCard<*>
) : PathfinderGoalTarget(creature, true, true) {

    private val nms = (card.p as CraftPlayer).handle
    private var timestamp: Int = 0
    private var lastHurtBy = nms.lastDamager

    override fun a(): Boolean {
        lastHurtBy = nms.lastDamager
        return timestamp != nms.hurtTimestamp && a(lastHurtBy, PathfinderTargetCondition.a)
    }

    override fun c() {
        creature.setGoalTarget(lastHurtBy, EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER, true)
        timestamp = nms.hurtTimestamp

        super.c()
    }

}

internal class CardMasterHurtTargetGoal1_16_R3(
    private val creature: EntityCreature,
    card: IBattleCard<*>
) : PathfinderGoalTarget(creature, true, true) {

    private val nms = (card.entity as CraftCreature).handle
    private var timestamp: Int = 0
    private var lastHurt = nms.db()

    override fun a(): Boolean {
        lastHurt = nms.db()
        return timestamp != nms.dc() && a(lastHurt, PathfinderTargetCondition.a)
    }

    override fun c() {
        creature.setGoalTarget(lastHurt, EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true)
        timestamp = nms.dc()

        super.c()
    }

}

internal class CardMasterHurtByTargetGoal1_16_R3(
    private val creature: EntityCreature,
    card: IBattleCard<*>
) : PathfinderGoalTarget(creature, true, true) {

    private val nms = (card.entity as CraftCreature).handle
    private var timestamp: Int = 0
    private var lastHurtBy = nms.lastDamager

    override fun a(): Boolean {
        lastHurtBy = nms.lastDamager
        return timestamp != nms.hurtTimestamp && a(lastHurtBy, PathfinderTargetCondition.a)
    }

    override fun c() {
        creature.setGoalTarget(lastHurtBy, EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER, true)
        timestamp = nms.hurtTimestamp

        super.c()
    }

}