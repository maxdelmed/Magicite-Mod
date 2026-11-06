package com.memoria.magicite.spell;

public final class FusionDescriptions {
    private FusionDescriptions() {}
    public static String forFusion(String id) {
        return switch(id) {
            case "fira", "blizzara", "thundara" -> "Focused elemental attack";
            case "firaga", "blizzaga", "thundaga" -> "Wide elemental explosion";
            case "fira_plus1", "blizzara_plus1", "thundara_plus1" -> "Surging elemental strike";
            case "fira_plus2", "blizzara_plus2", "thundara_plus2" -> "Powerful elemental burst";
            case "firaga_plus1", "blizzaga_plus1", "thundaga_plus1" -> "Advanced elemental area blast";
            case "firaga_plus2", "blizzaga_plus2", "thundaga_plus2" -> "Devastating elemental explosion";
            case "gravity" -> "Pulls creatures and players down";
            case "gravira", "graviga" -> "Slams foes; draws drops inward";
            case "curaga" -> "Restores nearby allies";
            case "clearaga" -> "Cleanses nearby allies";
            case "full_life" -> "Strong Life protection";
            case "holy" -> "Sacred blast; strong versus undead";
            case "holyra" -> "Wide holy burst; hurts undead more";
            case "holyra_plus1" -> "Massive holy burst versus undead";
            case "slow", "slowga" -> "Slows enemies in the area";
            case "stop" -> "Briefly stops nearby foes";
            case "haste", "hastega" -> "Speeds allied movement";
            case "sanctuary_of_memories" -> "A radiant dome that heals allies, weakens foes, and lingers beautifully";
            default -> "Separate base spells";
        };
    }
    public static String forSingle(MagiciteKind kind) {
        return switch(kind) {
            case FIRE -> "Mysterious orb filled with a swirling flame.";
            case BLIZZARD -> "Mysterious orb filled with swirling snow.";
            case THUNDER -> "Mysterious orb filled with a swirling storm.";
            case PURE_MEMORY -> "The first laugh of a baby,\nthe final breath of a dying sun,\nthe electric rush of running free,\nand snowy fields where children run.";
            case CURE -> "Mysterious orb filled with mana.";
            case CLEAR -> "Mysterious orb filled with purifying light.";
            case LIFE -> "Mysterious orb filled with life essence.";
        };
    }
}
