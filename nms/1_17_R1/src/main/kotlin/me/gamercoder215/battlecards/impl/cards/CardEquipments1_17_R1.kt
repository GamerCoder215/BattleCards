package me.gamercoder215.battlecards.impl.cards

import me.gamercoder215.battlecards.api.card.BattleCard
import me.gamercoder215.battlecards.api.card.item.CardEquipment
import me.gamercoder215.battlecards.api.card.item.CardEquipment.Rarity.AVERAGE
import me.gamercoder215.battlecards.api.card.item.CardEquipment.Rarity.HISTORICAL
import me.gamercoder215.battlecards.api.events.entity.CardUseAbilityEvent
import me.gamercoder215.battlecards.api.events.entity.CardUseAbilityEvent.AbilityType
import me.gamercoder215.battlecards.util.BattleUtil.ability
import me.gamercoder215.battlecards.util.BattleUtil.mod
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent

internal enum class CardEquipments1_17_R1(
    override val item: Material,
    override val rarity: CardEquipment.Rarity,
    modifiers: Array<Double>,
    override val ability: CardEquipment.Ability? = null
) : CardEquipment {

    // Average

    BRONZE_INGOT(Material.COPPER_INGOT, AVERAGE,
        mod(defense = 0.985, knockbackResistance = 1.0125)
    ),

    // Historical

    ICE_BALL(Material.SNOWBALL, HISTORICAL,
        mod(speed = 0.94), ability("freezing", AbilityType.OFFENSIVE, 1.0) { _, event ->
            val target = event.entity as? LivingEntity ?: return@ability
            target.freezeTicks += 45
        }),

    CANDLE_OF_LIFE(Material.CANDLE, HISTORICAL,
        mod(health = 1.07, knockbackResistance = 0.925)
    ),

    ;

    override val healthModifier: Double
    override val damageModifier: Double
    override val defenseModifier: Double
    override val speedModifier: Double
    override val knockbackResistanceModifier: Double

    init {
        require(modifiers.size == 5) { "Modifiers must be of size 5" }

        this.healthModifier = modifiers[0]
        this.damageModifier = modifiers[1]
        this.defenseModifier = modifiers[2]
        this.speedModifier = modifiers[3]
        this.knockbackResistanceModifier = modifiers[4]
    }

}