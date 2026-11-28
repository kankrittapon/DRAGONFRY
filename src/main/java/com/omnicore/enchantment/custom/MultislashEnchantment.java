package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * ⚔️ Multislash (มัลติสแลช / คมดาบกระจายคลื่น)
 * ยิงคลื่นคมดาบเสริมออกไปเป็นรูปพัดแบบ Multishot (2 - 5 แฉก)
 * รองรับดาบ อาวุธตีไกล และอาวุธ Runic จาก Simply Swords
 */
public class MultislashEnchantment extends Enchantment {
    public MultislashEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
            return true;
        }
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)
                || super.canEnchant(stack);
    }
}
