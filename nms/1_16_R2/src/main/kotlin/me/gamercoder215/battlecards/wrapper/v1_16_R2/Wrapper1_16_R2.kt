package me.gamercoder215.battlecards.wrapper.v1_16_R2

import me.gamercoder215.battlecards.impl.CardAttribute
import me.gamercoder215.battlecards.impl.cards.IBattleCard
import me.gamercoder215.battlecards.util.BattleParticle
import me.gamercoder215.battlecards.util.CardAttackType
import me.gamercoder215.battlecards.wrapper.BattleInventory
import me.gamercoder215.battlecards.wrapper.NBTWrapper
import me.gamercoder215.battlecards.wrapper.Wrapper
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.server.v1_16_R2.*
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftCreature
import org.bukkit.craftbukkit.v1_16_R2.util.CraftNamespacedKey
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Wither

@Suppress("unchecked_cast", "KotlinConstantConditions")
internal class Wrapper1_16_R2 : Wrapper {

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
        return IRegistry.ATTRIBUTE.get(toNMS(attribute.key)) ?: throw NullPointerException("Attribute ${attribute.key} not found")
    }

    override fun loadProperties(en: Creature, card: IBattleCard<*>) {
        val nms = (en as CraftCreature).handle
        EntityLiving::class.java.getDeclaredField("drops").apply { isAccessible = true }[nms] = emptyList<ItemStack>()

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

        removeGoals(nms.goalSelector, nms.targetSelector)
        nms.goalSelector.a(2, FollowCardOwner1_16_R2(nms, card))

        nms.targetSelector.a(1, CardOwnerHurtByTargetGoal1_16_R2(nms, card))
        nms.targetSelector.a(2, CardOwnerHurtTargetGoal1_16_R2(nms, card))
        nms.targetSelector.a(3, PathfinderGoalHurtByTarget(nms))

        nms.addScoreboardTag("battlecards")
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

        removeGoals(nms.goalSelector, nms.targetSelector)
        nms.goalSelector.a(2, FollowCardOwner1_16_R2(nms, ownerCard))

        nms.targetSelector.a(1, CardMasterHurtByTargetGoal1_16_R2(nms, ownerCard))
        nms.targetSelector.a(2, CardMasterHurtTargetGoal1_16_R2(nms, ownerCard))
        nms.targetSelector.a(3, CardOwnerHurtByTargetGoal1_16_R2(nms, ownerCard))
        nms.targetSelector.a(4, CardOwnerHurtTargetGoal1_16_R2(nms, ownerCard))
        nms.targetSelector.a(5, PathfinderGoalHurtByTarget(nms))

        ownerCard.minions.add(en)
        return en
    }

    private fun removeGoals(goalSelector: PathfinderGoalSelector, targetSelector: PathfinderGoalSelector) {
        val field = PathfinderGoalSelector::class.java.getDeclaredField("d").apply { isAccessible = true }
        (field.get(goalSelector) as Set<PathfinderGoalWrapped>).map { it.j() }.filter {
            it is PathfinderGoalAvoidTarget<*> || it is PathfinderGoalRestrictSun || it is PathfinderGoalFleeSun || it is PathfinderGoalBeg || it is PathfinderGoalBreed
        }.forEach { goalSelector.a(it) }

        (field.get(targetSelector) as Set<PathfinderGoalWrapped>).map { it.j() }.filter {
            it is PathfinderGoalNearestAttackableTarget<*> || it is PathfinderGoalNearestAttackableTargetWitch<*> || it is PathfinderGoalNearestHealableRaider<*> || it is PathfinderGoalDefendVillage || it is PathfinderGoalUniversalAngerReset<*>
        }.forEach { targetSelector.a(it) }
    }

    override fun getNBTWrapper(item: org.bukkit.inventory.ItemStack): NBTWrapper {
        return NBTWrapper1_16_R2(item)
    }

    override fun isCard(en: Creature): Boolean {
        return (en as CraftCreature).handle.scoreboardTags.contains("battlecards")
    }

    override fun createInventory(id: String, name: String, size: Int): BattleInventory {
        return BattleInventory1_16_R2(id, name, size)
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
        return IRegistry.ENTITY_TYPE[CraftNamespacedKey.toMinecraft(type.key)]
    }

    override fun getDefaultAttribute(type: EntityType, attribute: CardAttribute): Double {
        val supplier = AttributeDefaults.a(toNMS(type) as EntityTypes<out EntityLiving>)
        return supplier.b(toNMS(toBukkit(attribute)))
    }

    private fun removeAttackGoals(entity: EntityCreature) {
        val field = PathfinderGoalSelector::class.java.getDeclaredField("d").apply { isAccessible = true }
        (field.get(entity.goalSelector) as Set<PathfinderGoalWrapped>).map { it.j() }.filter {
            it is PathfinderGoalMeleeAttack || it is PathfinderGoalArrowAttack || it is PathfinderGoalBowShoot<*> || it is PathfinderGoalCrossbowAttack<*>
        }.forEach { entity.goalSelector.a(it) }
    }

    override fun setAttackType(entity: Creature, attackType: CardAttackType) {
        val nms = (entity as CraftCreature).handle
        removeAttackGoals(nms)

        nms.goalSelector.a(3, when (attackType) {
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

        val field = PathfinderGoalSelector::class.java.getDeclaredField("d").apply { isAccessible = true }
        val goals = (field.get(nms.goalSelector) as Set<PathfinderGoalWrapped>)
        val targetGoals = (field.get(nms.targetSelector) as Set<PathfinderGoalWrapped>)

        return (goals + targetGoals)
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

}