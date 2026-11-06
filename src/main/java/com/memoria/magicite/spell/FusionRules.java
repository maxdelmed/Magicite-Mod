package com.memoria.magicite.spell;

import java.util.List;

public final class FusionRules {
    private FusionRules() {}
    public static String result(List<MagiciteKind> kinds) { return result(kinds, false, false); }

    public static String result(List<MagiciteKind> kinds, boolean multiplayer, boolean late) {
        if (kinds == null || kinds.size() < 2 || kinds.size() > 4) return "";
        for (MagiciteKind kind : kinds) if (kind == null) return "";
        int n = kinds.size();
        if (kinds.contains(MagiciteKind.PURE_MEMORY)) return "sanctuary_of_memories";
        int fire=count(kinds,MagiciteKind.FIRE), ice=count(kinds,MagiciteKind.BLIZZARD), thunder=count(kinds,MagiciteKind.THUNDER);
        int elemental=fire+ice+thunder, life=count(kinds,MagiciteKind.LIFE);
        int cure=count(kinds,MagiciteKind.CURE), clear=count(kinds,MagiciteKind.CLEAR);
        if (elemental == n && (fire==n || ice==n || thunder==n)) {
            String middle = fire==n ? "fira" : ice==n ? "blizzara" : "thundara";
            String wide = fire==n ? "firaga" : ice==n ? "blizzaga" : "thundaga";
            if(n==2) return multiplayer && late ? wide : middle;
            if(n==3) return multiplayer && !late ? middle+"_plus1" : wide+(multiplayer ? "_plus1" : "");
            return multiplayer && !late ? middle+"_plus2" : wide+"_plus2";
        }
        if (cure==n) return "curaga";
        if (clear==n) return "clearaga";
        if (life==n) return "full_life";
        if (life==1 && cure==n-1 && n==3) return "haste";
        if (life==1 && cure==n-1 && n==4) return "hastega";
        if (elemental==n) {
            if (n==2 && fire<=1 && ice<=1 && thunder<=1) return "gravity";
            if (n==3 && fire==1 && ice==1 && thunder==1) return multiplayer && late ? "gravira" : "graviga";
            if (n==4 && distinctElements(kinds)>=2) return "gravira";
            return "";
        }
        if (life>=1 && elemental>=1 && life+elemental==n) {

            if (n==2) return kinds.get(0)==MagiciteKind.LIFE ? "slow" : "holy";
            if (n==3 && life==2 && elemental==1) return "holyra";
            if (n==4 && life==3 && elemental==1) return "holyra_plus1";
            if (n==3 && life==1 && elemental==2 && distinctElements(kinds)==2) {
                return multiplayer && !late ? "slowga" : "stop";
            }
        }
        return "";
    }
    public static int count(List<MagiciteKind> kinds,MagiciteKind kind) {
        int n=0; for(MagiciteKind k:kinds) if(k==kind)n++; return n;
    }
    private static int distinctElements(List<MagiciteKind> kinds) {
        return (kinds.contains(MagiciteKind.FIRE)?1:0)+(kinds.contains(MagiciteKind.BLIZZARD)?1:0)
                +(kinds.contains(MagiciteKind.THUNDER)?1:0);
    }
}
