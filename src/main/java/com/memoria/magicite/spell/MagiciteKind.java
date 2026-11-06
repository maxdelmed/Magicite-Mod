package com.memoria.magicite.spell;

import java.util.Arrays;

public enum MagiciteKind {
    FIRE("fire", 60, 0xFF5E37),
    BLIZZARD("blizzard", 60, 0x69D6FF),
    THUNDER("thunder", 60, 0xA46EFF),
    PURE_MEMORY("pure_memory", 12, 0xFFF2D8),
    CURE("cure", 60, 0x61E5AB),
    CLEAR("clear", 60, 0xF1F9F5),
    LIFE("life", 15, 0xFFCAAA);

    public final String id;
    public final int durability;
    public final int color;

    MagiciteKind(String id, int durability, int color) {
        this.id = id; this.durability = durability; this.color = color;
    }

    public boolean isSupport() { return this == PURE_MEMORY || this == CURE || this == CLEAR || this == LIFE; }

    public static MagiciteKind find(String id) {
        return Arrays.stream(values()).filter(k -> k.id.equals(id)).findFirst().orElse(null);
    }

}
