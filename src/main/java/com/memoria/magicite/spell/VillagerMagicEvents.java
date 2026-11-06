package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.MemoriaMagicite;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public final class VillagerMagicEvents {
    private static final String DAY="MemoriaMagicDay", COUNT="MemoriaMagicDailyCount";
    @SubscribeEvent public void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel world)
                || !MagiciteConfig.VILLAGER_MAGIC.get() || !villager.isAlive() || villager.isSleeping())return;
        long tick=world.getGameTime();
        if(Math.floorMod(tick+villager.getUUID().hashCode(),180)!=0)return;
        long dayTime=world.getDayTime()%24000L;
        if(dayTime<2000 || dayTime>9000)return;
        long day=world.getDayTime()/24000L;
        if(villager.getPersistentData().getLong(DAY)!=day) {
            villager.getPersistentData().putLong(DAY,day);
            villager.getPersistentData().putInt(COUNT,0);
        }
        if(villager.getPersistentData().getInt(COUNT)>=MagiciteConfig.VILLAGER_CASTS.get())return;
        VillagerProfession job=villager.getVillagerData().getProfession();
        MagiciteKind kind;
        boolean success=false;
        AABB vicinity=villager.getBoundingBox().inflate(5d);
        if(job==VillagerProfession.FARMER) {
            kind=MagiciteKind.CURE;
            List<Animal> animals=world.getEntitiesOfClass(Animal.class,vicinity,
                    a->a.isAlive() && a.getHealth()<a.getMaxHealth()-0.25f);
            animals.sort(Comparator.comparingDouble(villager::distanceToSqr));
            if(!animals.isEmpty()) {animals.get(0).heal(4f);success=true;}
        } else if(job==VillagerProfession.CLERIC) {
            kind=MagiciteKind.CURE;
            List<Villager> patients=world.getEntitiesOfClass(Villager.class,vicinity,
                    v->v!=villager && v.isAlive() && v.getHealth()<v.getMaxHealth()-0.25f);
            patients.sort(Comparator.comparingDouble(villager::distanceToSqr));
            if(!patients.isEmpty()) {patients.get(0).heal(4f);success=true;}
        } else if(job==VillagerProfession.ARMORER || job==VillagerProfession.WEAPONSMITH
                || job==VillagerProfession.TOOLSMITH) {
            kind=MagiciteKind.FIRE;
            success=SpellWorldInteractions.villagerFire(world,villager.blockPosition());
        } else if(job==VillagerProfession.FISHERMAN) {
            kind=MagiciteKind.BLIZZARD;
            success=SpellWorldInteractions.villagerBlizzard(world,villager.blockPosition());
        } else if(job==VillagerProfession.LIBRARIAN) {
            kind=MagiciteKind.CLEAR;
            List<Villager> patients=world.getEntitiesOfClass(Villager.class,vicinity,
                    v->v!=villager && v.isAlive() && v.getActiveEffects().stream().anyMatch(
                            fx->fx.getEffect().getCategory()==MobEffectCategory.HARMFUL));
            patients.sort(Comparator.comparingDouble(villager::distanceToSqr));
            if(!patients.isEmpty()) {
                for(MobEffectInstance effect:List.copyOf(patients.get(0).getActiveEffects()))
                    if(effect.getEffect().getCategory()==MobEffectCategory.HARMFUL)
                        patients.get(0).removeEffect(effect.getEffect());
                success=true;
            }
        } else return;
        if(!success)return;
        villager.getPersistentData().putInt(COUNT,villager.getPersistentData().getInt(COUNT)+1);
        Vec3 at=villager.position().add(0,1.35,0);
        int c=kind.color;
        SpellParticles.send(world,new DustParticleOptions(new Vector3f((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f),1.15f),
                at.x,at.y,at.z,12,.30,.30,.30,.015);
        world.playSound(null,villager.blockPosition(),MemoriaMagicite.ELEMENT_SOUNDS.get(kind).get(),
                SoundSource.NEUTRAL,.38f,1.35f);
    }
}
