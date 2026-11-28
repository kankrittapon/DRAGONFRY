package com.omnicore.enchantment.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 🛡️ Bastion Spikes (ปราการหนามเหล็ก)
 * เพิ่มความเสียหายสะท้อนในโหมด Titan Bastion และลดอัตราการลดของหลอดอาหาร
 */
public class BastionSpikesEnchantment extends Enchantment {
    public BastionSpikesEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.ARMOR_CHEST, new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 8 + (level - 1) * 7;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem) return true;
        return super.canEnchant(stack);
    }
}
