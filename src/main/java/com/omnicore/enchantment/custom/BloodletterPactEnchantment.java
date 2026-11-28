package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🩸 Bloodletter's Pact (พันธสัญญาหลั่งโลหิต)
 * เพิ่มอัตราการดูดเลือด (Lifesteal) ในโหมด Berserk ขึ้นอีกระดับละ +3% แม้เลือดจะไม่ต่ำ
 */
public class BloodletterPactEnchantment extends Enchantment {
    public BloodletterPactEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 18 + (level - 1) * 12;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 30;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) return true;
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)
                || super.canEnchant(stack);
    }
}
