package me.gamercoder215.battlecards.wrapper.v1_17_R1

import me.gamercoder215.battlecards.api.BattleConfig
import me.gamercoder215.battlecards.impl.CardAttribute
import me.gamercoder215.battlecards.impl.cards.IBattleCard
import me.gamercoder215.battlecards.util.*
import me.gamercoder215.battlecards.wrapper.BattleInventory
import me.gamercoder215.battlecards.wrapper.NBTWrapper
import me.gamercoder215.battlecards.wrapper.PACKET_INJECTOR_ID
import me.gamercoder215.battlecards.wrapper.Wrapper
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.core.IRegistry
import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle
import net.minecraft.resources.MinecraftKey
import net.minecraft.world.entity.EntityCreature
import net.minecraft.world.entity.EntityLiving
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.ai.attributes.AttributeBase
import net.minecraft.world.entity.ai.attributes.AttributeDefaults
import net.minecraft.world.entity.ai.attributes.AttributeMapBase
import net.minecraft.world.entity.ai.attributes.AttributeModifiable
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.*
import net.minecraft.world.entity.boss.wither.EntityWither
import net.minecraft.world.entity.monster.EntityMonster
import net.minecraft.world.entity.monster.ICrossbow
import net.minecraft.world.entity.monster.IRangedEntity
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftCreature
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_17_R1.util.CraftNamespacedKey
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Wither
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

@Suppress("unchecked_cast", "KotlinConstantConditions")
internal class Wrapper1_17_R1 : Wrapper {

