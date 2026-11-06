package com.memoria.magicite.spell;

public final class FusionNames {
    private FusionNames() {}
    public static String display(String id) {
        return switch (id) {
            case "fira_plus1" -> "Flarebrand";
            case "fira_plus2" -> "Wildfire";
            case "firaga_plus1" -> "Inferno";
            case "firaga_plus2" -> "Solar Flare";
            case "blizzara_plus1" -> "Frostbite";
            case "blizzara_plus2" -> "Whiteout";
            case "blizzaga_plus1" -> "Glacial Storm";
            case "blizzaga_plus2" -> "Absolute Zero";
            case "thundara_plus1" -> "Static Surge";
            case "thundara_plus2" -> "Stormcall";
            case "thundaga_plus1" -> "Thunderstorm";
            case "thundaga_plus2" -> "Stormbringer";
            case "holyra_plus1" -> "Divine Radiance";
            case "full_life" -> "Full-Life";
            case "sanctuary_of_memories" -> "Sanctuary of Memories";
            case "" -> "No Fusion";
            default -> readable(id);
        };
    }
    private static String readable(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.replace("-", "_").split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
