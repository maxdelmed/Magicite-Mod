package com.memoria.magicite.client;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientVisualConfig {
    private ClientVisualConfig() {}
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST=B.comment(
            "Use patterns and hatch marks on environmental targeting previews, not only color")
            .define("highContrastTargeting",false);
    public static final ForgeConfigSpec.IntValue PARTICLE_QUALITY=B.comment(
            "Ambient Magicite particles: 0=low, 1=balanced, 2=full; never hides necessary targeting cues")
            .defineInRange("ambientParticleQuality",2,0,2);
    public static final ForgeConfigSpec SPEC=B.build();
}
