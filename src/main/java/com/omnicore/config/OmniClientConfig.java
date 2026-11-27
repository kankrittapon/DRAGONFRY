package com.omnicore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class OmniClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final OmniClientConfig INSTANCE;

    public enum HudCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public final ForgeConfigSpec.EnumValue<HudCorner> hudCorner;
    public final ForgeConfigSpec.IntValue hudOffsetX;
    public final ForgeConfigSpec.IntValue hudOffsetY;

    static {
        Pair<OmniClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(OmniClientConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    private OmniClientConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("==================================================================",
                        " OmniCore Client Configuration (.toml)",
                        " การตั้งค่าฝั่ง Client สำหรับ HUD และการแสดงผลหน้าจอ",
                        "==================================================================")
               .push("hud");

        hudCorner = builder
                .comment("ตำแหน่งมุมของปุ่ม PvP HUD บนหน้าจอ (TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)")
                .defineEnum("corner", HudCorner.TOP_LEFT);

        hudOffsetX = builder
                .comment("ระยะห่างจากขอบจอแกน X (พิกเซล, ค่าเริ่มต้น: 10)")
                .defineInRange("offsetX", 10, 0, 1000);

        hudOffsetY = builder
                .comment("ระยะห่างจากขอบจอแกน Y (พิกเซล, ค่าเริ่มต้น: 10)")
                .defineInRange("offsetY", 10, 0, 1000);

        builder.pop();
    }
}
