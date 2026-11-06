package com.memoria.magicite;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagiciteConfig {
    private MagiciteConfig() {}
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue LIFE_DURATION = B.defineInRange("lifeDurationTicks", 200, 20, 1200);
    public static final ForgeConfigSpec.IntValue LIFE_TRIGGER_LOCK = B.defineInRange("lifeTriggerLockTicks", 1200, 0, 24000);
    public static final ForgeConfigSpec.DoubleValue SACRIFICE_HEALTH_FRACTION = B.defineInRange("sacrificeMaxHealthCostFraction", 0.80d, 0.10d, 1.0d);
    public static final ForgeConfigSpec.IntValue FUSION_WINDOW = B.defineInRange("fusionWindowTicksV011", 16, 2, 40);
    public static final ForgeConfigSpec.IntValue FUSION_JITTER_GRACE = B.defineInRange("fusionHighPingJitterGraceTicks",2,0,2);
    public static final ForgeConfigSpec.DoubleValue FUSION_RANGE = B.defineInRange("fusionRangeBlocks", 2.5d, 0.5d, 10d);

    public static final ForgeConfigSpec.IntValue REPAIR_AMOUNT = B.defineInRange("repairAmountV011", 20, 1, 30);
    public static final ForgeConfigSpec.IntValue TIER_TWO_XP = B.defineInRange("gauntletTier2CostLevels", 18, 1, 100);
    public static final ForgeConfigSpec.IntValue TIER_THREE_XP = B.defineInRange("gauntletTier3CostLevels", 35, 1, 100);
    public static final ForgeConfigSpec.IntValue TIER_FOUR_XP = B.defineInRange("gauntletTier4CostLevels", 60, 1, 200);
    public static final ForgeConfigSpec.BooleanValue AUTO_ALL_PLAYERS_ALLIES = B.define("allPlayersFriendly", true);

    public static final ForgeConfigSpec.DoubleValue DAMAGE_BASE = B.defineInRange("damageBaseElementalHp", 4d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_TWO = B.defineInRange("damageTwoElementalHp", 7d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_THREE = B.defineInRange("damageThreeElementalHp", 10d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MINOR_PLUS_ONE = B.defineInRange("damageFiraPlusOneHp", 9d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MINOR_PLUS_TWO = B.defineInRange("damageFiraPlusTwoHp", 11d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PLUS_ONE = B.defineInRange("damageFiragaPlusOneHp", 12d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PLUS_TWO = B.defineInRange("damageFiragaPlusTwoHp", 14d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_HOLY = B.defineInRange("damageHolyHp", 8d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_HOLYRA = B.defineInRange("damageHolyraHp", 12d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_HOLYRA_PLUS = B.defineInRange("damageHolyraPlusOneHp", 18d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue HOLY_UNDEAD_MULTIPLIER = B.defineInRange("holyUndeadDamageMultiplier", 1.6d, 1d, 5d);
    public static final ForgeConfigSpec.BooleanValue DEBUG_HOLY_HITS = B.define("debugHolyHits", false);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_GRAVITY = B.defineInRange("damageGravityHp", 2d, 0d, 100d);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_GRAVIRA = B.defineInRange("damageAdvancedGravityHp", 5d, 0d, 100d);

    public static final ForgeConfigSpec.IntValue COOLDOWN_ONE = B.defineInRange("castingCooldownSingleTicks", 36, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN_TWO = B.defineInRange("castingCooldownDualTicks", 46, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN_THREE = B.defineInRange("castingCooldownTripleTicks", 56, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN_FOUR = B.defineInRange("castingCooldownQuadTicks", 68, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN_CURE_MIN = B.defineInRange("castingCooldownHealingMinimumTicks", 70, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOLDOWN_LIFE_MIN = B.defineInRange("castingCooldownLifeWardMinimumTicks", 100, 0, 1200);

    public static final ForgeConfigSpec.BooleanValue GRAVITY_AFFECTS_PLAYERS = B.define("gravityAffectsPlayers", true);
    public static final ForgeConfigSpec.DoubleValue GRAVITY_DOWNWARD_IMPULSE = B.defineInRange("gravityDownwardImpulse", 0.55d, 0d, 2d);
    public static final ForgeConfigSpec.DoubleValue GRAVITY_HORIZONTAL_PULL = B.defineInRange("gravityHorizontalPull", 0.22d, 0d, 1.5d);
    public static final ForgeConfigSpec.DoubleValue GRAVITY_ADVANCED_MULTIPLIER = B.defineInRange("gravityAdvancedImpulseMultiplier", 1.40d, 1d, 3d);
    public static final ForgeConfigSpec.BooleanValue GRAVITY_ATTRACT_ITEMS = B.define("gravityAttractsItems", true);
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_THRESHOLD = B.defineInRange("bossControlHealthThresholdHp", 80d, 1d, 100000d);
    public static final ForgeConfigSpec.IntValue BOSS_CONTROL_TICKS = B.defineInRange("bossControlTicks", 20, 0, 200);

    public static final ForgeConfigSpec.DoubleValue COOP_TWO_POWER = B.defineInRange("cooperativeTwoPlayerPower", 1.20d, 1d, 3d);
    public static final ForgeConfigSpec.DoubleValue COOP_THREE_POWER = B.defineInRange("cooperativeThreePlayerPower", 1.35d, 1d, 3d);
    public static final ForgeConfigSpec.DoubleValue COOP_FOUR_POWER = B.defineInRange("cooperativeFourPlayerPower", 1.50d, 1d, 3d);
    public static final ForgeConfigSpec.DoubleValue COOP_RADIUS_PER_PLAYER = B.defineInRange("cooperativeRadiusPerExtraPlayer", 0.35d, 0d, 2d);
    public static final ForgeConfigSpec.DoubleValue COOP_HEAL_PER_PLAYER = B.defineInRange("cooperativeHealingPerExtraPlayer", 0.12d, 0d, 1d);
    public static final ForgeConfigSpec.DoubleValue HEAL_MISSING_HEALTH_FRACTION = B.defineInRange("healingMissingHealthFraction", 0.15d, 0d, 1d);
    public static final ForgeConfigSpec.DoubleValue CURE_HEAL_BONUS_CAP = B.defineInRange("cureMissingHealthBonusCapHp", 5d, 0d, 50d);
    public static final ForgeConfigSpec.DoubleValue CURAGA_HEAL_BONUS_CAP = B.defineInRange("curagaMissingHealthBonusCapHp", 7d, 0d, 50d);
    public static final ForgeConfigSpec.IntValue COOP_REGEN_TICKS = B.defineInRange("cooperativeCuragaRegenTicks", 100, 0, 1200);
    public static final ForgeConfigSpec.IntValue COOP_GROUND_TICKS = B.defineInRange("cooperativeGravityGroundTicks", 40, 0, 200);
    public static final ForgeConfigSpec.BooleanValue COOP_THUNDER_CHAIN = B.define("cooperativeThunderChain", true);
    public static final ForgeConfigSpec.BooleanValue UTILITY_FIRE = B.define("fireLightsCampfiresAndCandles", true);
    public static final ForgeConfigSpec.BooleanValue UTILITY_BLIZZARD = B.define("blizzardTemporaryIce", true);
    public static final ForgeConfigSpec.IntValue ICE_DURATION = B.defineInRange("blizzardIceDurationTicks", 200, 20, 2400);
    public static final ForgeConfigSpec.BooleanValue UTILITY_THUNDER = B.define("thunderPowersReceivers", true);
    public static final ForgeConfigSpec.IntValue THUNDER_PULSE_TICKS = B.defineInRange("thunderPulseTicks", 12, 2, 100);

    public static final ForgeConfigSpec.BooleanValue MINOR_MAGIC = B.define("minorMagicEnabled",true);
    public static final ForgeConfigSpec.IntValue MINOR_COOLDOWN = B.defineInRange("minorMagicCooldownTicks",12,0,200);
    public static final ForgeConfigSpec.BooleanValue FIRE_PLACE_BLOCKS = B.define("fireFusionPlacesFire",true);
    public static final ForgeConfigSpec.BooleanValue COSMETIC_LIGHTNING = B.define("thunderVisualLightning",true);
    public static final ForgeConfigSpec.DoubleValue THUNDER_WET_BONUS = B.defineInRange("thunderWetTargetBonusV0111",0.25d,0d,2d);
    public static final ForgeConfigSpec.DoubleValue THUNDER_STORM_BONUS = B.defineInRange("thunderStormBonusV0111",0.15d,0d,2d);
    public static final ForgeConfigSpec.DoubleValue FIRE_NETHER_BONUS = B.defineInRange("fireNetherBonusV0111",0.20d,0d,2d);
    public static final ForgeConfigSpec.BooleanValue FIRE_MELTS_TERRAIN = B.define("fireMeltsSnowAndIce",true);
    public static final ForgeConfigSpec.BooleanValue FIRE_COOKS_FOOD = B.define("fireCooksDroppedFood",true);
    public static final ForgeConfigSpec.BooleanValue FIRE_ACCELERATES_FURNACES = B.define("fireAcceleratesActiveFurnaces",true);
    public static final ForgeConfigSpec.BooleanValue BLIZZARD_EXTINGUISH_ENTITIES = B.define("blizzardExtinguishesBurningEntities",true);
    public static final ForgeConfigSpec.BooleanValue BLIZZARD_WATERFALLS = B.define("blizzagaFreezesWaterfalls",true);
    public static final ForgeConfigSpec.BooleanValue BLIZZARD_LAVA_BRIDGES = B.define("blizzardTemporaryLavaBridges",true);
    public static final ForgeConfigSpec.BooleanValue GRAVITY_XP_ORBS = B.define("gravityPullsExperienceOrbs",true);
    public static final ForgeConfigSpec.BooleanValue THUNDER_WET_CHAINS = B.define("thunderWetChainExtension",true);
    public static final ForgeConfigSpec.BooleanValue VILLAGER_MAGIC = B.define("villagerMinorMagic",true);
    public static final ForgeConfigSpec.IntValue VILLAGER_CASTS = B.defineInRange("villagerMaxMagicPerDay",3,0,16);
    public static final ForgeConfigSpec.IntValue COOP_BRIDGE_TICKS = B.defineInRange("cooperativeIceBridgeTicks",500,100,2400);

    public static final ForgeConfigSpec SPEC = B.build();
}
