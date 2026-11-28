package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🏹 Eagle's Eye (สายตาพญาอินทรี)
 * ลดความมืดรอบข้างของ Marksman Focus และขยายเวลาสโลว์โมชั่น Bullet-time ขึ้นอีก 0.5 วินาทีต่อระดับ
 */
public class EaglesEyeEnchantment extends Enchantment {
    public EaglesEyeEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.BOW, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 9;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
