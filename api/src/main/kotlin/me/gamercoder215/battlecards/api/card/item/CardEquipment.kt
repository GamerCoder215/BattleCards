package me.gamercoder215.battlecards.api.card.item

import me.gamercoder215.battlecards.api.card.BattleCard
import me.gamercoder215.battlecards.api.events.entity.CardUseAbilityEvent
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffectType
import java.io.Serializable
import java.security.SecureRandom

/**
 * Represents Card Equipment
 */
interface CardEquipment : Serializable {

    /**
     * Fetches the name of this CardEquipment.
     */
    val name: String

    /**
     * Fetches the item icon of this CardEquipment.
     */
    val item: Material

    /**
     * Fetches the multiplicative health modifier of this CardEquipment.
     */
    val healthModifier: Double

    /**
     * Fetches the multiplicative damage modifier of this CardEquipment.
     */
    val damageModifier: Double

    /**
     * Fetches the multiplicative defense modifier of this CardEquipment.
     */
    val defenseModifier: Double

    /**
     * Fetches the multiplicative speed modifier of this CardEquipment.
     */
    val speedModifier: Double

    /**
     * Fetches the multiplicative knockback resistance modifier of this CardEquipment.
     */
    val knockbackResistanceModifier: Double

    /**
     * Fetches the rarity of this CardEquipment.
     */
    val rarity: Rarity

    /**
     * Fetches the ability of this CardEquipment.
     */
    val ability: Ability?

    /**
     * Fetches the effects of this CardEquipment. These efefcts are only active when the Card is spawned.
     */
    val effects: Set<Potion>
        get() = emptySet()

    /**
     * Rarity for a [CardEquipment] Item
     */
    enum class Rarity(
        color: ChatColor
    ) {

        /**
         * Represents the Average Rarity
         */
        AVERAGE(ChatColor.WHITE),

        /**
         * Represents the Frequent Rarity
         */
        FREQUENT(ChatColor.DARK_GREEN),

        /**
         * Represents the Historical Rarity
         */
        HISTORICAL(ChatColor.DARK_AQUA),

        /**
         * Represents the Mythological Rarity
         */
        MYTHOLOGICAL(ChatColor.YELLOW),

        /**
         * Represents the Special Rarity
         */
        SPECIAL(ChatColor.RED)

        ;

        /**
         * Fetches the color of this rarity.
         */
        val color: ChatColor

        init {
            this.color = color
        }

        override fun toString(): String = "$color${ChatColor.BOLD}$name"

    }

    /**
     * Represents a [CardEquipment] Potion Effect
     * @param type The type of this Potion Effect
     * @param amplifier The level of this Potion Effect
     * @param status The Status of this Potion Effect
     */
    data class Potion(
        /**
         * Represents the Potion Effect Type
         */
        val type: PotionEffectType,
        /**
         * Represents the Potion Effect Amplifier
         */
        val amplifier: Int,
        /**
         * Whether the Potion Effect applies to the card user while active
         */
        val status: Status = Status.CARD_ONLY
    ) {

        /**
         * Represents the Potion Effect Status
         */
        enum class Status {

            /**
             * Effect only applies to the Card
             */
            CARD_ONLY,

            /**
             * Effect only applies to the Card User
             */
            USER_ONLY,

            /**
             * Effect applies to both the Card and its User
             */
            BOTH

        }

    }

    /**
     * Reprsents a [CardEquipment] Ability
     * @param name The name of this Ability
     * @param type The type of this Ability
     * @param probability The probability calculation of this Ability being used
     * @param action The action to perform when this Ability is used
     */
    class Ability(
        /**
         * Represents the name of this Ability
         */
        val name: String,
        /**
         * Represents the Ability Type
         */
        val type: CardUseAbilityEvent.AbilityType,
        probability: (BattleCard<*>) -> Double,
        action: (BattleCard<*>, EntityDamageByEntityEvent) -> Unit
    ) {

        private companion object {
            private val r = SecureRandom()
        }

        /**
         * The Function that represents the Ability Action
         */
        val action: (BattleCard<*>, EntityDamageByEntityEvent) -> Unit

        init {
            this.action = { card, event ->
                if (r.nextDouble() < probability(card).coerceAtMost(1.0))
                    action(card, event)
            }
        }

    }

}