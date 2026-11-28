package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🗡️ Shadow Edge (คมดาบเงาพราง)
 * เพิ่มความเสียหาย Backstab ในโหมด Shadow Veil และลดระยะเวลาติดสถานะ Exposed
 */
public class ShadowEdgeEnchantment extends Enchantment {
    public ShadowEdgeEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) return true;
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)
                || super.canEnchant(stack);
    }
}
