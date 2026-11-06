package com.memoria.magicite.spell;

import com.memoria.magicite.MemoriaMagicite;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class SpellSounds {
    private SpellSounds() {}
    public static void charge(ServerLevel level,Player caster,List<MagiciteKind> kinds) {
        MagiciteKind first=kinds.isEmpty()?null:kinds.get(0);
        float pitch=first==null?1.10f:switch(first) {
            case FIRE -> .90f;
            case BLIZZARD -> 1.28f;
            case THUNDER -> 1.48f;
            case CURE -> 1.18f;
            case CLEAR -> 1.32f;
            case LIFE -> .99f;
            case PURE_MEMORY -> 1.42f;
        };
        level.playSound(null,caster.blockPosition(),MemoriaMagicite.CHARGE.get(),SoundSource.PLAYERS,.32f,pitch);
        if(first==MagiciteKind.BLIZZARD || first==MagiciteKind.PURE_MEMORY)
            level.playSound(null,caster.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.12f,1.35f);
        if(first==MagiciteKind.THUNDER)
            level.playSound(null,caster.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,.12f,.85f);
    }
}
