package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🪶 Aerial Grace (ขนนกเหินเวหา)
 * ลดแรงผลักกระเด็นกลางอากาศของ Zephyr Feather ลง 50% และเพิ่มระยะพุ่งตัวของ Double Jump
 */
public class AerialGraceEnchantment extends Enchantment {
    public AerialGraceEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR_FEET, new EquipmentSlot[]{EquipmentSlot.FEET});
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 12;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}
