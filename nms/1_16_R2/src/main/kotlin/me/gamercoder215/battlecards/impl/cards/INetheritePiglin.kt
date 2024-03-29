package me.gamercoder215.battlecards.impl.cards

import me.gamercoder215.battlecards.api.card.BattleCardType
import me.gamercoder215.battlecards.impl.*
import me.gamercoder215.battlecards.util.BattleSound
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Hoglin
import org.bukkit.entity.PiglinBrute
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Type(BattleCardType.NETHERITE_PIGLIN)
@Attributes(400.0, 11.0, 45.0, 0.22, 60.0)
@AttributesModifier(CardAttribute.MAX_HEALTH, CardOperation.ADD, 9.5)
@AttributesModifier(CardAttribute.ATTACK_DAMAGE, CardOperation.ADD, 2.75)
@AttributesModifier(CardAttribute.DEFENSE, CardOperation.ADD, 7.5)
class INetheritePiglin(data: ICard) : IBattleCard<PiglinBrute>(data) {

    override fun init() {
        super.init()

        entity.isImmuneToZombification = true

        val equipment = entity.equipment!!
        equipment.helmet = ItemStack(Material.NETHERITE_HELMET).apply {
            itemMeta = itemMeta!!.apply {
                isUnbreakable = true

                if (level >= 15)
                    addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, level / 15, true)
            }
        }

        if (level >= 45)
            equipment.chestplate = ItemStack(Material.NETHERITE_CHESTPLATE).apply {
                itemMeta = itemMeta!!.apply {
                    isUnbreakable = true

                    if (level >= 60)
                        addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, (level / 15) - 3, true)
                }
            }

        equipment.setItemInMainHand(ItemStack(when (level) {
            in 0 until 30 -> Material.NETHERITE_SWORD
            else -> Material.NETHERITE_AXE
        }).apply {
            itemMeta = itemMeta!!.apply {
                isUnbreakable = true

                if (level >= 20)
                    addEnchant(Enchantment.DAMAGE_ALL, level / 20, true)

                if (level >= 35)
                    addEnchant(Enchantment.KNOCKBACK, level / 35, true)
            }
        })
    }

    @CardAbility("card.netherite_piglin.ability.netherite_rage", ChatColor.RED)
    @Passive(400, CardOperation.SUBTRACT, 5, min = 300)
    private fun netheriteRage() {
        entity.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 5, (level / 15) + 3, true))
    }

    @CardAbility("card.netherite_piglin.ability.heat_shield", ChatColor.YELLOW)
    @Damage
    @UnlockedAt(30)
    private fun heatShield(event: EntityDamageEvent) {
        if (event.cause == EntityDamageEvent.DamageCause.LAVA)
            event.isCancelled = true
    }

    @CardAbility("card.netherite_piglin.ability.hoglin", ChatColor.LIGHT_PURPLE)
    @Defensive(0.1, CardOperation.ADD, 0.02, 0.25)
    @UnlockedAt(45)
    private fun hoglin(event: EntityDamageByEntityEvent) {
        event.isCancelled = true
        val sound = BattleSound.ITEM_SHIELD_BLOCK.findOrNull()
        if (sound != null) entity.world.playSound(entity.location, sound, 3F, 1F)

        minion(Hoglin::class.java) {
            isImmuneToZombification = true

            if (r.nextDouble() < 0.25) setBaby()
        }
    }

}