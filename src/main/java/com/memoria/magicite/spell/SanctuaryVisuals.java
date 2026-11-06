package com.memoria.magicite.spell;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.List;

final class SanctuaryVisuals {
    private SanctuaryVisuals() {}

    static void sanctuaryDomeVisuals(ServerLevel world, Vec3 center, double radius,
                                              int harmony, long time, List<MagiciteKind> colors) {
        int[] palette = new int[] {0xFFFFFF, 0xFFE39A, 0xC9A7FF, 0x93F2FF, 0xFFD2EC};
        double yBase = center.y + 0.08d;
        int ringSteps = 22;
        int domeBands = 5;
        for (int band = 0; band <= domeBands; band++) {
            double phi = (Math.PI / 2.0d) * (band / (double) domeBands);
            double ringRadius = Math.sin(phi) * radius;
            double y = yBase + Math.cos(phi) * (radius * 0.72d);
            for (int step = 0; step < ringSteps; step++) {
                double angle = ((Math.PI * 2.0d) / ringSteps) * step + (time * 0.03d) + band * 0.15d;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;

                int rgb = palette[(step + band + harmony) % palette.length];
                if(!colors.isEmpty() && (step+2*band)%4==0) {
                    int elementColor=colors.get((step/4+band)%colors.size()).color;
                    rgb=blendColor(rgb,elementColor,0.37f);
                }
                Vector3f color = new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
                SpellParticles.send(world,new DustParticleOptions(color, band == domeBands ? 1.15f : 0.95f), x, y, z, 1, 0, 0, 0, 0);
            }
        }
        for (int step = 0; step < 26; step++) {
            double angle = ((Math.PI * 2.0d) / 26.0d) * step - time * 0.025d;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            Vector3f gold = new Vector3f(1.0f, 0.92f, 0.68f);
            SpellParticles.send(world,new DustParticleOptions(gold, 1.25f), x, yBase + 0.03d, z, 1, 0, 0, 0, 0);
        }
        SpellParticles.send(world,ParticleTypes.END_ROD, center.x, yBase + radius * 0.55d, center.z, 6 + harmony * 2, radius * 0.22d, radius * 0.12d, radius * 0.22d, 0.01d);
        SpellParticles.send(world,ParticleTypes.WAX_ON, center.x, yBase + 0.4d, center.z, 10 + harmony * 3, radius * 0.32d, 0.18d, radius * 0.32d, 0.01d);
    }

    static int blendColor(int original,int accent,float fraction) {
        int r=(int)(((original>>16)&255)*(1-fraction)+((accent>>16)&255)*fraction);
        int g=(int)(((original>>8)&255)*(1-fraction)+((accent>>8)&255)*fraction);
        int b=(int)((original&255)*(1-fraction)+(accent&255)*fraction);
        return (r<<16)|(g<<8)|b;
    }

    static void sanctuaryFormationWave(ServerLevel world,Vec3 center,double radius,
                                               MagiciteKind kind,int wave) {
        double bandRadius=radius*(0.32d+0.16d*wave);
        double y=center.y+0.18d+0.46d*wave;
        int rgb=blendColor(0xFFF8E7,kind.color,0.47f);
        Vector3f color=new Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f);
        DustParticleOptions sparkle=new DustParticleOptions(color,1.3f);
        for(int i=0;i<48;i++) {
            double theta=2d*Math.PI*i/48d;
            double x=center.x+Math.cos(theta)*bandRadius;
            double z=center.z+Math.sin(theta)*bandRadius;
            SpellParticles.send(world,sparkle,x,y,z,1,0.02,0.015,0.02,0.01);
            if(i%8==0)SpellParticles.send(world,ParticleTypes.END_ROD,x,y+0.12d,z,1,0.03,0.03,0.03,0.01);
        }
        world.playSound(null,center.x,center.y,center.z,
                SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,0.36f,1.05f+wave*0.13f);
    }
}