    override fun sendActionbar(player: Player, component: BaseComponent) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, component)
    }

    override fun sendActionbar(player: Player, message: String) {
        sendActionbar(player, TextComponent(message))
    }

    override fun setBossBarVisibility(boss: Wither, visible: Boolean) {
        boss.bossBar?.isVisible = visible
    }

    fun toNMS(key: NamespacedKey): MinecraftKey {
        return CraftNamespacedKey.toMinecraft(key)
    }

    fun toBukkit(attribute: CardAttribute): Attribute {
        return when (attribute) {
            CardAttribute.MAX_HEALTH -> Attribute.GENERIC_MAX_HEALTH
            CardAttribute.ATTACK_DAMAGE -> Attribute.GENERIC_ATTACK_DAMAGE
            CardAttribute.KNOCKBACK_RESISTANCE -> Attribute.GENERIC_KNOCKBACK_RESISTANCE
            CardAttribute.SPEED -> Attribute.GENERIC_MOVEMENT_SPEED
            CardAttribute.DEFENSE -> Attribute.GENERIC_ARMOR
            CardAttribute.FOLLOW_RANGE -> Attribute.GENERIC_FOLLOW_RANGE
        }
    }

    fun toNMS(attribute: Attribute): AttributeBase {
        return IRegistry.al.get(toNMS(attribute.key)) ?: throw NullPointerException("Attribute ${attribute.key} not found")
    }

    override fun loadProperties(en: Creature, card: IBattleCard<*>) {
        val nms = (en as CraftCreature).handle

        nms.drops.clear()

        for (entry in card.statistics.attributes) {
            val attribute = toNMS(toBukkit(entry.key))
            val value = entry.value

            var handle = nms.getAttributeInstance(attribute)
            if (handle == null) {
                val attributesF = AttributeMapBase::class.java.getDeclaredField("b")
                attributesF.isAccessible = true
                val attributes = attributesF.get(nms) as MutableMap<AttributeBase, AttributeModifiable>

                handle = AttributeModifiable(attribute) {}
                attributes[attribute] = handle
            }

            handle.value = value
        }

        removeGoals(nms.bP, nms.bQ)
        nms.bP.a(2, FollowCardOwner1_17_R1(nms, card))

        nms.bQ.a(1, CardOwnerHurtByTargetGoal1_17_R1(nms, card))
        nms.bQ.a(2, CardOwnerHurtTargetGoal1_17_R1(nms, card))
        nms.bQ.a(3, PathfinderGoalHurtByTarget(nms))

        nms.addScoreboardTag("battlecards")

        if (nms is EntityWither)
            object : BukkitRunnable() {
                override fun run() {
                    if (en.isDead)
                        return cancel()

                    for (i in 0..2) {
                        val alt = nms.t.getEntity(nms.getHeadTarget(i))?.bukkitEntity ?: continue

                        if ((alt is Player && !BattleConfig.config.cardAttackPlayers) || (alt !is Player && !alt.isCard))
                            nms.setHeadTarget(i, 0)
                    }
                }
            }.runTaskTimer(BattleConfig.plugin, 0L, 1L)
    }

    override fun <T : Creature> spawnMinion(clazz: Class<T>, ownerCard: IBattleCard<*>): T {
        val card = ownerCard.entity
        val en = card.world.spawn(card.location, clazz)

        en.isCustomNameVisible = true
        en.customName = "${ownerCard.rarity.color}${ownerCard.name}'s Minion"

        val equipment = en.equipment!!
        equipment.itemInMainHandDropChance = 0F
        equipment.itemInOffHandDropChance = 0F
        equipment.helmetDropChance = 0F
        equipment.chestplateDropChance = 0F
        equipment.leggingsDropChance = 0F
        equipment.bootsDropChance = 0F

        en.target = ownerCard.target

        val nms = (en as CraftCreature).handle

        removeGoals(nms.bP, nms.bQ)
        nms.bP.a(2, FollowCardOwner1_17_R1(nms, ownerCard))

        nms.bQ.a(1, CardMasterHurtByTargetGoal1_17_R1(nms, ownerCard))
        nms.bQ.a(2, CardMasterHurtTargetGoal1_17_R1(nms, ownerCard))
        nms.bQ.a(3, CardOwnerHurtByTargetGoal1_17_R1(nms, ownerCard))
        nms.bQ.a(4, CardOwnerHurtTargetGoal1_17_R1(nms, ownerCard))
        nms.bQ.a(5, PathfinderGoalHurtByTarget(nms))

        ownerCard.minions.add(en)
        return en
    }

    private fun removeGoals(goalSelector: PathfinderGoalSelector, targetSelector: PathfinderGoalSelector) {
        goalSelector.c().map { it.j() }.filter {
            it is PathfinderGoalAvoidTarget<*> || it is PathfinderGoalRestrictSun || it is PathfinderGoalFleeSun || it is PathfinderGoalBeg || it is PathfinderGoalBreed
        }.forEach { goalSelector.a(it) }

        targetSelector.c().map { it.j() }.filter {
            it is PathfinderGoalNearestAttackableTarget<*> || it is PathfinderGoalNearestAttackableTargetWitch<*> || it is PathfinderGoalNearestHealableRaider<*> || it is PathfinderGoalDefendVillage || it is PathfinderGoalUniversalAngerReset<*>
        }.forEach { targetSelector.a(it) }
    }

    override fun getNBTWrapper(item: ItemStack): NBTWrapper {
        return NBTWrapper1_17_R1(item)
    }

    override fun isCard(en: Creature): Boolean {
        return (en as CraftCreature).handle.scoreboardTags.contains("battlecards")
    }

    override fun createInventory(id: String, name: String, size: Int): BattleInventory {
        return BattleInventory1_17_R1(id, name, size)
    }

    override fun spawnParticle(
        particle: BattleParticle, location: Location, count: Int,
        dX: Double, dY: Double, dZ: Double,
        speed: Double, force: Boolean
    ) {
        if (location.world == null) return
        location.world!!.spawnParticle(Particle.valueOf(particle.name.uppercase()), location, count, dX, dY, dZ, speed)
    }

    private fun toNMS(type: EntityType): EntityTypes<*> {
        return IRegistry.Y[CraftNamespacedKey.toMinecraft(type.key)]
    }

    override fun getDefaultAttribute(type: EntityType, attribute: CardAttribute): Double {
        val supplier = AttributeDefaults.a(toNMS(type) as EntityTypes<out EntityLiving>)
        return supplier.b(toNMS(toBukkit(attribute)))
    }

    private fun removeAttackGoals(entity: EntityCreature) {
        entity.bP.c().map { it.j() }.filter {
            it is PathfinderGoalMeleeAttack || it is PathfinderGoalArrowAttack || it is PathfinderGoalBowShoot<*> || it is PathfinderGoalCrossbowAttack<*>
        }.forEach { entity.bP.a(it) }
    }

    override fun setAttackType(entity: Creature, attackType: CardAttackType) {
        val nms = (entity as CraftCreature).handle
        removeAttackGoals(nms)

        nms.bP.a(3, when (attackType) {
            CardAttackType.MELEE -> PathfinderGoalMeleeAttack(nms, 1.0, false)
            CardAttackType.BOW -> {
                if (nms !is EntityMonster) throw UnsupportedOperationException("Invalid Monster Type ${entity::class.java.simpleName}")
                if (nms !is IRangedEntity) throw UnsupportedOperationException("Invalid Ranged Type ${entity::class.java.simpleName}")

                PathfinderGoalBowShoot(nms, 1.0, 20, 15.0F)
            }
            CardAttackType.CROSSBOW -> {
                if (nms !is EntityMonster) throw UnsupportedOperationException("Invalid Monster Type ${entity::class.java.simpleName}")
                if (nms !is ICrossbow) throw UnsupportedOperationException("Invalid Crossbow Type ${entity::class.java.simpleName}")
                PathfinderGoalCrossbowAttack(nms,1.0, 15.0F)
            }
        })
    }

    override fun getAttackType(entity: Creature): CardAttackType {
        val nms = (entity as CraftCreature).handle

        return (nms.bP.c() + nms.bQ.c())
            .sortedBy { it.h() }
            .firstNotNullOf {
                when (it.j()) {
                    is PathfinderGoalMeleeAttack -> CardAttackType.MELEE
                    is PathfinderGoalBowShoot<*> -> CardAttackType.BOW
                    is PathfinderGoalCrossbowAttack<*> -> CardAttackType.CROSSBOW
                    else -> null
                } ?: CardAttackType.MELEE
            }
    }

    override fun getYBodyRot(entity: org.bukkit.entity.LivingEntity): Float = (entity as CraftLivingEntity).handle.aX

    override fun addPacketInjector(p: Player) {
        val sp = (p as CraftPlayer).handle
        val ch = sp.b.a.k

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return
        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, PacketHandler1_17_R1(p))

        PacketHandler1_17_R1.PACKET_HANDLERS[p.uniqueId] = handler@{ packet ->
            if (packet is PacketPlayInSteerVehicle) {
                val vehicle = p.vehicle ?: return@handler
                val card = vehicle.card ?: return@handler
                if (!card.isRideable) return@handler

                vehicle.setRotation(p.location.yaw, p.location.pitch)
                vehicle.velocity += (p.location.apply { pitch = 0F }.direction * packet.c()).plus(Vector(0, 1, 0).crossProduct(p.location.apply { pitch = 0F }.direction) * packet.b()) * (card.statistics.speed * 0.75) * if (vehicle.isOnGround) 1 else 0.3

                if (packet.d() && vehicle.isOnGround)
                    (vehicle as CraftCreature).handle.controllerJump.jump()
            }
        }
    }

    override fun removePacketInjector(p: Player) {
        val sp = (p as CraftPlayer).handle
        val ch = sp.b.a.k

        if (ch.pipeline().get(PACKET_INJECTOR_ID) == null) return
        ch.pipeline().remove(PACKET_INJECTOR_ID)
    }

}