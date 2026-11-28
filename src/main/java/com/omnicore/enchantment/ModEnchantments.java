package com.omnicore.enchantment;

import com.omnicore.OmniCoreMod;
import com.omnicore.enchantment.custom.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry ศูนย์รวม Enchantment ประจำ OmniCore
 * JEI จะดึงไปแสดงในรายการค้นหาและทั่งตีเหล็กอัตโนมัติ 100%
 * และ Apotheosis นำไปคำนวณในแท่นตีบวกและบอสอัตโนมัติ
 */
public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, OmniCoreMod.MOD_ID);

    public static final RegistryObject<Enchantment> MULTISLASH =
            ENCHANTMENTS.register("multislash", MultislashEnchantment::new);

    public static final RegistryObject<Enchantment> SHADOW_EDGE =
            ENCHANTMENTS.register("shadow_edge", ShadowEdgeEnchantment::new);

    public static final RegistryObject<Enchantment> BASTION_SPIKES =
            ENCHANTMENTS.register("bastion_spikes", BastionSpikesEnchantment::new);

    public static final RegistryObject<Enchantment> AERIAL_GRACE =
            ENCHANTMENTS.register("aerial_grace", AerialGraceEnchantment::new);

    public static final RegistryObject<Enchantment> PYRETIC_CORE =
            ENCHANTMENTS.register("pyretic_core", PyreticCoreEnchantment::new);

    public static final RegistryObject<Enchantment> EAGLES_EYE =
            ENCHANTMENTS.register("eagles_eye", EaglesEyeEnchantment::new);

    public static final RegistryObject<Enchantment> TESLA_COIL =
            ENCHANTMENTS.register("tesla_coil", TeslaCoilEnchantment::new);

    public static final RegistryObject<Enchantment> BLOODLETTER_PACT =
            ENCHANTMENTS.register("bloodletter_pact", BloodletterPactEnchantment::new);

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
