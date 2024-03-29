package me.gamercoder215.battlecards.impl

import me.gamercoder215.battlecards.api.card.BattleCardType
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.lang.annotation.Inherited

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Type(
    val type: BattleCardType
)

// Attributes

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Attributes(
    val maxHealth: Double,
    val attackDamage: Double,
    val defense: Double,
    val speed: Double,
    val knockbackResistance: Double,
    val followRange: Double = 64.0
)

@Inherited
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class AttributesModifier(
    val attribute: CardAttribute,
    val operation: CardOperation,
    val value: Double = Double.NaN,
    val max: Double = Double.MAX_VALUE
)

enum class CardAttribute(
    val max: Double = Double.MAX_VALUE
) {
    MAX_HEALTH(25_000_000.0),
    ATTACK_DAMAGE(3_500_000.0),
    DEFENSE(2_000_000.0),
    SPEED(1.05),
    KNOCKBACK_RESISTANCE,
    FOLLOW_RANGE(1024.0)

    ;

    fun getAttribute(attributes: Attributes): Double {
        return when (this) {
            MAX_HEALTH -> attributes.maxHealth
            ATTACK_DAMAGE -> attributes.attackDamage
            DEFENSE -> attributes.defense
            SPEED -> attributes.speed
            KNOCKBACK_RESISTANCE -> attributes.knockbackResistance
            FOLLOW_RANGE -> attributes.followRange
        }
    }
}

enum class CardOperation(
    private val apply: (Double, Double) -> Double
) : (Double, Double) -> Double by apply {
    ADD({ a, b -> a + b }),
    MULTIPLY({ a, b -> a * b }),
    DIVIDE({ a, b -> a / b }),
    SUBTRACT({ a, b -> a - b }),
}

// Abilities

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class CardAbility(
    val name: String,
    val color: ChatColor = ChatColor.WHITE,
    val desc: String = "<desc>"
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Defensive(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Damage(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Offensive(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Passive(
    val interval: Long,
    val operation: CardOperation = CardOperation.ADD,
    val value: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE,
    val min: Long = 1
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UnlockedAt(val level: Int)

// Visuals

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BlockAttachment(
    val material: Material,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val offsetYaw: Float = 0.0F,
    val small: Boolean = false,
    val local: Boolean = true
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class MinionBlockAttachment(
    val type: EntityType,
    val material: Material,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val offsetYaw: Float = 0.0F,
    val small: Boolean = false,
    val local: Boolean = true
)

// User Grants

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UserDefensive(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UserOffensive(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UserDamage(
    val chance: Double = 1.0,
    val operation: CardOperation = CardOperation.ADD,
    val value: Double = Double.NaN,
    val max: Double = 1.0
)

// Other

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Rideable