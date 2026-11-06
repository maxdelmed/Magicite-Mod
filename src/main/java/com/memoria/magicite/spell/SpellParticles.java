package com.memoria.magicite.spell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SpellParticles {
    private SpellParticles() {}
    private static final Map<UUID,Integer> QUALITY=new HashMap<>();
    public static void setQuality(ServerPlayer player,int level) {
        QUALITY.put(player.getUUID(),Math.max(0,Math.min(2,level)));
    }
    public static <T extends ParticleOptions> void send(ServerLevel world,T type,double x,double y,double z,
                int count,double dx,double dy,double dz,double speed) {
        for(ServerPlayer viewer:world.players()) {
            double distance=viewer.distanceToSqr(x,y,z);
            if(distance>64d*64d)continue;
            int quality=QUALITY.getOrDefault(viewer.getUUID(),2);
            if(quality==0 && distance>24d*24d)continue;
            if(quality==1 && distance>40d*40d)continue;

            if(count<=1 && quality<2) {
                long hash=Double.doubleToLongBits(x*13.13d+z*7.07d+y*3.33d)
                        ^ (world.getGameTime()*0x9E3779B97F4A7C15L);
                int mask=quality==0?3:1;
                if((hash&mask)!=0L)continue;
            }
            int visible=quality==2?count : quality==1?Math.max(1,(count+1)/2):Math.max(1,(count+3)/4);
            world.sendParticles(viewer,type,false,x,y,z,visible,dx,dy,dz,speed);
        }
    }
}
