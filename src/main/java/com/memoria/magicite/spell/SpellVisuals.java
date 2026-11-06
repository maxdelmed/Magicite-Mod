package com.memoria.magicite.spell;

import com.memoria.magicite.MemoriaMagicite;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraft.server.MinecraftServer;
final class SpellVisuals {
    private SpellVisuals() {}

    static void tickLifeHalos(MinecraftServer server) {
        for(ServerLevel world:server.getAllLevels()) {
            if(world.getGameTime()%16L!=0L)continue;
            DustParticleOptions pearl=new DustParticleOptions(new Vector3f(.95f,.92f,.83f),.65f);
            for(ServerPlayer player:world.players()) {
                if(player.isSpectator() || !player.isAlive() ||
                        player.getPersistentData().getLong(SpellEffects.LIFE_EXPIRY)<=world.getGameTime())continue;
                double centerY=player.getY()+player.getBbHeight()+.16d;
                for(int i=0;i<7;i++) {
                    double angle=2d*Math.PI*i/7d + world.getGameTime()*.015d;
                    double x=player.getX()+Math.cos(angle)*.32d;
                    double z=player.getZ()+Math.sin(angle)*.32d;
                    SpellParticles.send(world,pearl,x,centerY,z,1,0d,0d,0d,0d);
                }
            }
        }
    }

    static void blockedSpellImpact(ServerLevel world,LivingEntity target) {
        SpellParticles.send(world,ParticleTypes.CRIT,target.getX(),
                target.getY()+target.getBbHeight()*.6d,target.getZ(),8,.18d,.17d,.18d,.025d);
        SpellParticles.send(world,ParticleTypes.ENCHANT,target.getX(),
                target.getY()+target.getBbHeight()*.6d,target.getZ(),3,.12d,.12d,.12d,.01d);
    }

    static void healingRibbon(ServerPlayer caster,LivingEntity recipient) {
        ServerLevel world=caster.serverLevel();
        Vec3 start=caster.getEyePosition().add(caster.getLookAngle().scale(.32d)).add(0,-.22d,0);
        Vec3 end=recipient.position().add(0,recipient.getBbHeight()*.58d,0);
        for(int i=0;i<=14;i++) {
            double t=i/14d;
            Vec3 point=start.lerp(end,t).add(0,Math.sin(t*Math.PI)*.15d,0);
            SpellParticles.send(world,ParticleTypes.END_ROD,point.x,point.y,point.z,
                2,.035d,.035d,.035d,.003d);
        }
    }

    static void visualLightning(ServerLevel world,Vec3 position) {
        LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(world);
        if(bolt==null)return;
        bolt.moveTo(position.x,position.y,position.z);
        bolt.setVisualOnly(true);
        bolt.setDamage(0f);
        world.addFreshEntity(bolt);
    }

    static void areaBurst(ServerLevel world, Vec3 loc, double radius, int rgb, MagiciteKind kind,
                                  int budget, boolean unstable) {
        Vector3f color = new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        DustParticleOptions colored = new DustParticleOptions(color, unstable ? 1.5f : 1.15f);
        int steps = Math.min(35, Math.max(16, budget / 2));

        for (int i = 0; i < steps; i++) {
            double angle = i * 2.399963229728653;
            double radial = radius * Math.sqrt((i + .5) / steps);
            double x = loc.x + Math.cos(angle) * radial;
            double z = loc.z + Math.sin(angle) * radial;
            double y = loc.y + .13 + .4 * ((i * 7 % 11) / 10.0);
            SpellParticles.send(world,colored, x, y, z, 2, .09, .22, .09, .015);
            if (kind == MagiciteKind.FIRE && i % 3 == 0)
                SpellParticles.send(world,ParticleTypes.FLAME, x, y + .12, z, 2, .12, .18, .12, .02);
            else if (kind == MagiciteKind.BLIZZARD && i % 3 == 0)
                SpellParticles.send(world,ParticleTypes.SNOWFLAKE, x, y + .12, z, 2, .12, .18, .12, .018);
            else if (kind == MagiciteKind.THUNDER && i % 2 == 0)
                SpellParticles.send(world,ParticleTypes.ELECTRIC_SPARK, x, y + .12, z, 2, .16, .15, .16, .06);
            else if (unstable && i % 4 == 0)
                SpellParticles.send(world,ParticleTypes.END_ROD, x, y + .1, z, 1, .08, .08, .08, .035);
        }
        int edge = Math.min(24, Math.max(10, (int)(radius * 5)));
        for (int i = 0; i < edge; i++) {
            double angle = i * Math.PI * 2 / edge;
            double x = loc.x + Math.cos(angle) * radius;
            double z = loc.z + Math.sin(angle) * radius;
            SpellParticles.send(world,colored, x, loc.y + .10, z, unstable ? 4 : 2, .07, .12, .07, .035);
        }
        SpellParticles.send(world,colored, loc.x, loc.y + .25, loc.z, 9, .30, .35, .30, .11);
    }

    static void sparkle(ServerLevel world, Vec3 loc, int rgb, int count) {
        Vector3f color = new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        SpellParticles.send(world,new DustParticleOptions(color, 1.25f), loc.x, loc.y, loc.z, count, 0.75, 0.35, 0.75, 0.035);
    }

    static void soundSpell(ServerLevel world, Vec3 pos, MagiciteKind kind, String fusion, int power) {
        if (kind != null) world.playSound(null, pos.x, pos.y, pos.z,
                MemoriaMagicite.ELEMENT_SOUNDS.get(kind).get(), SoundSource.PLAYERS, 0.8f, 1f);
        if (power > 1) {
            world.playSound(null, pos.x, pos.y, pos.z,
                    power >= 3 ? MemoriaMagicite.POWER_THREE.get() : MemoriaMagicite.POWER_TWO.get(),
                    SoundSource.PLAYERS, power >= 3 ? 1.1f : 0.85f, 1.0f);
        }
        if (fusion.equals("gravity") || fusion.startsWith("gravi"))
            world.playSound(null, pos.x, pos.y, pos.z, MemoriaMagicite.FUSION.get(), SoundSource.PLAYERS, 0.7f, 0.85f);
    }
}
