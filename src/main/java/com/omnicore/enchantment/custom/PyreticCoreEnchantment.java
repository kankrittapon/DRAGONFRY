package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🔥 Pyretic Core (แกนกลางอัคคี)
 * เพิ่มอัตราการฮีลในไฟ/ลาวาของ Phoenix Soul และลดความเสียหายจากไอน้ำร้อนลง 30% ต่อระดับ
 */
public class PyreticCoreEnchantment extends Enchantment {
    public PyreticCoreEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR_CHEST, new EquipmentSlot[]{EquipmentSlot.CHEST});
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
