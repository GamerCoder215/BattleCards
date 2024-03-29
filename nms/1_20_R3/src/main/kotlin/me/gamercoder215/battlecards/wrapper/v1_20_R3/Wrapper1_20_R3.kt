package me.gamercoder215.battlecards.wrapper.v1_20_R3

import me.gamercoder215.battlecards.api.BattleConfig
import me.gamercoder215.battlecards.impl.CardAttribute
import me.gamercoder215.battlecards.impl.cards.IBattleCard
import me.gamercoder215.battlecards.util.*
import me.gamercoder215.battlecards.wrapper.BattleInventory
import me.gamercoder215.battlecards.wrapper.NBTWrapper
import me.gamercoder215.battlecards.wrapper.Wrapper
import me.gamercoder215.battlecards.wrapper.Wrapper.Companion.PACKET_INJECTOR_ID
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeInstance
import net.minecraft.world.entity.ai.attributes.AttributeMap
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.*
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.CrossbowAttackMob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.RangedAttackMob
import net.minecraft.world.entity.monster.piglin.Piglin
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftCreature
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Wither
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector


@Suppress("unchecked_cast", "KotlinConstantConditions")
internal class Wrapper1_20_R3 : Wrapper {

    override fun sendActionbar(player: Player, component: BaseComponent) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, component)
    }

    override fun sendActionbar(player: Player, message: String) {
        sendActionbar(player, TextComponent(message))
    }

    override fun setBossBarVisibility(boss: Wither, visible: Boolean) {
        boss.bossBar?.isVisible = visible
    }

    fun toNMS(key: NamespacedKey): ResourceLocation {
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

    fun toNMS(attribute: Attribute): net.minecraft.world.entity.ai.attributes.Attribute {
        return BuiltInRegistries.ATTRIBUTE.get(toNMS(attribute.key)) ?: throw NullPointerException("Attribute ${attribute.key} not found")
    }

    override fun loadProperties(en: Creature, card: IBattleCard<*>) {
        val nms: PathfinderMob = (en as CraftCreature).handle

        nms.drops.clear()

        for (entry in card.statistics.attributes) {
            val attribute = toNMS(toBukkit(entry.key))
            val value = entry.value

            var handle: AttributeInstance? = nms.getAttribute(attribute)
            if (handle == null) {
                val attributesF = AttributeMap::class.java.getDeclaredField("b")
                attributesF.isAccessible = true
                val attributes = attributesF.get(nms.attributes) as MutableMap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeInstance>

                handle = AttributeInstance(attribute) {}
                attributes[attribute] = handle
            }

            handle.baseValue = value
        }

        removeGoals(nms.goalSelector, nms.targetSelector)
        nms.goalSelector.addGoal(2, FollowCardOwner1_20_R3(nms, card))

        nms.targetSelector.addGoal(1, CardOwnerHurtByTargetGoal1_20_R3(nms, card))
        nms.targetSelector.addGoal(2, CardOwnerHurtTargetGoal1_20_R3(nms, card))
        nms.targetSelector.addGoal(3, HurtByTargetGoal(nms))

        nms.addTag("battlecards")

        if (nms is WitherBoss)
            object : BukkitRunnable() {
                override fun run() {
                    if (en.isDead)
                        return cancel()

                    for (i in 0..2) {
                        val alt = nms.level().getEntity(nms.getAlternativeTarget(i))?.bukkitEntity ?: continue

                        if ((alt is Player && !BattleConfig.config.cardAttackPlayers) || (alt !is Player && !alt.isCard))
                            nms.setAlternativeTarget(i, 0)
                    }
                }
            }.runTaskTimer(BattleConfig.plugin, 0L, 1L)

        if (nms is Piglin) {
            nms.interestItems.clear()
            nms.allowedBarterItems.clear()

            object : BukkitRunnable() {
                override fun run() {
                    if (en.isDead) return cancel()

                    nms.brain.setMemory(MemoryModuleType.ADMIRING_DISABLED, true)
                }
            }.runTaskTimer(BattleConfig.plugin, 0L, 1L)
        }
    }

    override fun <T : Creature> spawnMinion(clazz: Class<T>, ownerCard: IBattleCard<*>): T {
        val card = ownerCard.entity
        val en = card.world.spawn(card.location, clazz)

        en.isCustomNameVisible = true
        en.customName = "${ownerCard.rarity.color}${ownerCard.name}'s Minion (${ownerCard.p.name})"

        val equipment = en.equipment!!
        equipment.itemInMainHandDropChance = 0F
        equipment.itemInOffHandDropChance = 0F
        equipment.helmetDropChance = 0F
        equipment.chestplateDropChance = 0F
        equipment.leggingsDropChance = 0F
        equipment.bootsDropChance = 0F

        en.target = ownerCard.entity.target

        val nms = (en as CraftCreature).handle

        removeGoals(nms.goalSelector, nms.targetSelector)
        nms.goalSelector.addGoal(2, FollowCardOwner1_20_R3(nms, ownerCard))

        nms.targetSelector.addGoal(1, CardMasterHurtByTargetGoal1_20_R3(nms, ownerCard))
        nms.targetSelector.addGoal(2, CardMasterHurtTargetGoal1_20_R3(nms, ownerCard))
        nms.targetSelector.addGoal(3, CardOwnerHurtByTargetGoal1_20_R3(nms, ownerCard))
        nms.targetSelector.addGoal(4, CardOwnerHurtTargetGoal1_20_R3(nms, ownerCard))
        nms.targetSelector.addGoal(5, HurtByTargetGoal(nms))

        ownerCard.minions.add(en)
        return en
    }

    override fun addFollowGoal(entity: org.bukkit.entity.LivingEntity, ownerCard: IBattleCard<*>) {
        if (entity !is CraftMob) return
        entity.handle.goalSelector.addGoal(2, FollowCardOwner1_20_R3(entity.handle, ownerCard))
    }

    private fun removeGoals(goalSelector: GoalSelector, targetSelector: GoalSelector) {
        goalSelector.removeAllGoals {
            it is AvoidEntityGoal<*> || it is RestrictSunGoal || it is FleeSunGoal || it is BegGoal || it is BreedGoal || it is MoveBackToVillageGoal || it is GolemRandomStrollInVillageGoal
        }

        targetSelector.removeAllGoals {
            it is NearestAttackableTargetGoal<*> || it is NearestAttackableWitchTargetGoal<*> || it is NearestHealableRaiderTargetGoal<*> || it is DefendVillageTargetGoal || it is ResetUniversalAngerTargetGoal<*>
        }
    }

    override fun getNBTWrapper(item: ItemStack): NBTWrapper {
        return NBTWrapper1_20_R3(item)
    }

    override fun isCard(en: Creature): Boolean {
        return (en as CraftCreature).handle.tags.contains("battlecards")
    }

    override fun createInventory(id: String, name: String, size: Int): BattleInventory {
        return BattleInventory1_20_R3(id, name, size)
    }

    override fun spawnParticle(
        particle: BattleParticle, location: Location, count: Int,
        dX: Double, dY: Double, dZ: Double,
        speed: Double, force: Boolean
    ) {
        if (location.world == null) return
        location.world!!.spawnParticle(Particle.valueOf(particle.name.uppercase()), location, count, dX, dY, dZ, speed)
    }

    private fun toNMS(type: EntityType): net.minecraft.world.entity.EntityType<*> {
        return BuiltInRegistries.ENTITY_TYPE[CraftNamespacedKey.toMinecraft(type.key)]
    }

    override fun getDefaultAttribute(type: EntityType, attribute: CardAttribute): Double {
        val supplier = DefaultAttributes.getSupplier(toNMS(type) as net.minecraft.world.entity.EntityType<out LivingEntity>)
        return supplier.getBaseValue(toNMS(toBukkit(attribute)))
    }

    private fun removeAttackGoals(entity: PathfinderMob) {
        entity.goalSelector.removeAllGoals {
            it is MeleeAttackGoal || it is RangedAttackGoal || it is RangedBowAttackGoal<*> || it is RangedCrossbowAttackGoal<*>
        }
    }

    override fun setAttackType(entity: Creature, attackType: CardAttackType) {
        val nms = (entity as CraftCreature).handle
        removeAttackGoals(nms)

        nms.goalSelector.addGoal(3, when (attackType) {
            CardAttackType.MELEE -> MeleeAttackGoal(nms, 1.0, false)
            CardAttackType.BOW -> {
                if (nms !is Monster) throw UnsupportedOperationException("Invalid Monster Type ${entity::class.java.simpleName}")
                if (nms !is RangedAttackMob) throw UnsupportedOperationException("Invalid Ranged Type ${entity::class.java.simpleName}")

                RangedBowAttackGoal(nms, 1.0, 20, 15.0F)
            }
            CardAttackType.CROSSBOW -> {
                if (nms !is Monster) throw UnsupportedOperationException("Invalid Monster Type ${entity::class.java.simpleName}")
                if (nms !is CrossbowAttackMob) throw UnsupportedOperationException("Invalid Crossbow Type ${entity::class.java.simpleName}")
                RangedCrossbowAttackGoal(nms,1.0, 15.0F)
            }
        })
    }

    override fun getAttackType(entity: Creature): CardAttackType {
        val nms = (entity as CraftCreature).handle

        return (nms.goalSelector.availableGoals + nms.targetSelector.availableGoals)
            .sortedBy { it.priority }
            .firstNotNullOf {
                when (it.goal) {
                    is MeleeAttackGoal -> CardAttackType.MELEE
                    is RangedBowAttackGoal<*> -> CardAttackType.BOW
                    is RangedCrossbowAttackGoal<*> -> CardAttackType.CROSSBOW
                    else -> null
                } ?: CardAttackType.MELEE
            }
    }

    override fun getYBodyRot(entity: org.bukkit.entity.LivingEntity): Float = (entity as CraftLivingEntity).handle.visualRotationYInDegrees

    override fun addPacketInjector(p: Player) {
        val sp = (p as CraftPlayer).handle
        val ch = (ServerCommonPacketListenerImpl::class.java.getDeclaredField("c").apply { isAccessible = true }.get(sp.connection) as Connection).channel

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return
        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, PacketHandler1_20_R3(p))

        PacketHandler1_20_R3.PACKET_HANDLERS[p.uniqueId] = handler@{ packet ->
            if (packet is ServerboundPlayerInputPacket) {
                val vehicle = p.vehicle as? CraftCreature ?: return@handler
                val card = vehicle.card ?: return@handler
                if (!card.isRideable) return@handler

                vehicle.setRotation(p.location.yaw, vehicle.location.pitch)

                val vector = (p.location.apply { pitch = 0F }.direction * packet.zza).plus(Vector(0, 1, 0).crossProduct(p.location.apply { pitch = 0F }.direction) * packet.xxa) * card.statistics.speed * 1.1
                vehicle.handle.move(MoverType.SELF, Vec3(vector.x, vector.y, vector.z))
            }
        }
    }

    override fun removePacketInjector(p: Player) {
        val sp = (p as CraftPlayer).handle
        val ch = (ServerCommonPacketListenerImpl::class.java.getDeclaredField("c").apply { isAccessible = true }.get(sp.connection) as Connection).channel

        if (ch.pipeline().get(PACKET_INJECTOR_ID) == null) return
        ch.pipeline().remove(PACKET_INJECTOR_ID)
    }

}