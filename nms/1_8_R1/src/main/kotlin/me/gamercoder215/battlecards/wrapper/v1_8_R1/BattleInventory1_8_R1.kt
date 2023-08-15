package me.gamercoder215.battlecards.wrapper.v1_8_R1

import me.gamercoder215.battlecards.wrapper.BattleInventory
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftInventoryCustom

internal class BattleInventory1_8_R1(
    id: String,
    name: String,
    size: Int
) : CraftInventoryCustom(null, size, name), BattleInventory {

    override val attributes = mutableMapOf<String, Any?>()

    init {
        set("_id", id)
        set("_name", name)
    }

    override fun set(key: String, value: Any?) {
        attributes[key] = value
    }

    override fun getTitle() = super<BattleInventory>.getTitle()

}