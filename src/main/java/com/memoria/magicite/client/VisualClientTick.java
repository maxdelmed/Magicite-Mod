package com.memoria.magicite.client;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.net.MagiciteNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MemoriaMagicite.ID,value=Dist.CLIENT)
public final class VisualClientTick {
    private VisualClientTick() {}
    private static Level seenLevel;
    private static int lastQuality=-1;
    private static int ticks=0;
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null) {seenLevel=null;lastQuality=-1;return;}
        int quality=ClientVisualConfig.PARTICLE_QUALITY.get();
        if(seenLevel!=mc.level || lastQuality!=quality || ++ticks%1200==0) {
            seenLevel=mc.level;lastQuality=quality;
            MagiciteNetwork.sendQuality(quality);
        }
    }
}
