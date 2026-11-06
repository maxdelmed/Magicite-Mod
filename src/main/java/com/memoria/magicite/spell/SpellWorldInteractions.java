package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.MemoriaMagicite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Map;

public final class SpellWorldInteractions {
    private SpellWorldInteractions() {}
    public static final TagKey<Block> THUNDER_RECEIVERS = TagKey.create(Registries.BLOCK,
            new ResourceLocation(MemoriaMagicite.ID,"thunder_receivers"));

    public static int cleanupExpired(ServerLevel world) { return WorldTemporaryState.cleanupExpired(world); }
    public static boolean clearDecorativeSnow(ServerLevel world,BlockPos pos) { return WorldTemporaryState.clearDecorativeSnow(world,pos); }
    public static String diagnostic(ServerLevel world) { return WorldTemporaryState.diagnostic(world); }
    public static void tick(MinecraftServer server) { WorldTemporaryState.tick(server); }

    private static final Map<Item,Item> COOKED_FOODS=Map.of(
        Items.BEEF,Items.COOKED_BEEF, Items.PORKCHOP,Items.COOKED_PORKCHOP,
        Items.CHICKEN,Items.COOKED_CHICKEN, Items.MUTTON,Items.COOKED_MUTTON,
        Items.RABBIT,Items.COOKED_RABBIT, Items.COD,Items.COOKED_COD,
        Items.SALMON,Items.COOKED_SALMON);

    public static void cast(ServerLevel world,Vec3 at,MagiciteKind element,int participants,int tier,Vec3 direction,double radius) {
        if(element==MagiciteKind.FIRE && MagiciteConfig.UTILITY_FIRE.get()) {
            WorldUtilities.ignite(world,at,tier,participants,radius);

            for (FootprintCell cell : cooperativeFootprint(world,at,element,participants,tier,direction))
                if (cell.valid()) WorldUtilities.fireAt(world,cell.pos());
            if(MagiciteConfig.FIRE_MELTS_TERRAIN.get()) meltTerrain(world,at,tier,participants);
            if(MagiciteConfig.FIRE_COOKS_FOOD.get()) cookDroppedFood(world,at,radius,tier,participants);
            if(MagiciteConfig.FIRE_ACCELERATES_FURNACES.get())
                WorldTemporaryState.boostNearbyFurnaces(world,at,Math.min(4.0d,radius),Math.min(3,1+participants));
        }
        else if(element==MagiciteKind.BLIZZARD && MagiciteConfig.UTILITY_BLIZZARD.get()) {
            freeze(world,at,tier,participants,direction,radius);
            WorldUtilities.extinguishFireCluster(world,at,tier);
            temporarySnowfall(world,at,tier);
        }
        else if(element==MagiciteKind.THUNDER && MagiciteConfig.UTILITY_THUNDER.get()) pulse(world,at,tier,participants,radius);
    }

    private static void temporarySnowfall(ServerLevel world,Vec3 center,int tier) {
        BlockPos origin=BlockPos.containing(center);
        int radius=Math.min(3,Math.max(2,tier));
        int placed=0,max=24;
        for(int x=-radius;x<=radius && placed<max;x++)for(int z=-radius;z<=radius && placed<max;z++) {
            if(x*x+z*z>radius*radius)continue;
            for(int dy=2;dy>=-2;dy--) {
                BlockPos pos=origin.offset(x,dy,z);
                if(!world.hasChunkAt(pos) || !world.hasChunkAt(pos.below()))continue;
                BlockState current=world.getBlockState(pos);
                if(!current.isAir() || !world.getFluidState(pos).isEmpty())continue;
                BlockState snow=Blocks.SNOW.defaultBlockState();
                if(!snow.canSurvive(world,pos))continue;

                int fadeOffset=Math.floorMod(pos.getX()*31+pos.getZ()*17,81);
                WorldTemporaryState.setTemporary(world,pos,current,snow,800-fadeOffset);
                SpellParticles.send(world,ParticleTypes.SNOWFLAKE,pos.getX()+.5,pos.getY()+.15,pos.getZ()+.5,
                        2,.2,.08,.2,.008);
                placed++;
                break;
            }
        }
    }

    public record FootprintCell(BlockPos pos, boolean valid) {}

    public static List<FootprintCell> cooperativeFootprint(ServerLevel world, Vec3 at,
                MagiciteKind kind, int participants, int tier, Vec3 direction) {
        List<FootprintCell> cells=new ArrayList<>();
        if(participants<2 || (kind==MagiciteKind.BLIZZARD &&
                (!MagiciteConfig.UTILITY_BLIZZARD.get() || !BlizzardBalance.canBridge(tier,participants))) ||
                (kind==MagiciteKind.FIRE && (!MagiciteConfig.UTILITY_FIRE.get() ||
                !MagiciteConfig.FIRE_PLACE_BLOCKS.get() || tier<2)))return cells;
        if(kind!=MagiciteKind.FIRE && kind!=MagiciteKind.BLIZZARD)return cells;
        double dx=direction.x,dz=direction.z,mag=Math.hypot(dx,dz);
        if(mag<.05d)return cells;
        dx/=mag;dz/=mag;
        BlockPos center=BlockPos.containing(at);
        if(kind==MagiciteKind.BLIZZARD) {

            center = bridgeExtensionAnchor(world, center, dx, dz);
            at = new Vec3(center.getX()+.5d,center.getY(),center.getZ()+.5d);
            int length=BlizzardBalance.bridgeLength(tier,participants);
            int width=BlizzardBalance.bridgeWidth(tier,participants);
            java.util.Set<BlockPos> unique=new java.util.LinkedHashSet<>();
            for(int step=-3;step<=length;step++)for(int across=0;across<width;across++) {
                double lateral=across-(width-1)/2d;
                int x=(int)Math.round(at.x+dx*step-dz*lateral);
                int z=(int)Math.round(at.z+dz*step+dx*lateral);
                BlockPos base=new BlockPos(x,center.getY(),z);
                if(!unique.add(base))continue;
                FootprintCell valid=iceCandidate(world,base);
                cells.add(valid==null?new FootprintCell(base,false):valid);
            }
        } else {
            int halfLength=participants>=4?5:participants==3?4:3;
            java.util.Set<BlockPos> unique=new java.util.LinkedHashSet<>();
            for(int t=-halfLength;t<=halfLength;t++) {
                int x=(int)Math.round(at.x-dz*t);
                int z=(int)Math.round(at.z+dx*t);
                BlockPos base=new BlockPos(x,center.getY(),z);
                if(!unique.add(base))continue;
                FootprintCell valid=fireCandidate(world,base);
                cells.add(valid==null?new FootprintCell(base,false):valid);
            }
        }
        return cells;
    }

    private static BlockPos bridgeExtensionAnchor(ServerLevel world, BlockPos aimed, double dx, double dz) {
        WorldTemporaryState.TemporaryState saved = WorldTemporaryState.saved(world);
        BlockPos end = null;
        double furthest = -2.0d;
        double bestLateral=Double.POSITIVE_INFINITY;

        for(int ox=-6;ox<=6;ox++)for(int oz=-6;oz<=6;oz++)for(int oy=-1;oy<=1;oy++) {
            BlockPos pos=aimed.offset(ox,oy,oz);
            WorldTemporaryState.TemporaryBlock prior=saved.entries.get(pos);
            if(prior==null || !prior.replacement().is(Blocks.PACKED_ICE) || !world.hasChunkAt(pos) ||
                    !world.getBlockState(pos).is(Blocks.PACKED_ICE))continue;
            double deltaX=ox,deltaZ=oz;
            if(deltaX*deltaX+deltaZ*deltaZ>36.0d)continue;
            double longitudinal=deltaX*dx+deltaZ*dz;
            double lateral=Math.abs(-deltaX*dz+deltaZ*dx);
            if(longitudinal < -2.0d || lateral>3.0d)continue;

            boolean ahead=false;
            for(int step=1;step<=2;step++) {
                BlockPos next=pos.offset((int)Math.round(dx*step),0,(int)Math.round(dz*step));
                WorldTemporaryState.TemporaryBlock nextRecord=saved.entries.get(next);
                if(nextRecord!=null && nextRecord.replacement().is(Blocks.PACKED_ICE) &&
                        world.hasChunkAt(next) && world.getBlockState(next).is(Blocks.PACKED_ICE)){
                    ahead=true;break;
                }
            }
            if(!ahead && (longitudinal>furthest+0.05d ||
                    (Math.abs(longitudinal-furthest)<=0.05d && lateral<bestLateral))) {
                furthest=longitudinal;
                bestLateral=lateral;
                end=pos;
            }
        }
        if(end==null) return aimed;

        return end.offset((int)Math.round(dx*3),0,(int)Math.round(dz*3));
    }

    public static List<FootprintCell> soloFreezeFootprint(ServerLevel world, Vec3 at, int tier) {
        List<FootprintCell> cells = new ArrayList<>();
        if(!MagiciteConfig.UTILITY_BLIZZARD.get())return cells;
        BlockPos center=BlockPos.containing(at);
        int range=BlizzardBalance.freezeRadius(tier);
        for(int x=-range;x<=range;x++)for(int z=-range;z<=range;z++) {
            if(!BlizzardBalance.inSoloPatch(tier,x,z))continue;
            BlockPos pos=center.offset(x,0,z);
            FootprintCell valid=soloIceCandidate(world,pos);
            cells.add(valid==null?new FootprintCell(pos,false):valid);
        }
        return cells;
    }

    public static List<FootprintCell> minorIceFootprint(ServerPlayer player) {
        if(!MagiciteConfig.MINOR_MAGIC.get() || !MagiciteConfig.UTILITY_BLIZZARD.get())return List.of();
        Vec3 eye=player.getEyePosition();
        BlockHitResult hit=player.serverLevel().clip(new ClipContext(eye,
                eye.add(player.getLookAngle().scale(6d)),ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,player));
        if(hit.getType()==HitResult.Type.MISS || !player.serverLevel().getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
            return List.of();
        FootprintCell valid=soloIceCandidate(player.serverLevel(),hit.getBlockPos());
        return List.of(valid==null?new FootprintCell(hit.getBlockPos(),false):valid);
    }

    private static FootprintCell soloIceCandidate(ServerLevel world,BlockPos base) {
        for(int dy=2;dy>=-3;dy--) {
            BlockPos pos=base.offset(0,dy,0);
            if(!world.hasChunkAt(pos) || !world.hasChunkAt(pos.above()))continue;
            BlockState state=world.getBlockState(pos);
            if(state.is(Blocks.WATER) && state.getFluidState().isSource() && world.getBlockState(pos.above()).isAir())
                return new FootprintCell(pos,true);
            WorldTemporaryState.TemporaryBlock prior=WorldTemporaryState.saved(world).entries.get(pos);
            if(prior!=null && prior.replacement().equals(state) && state.is(Blocks.FROSTED_ICE))
                return new FootprintCell(pos,true);
        }
        return null;
    }

    private static FootprintCell fireCandidate(ServerLevel world,BlockPos base) {
        for(int dy=2;dy>=-3;dy--) {
            BlockPos pos=base.offset(0,dy,0);
            if(!world.hasChunkAt(pos))continue;
            BlockState state=world.getBlockState(pos);
            if(state.canBeReplaced() && Blocks.FIRE.defaultBlockState().canSurvive(world,pos))
                return new FootprintCell(pos,true);
        }
        return null;
    }
    private static FootprintCell iceCandidate(ServerLevel world,BlockPos base) {
        for(int dy=2;dy>=-3;dy--) {
            BlockPos pos=base.offset(0,dy,0);
            if(!world.hasChunkAt(pos) || !world.hasChunkAt(pos.above()))continue;
            BlockState state=world.getBlockState(pos);
            if(state.is(Blocks.WATER) && state.getFluidState().isSource() && world.getBlockState(pos.above()).isAir())
                return new FootprintCell(pos,true);
            WorldTemporaryState.TemporaryBlock prior=WorldTemporaryState.saved(world).entries.get(pos);
            if(prior!=null && prior.replacement().equals(state) && (state.is(Blocks.FROSTED_ICE)||state.is(Blocks.PACKED_ICE)))
                return new FootprintCell(pos,true);
            if(MagiciteConfig.BLIZZARD_LAVA_BRIDGES.get() && state.is(Blocks.LAVA) &&
                    state.getFluidState().isSource() && world.getBlockState(pos.above()).isAir())
                return new FootprintCell(pos,true);
        }
        return null;
    }

    private static boolean iceAt(ServerLevel world,BlockPos center,int duration,boolean bridge) {
        for(int dy=2;dy>=-3;dy--) {
            BlockPos pos=center.offset(0,dy,0);
            if(!world.hasChunkAt(pos) || !world.hasChunkAt(pos.above()))continue;
            BlockState water=world.getBlockState(pos);
            if(water.is(Blocks.WATER) && water.getFluidState().isSource() && world.getBlockState(pos.above()).isAir()) {
                WorldTemporaryState.setTemporary(world,pos,water,(bridge?Blocks.PACKED_ICE:Blocks.FROSTED_ICE).defaultBlockState(),duration);
                return true;
            }
            if(water.is(Blocks.FROSTED_ICE) || (bridge && water.is(Blocks.PACKED_ICE))) {
                WorldTemporaryState.TemporaryBlock prior=WorldTemporaryState.saved(world).entries.get(pos);
                if(prior!=null && prior.replacement().equals(water)) {
                    WorldTemporaryState.setTemporary(world,pos,prior.original(),
                            (bridge ? Blocks.PACKED_ICE : Blocks.FROSTED_ICE).defaultBlockState(),duration);
                    return true;
                }
            }
        }
        return false;
    }

    static boolean freezeExact(ServerLevel world,BlockPos pos,int duration,boolean bridge) {
        if(!world.hasChunkAt(pos) || !world.hasChunkAt(pos.above()))return false;
        BlockState state=world.getBlockState(pos);
        if(state.is(Blocks.WATER) && state.getFluidState().isSource() && world.getBlockState(pos.above()).isAir()) {
            WorldTemporaryState.setTemporary(world,pos,state,(bridge?Blocks.PACKED_ICE:Blocks.FROSTED_ICE).defaultBlockState(),duration);
            return true;
        }
        WorldTemporaryState.TemporaryBlock previous=WorldTemporaryState.saved(world).entries.get(pos);
        if(previous!=null && previous.replacement().equals(state) &&
                (state.is(Blocks.FROSTED_ICE) || (bridge && state.is(Blocks.PACKED_ICE)))) {
            WorldTemporaryState.setTemporary(world,pos,previous.original(),
                    (bridge?Blocks.PACKED_ICE:Blocks.FROSTED_ICE).defaultBlockState(),duration);
            return true;
        }
        if(bridge && MagiciteConfig.BLIZZARD_LAVA_BRIDGES.get() && state.is(Blocks.LAVA) &&
                state.getFluidState().isSource() && world.getBlockState(pos.above()).isAir()) {
            WorldTemporaryState.setTemporary(world,pos,state,Blocks.BASALT.defaultBlockState(),Math.min(duration,300));
            return true;
        }
        return false;
    }

    private static void freezeWaterfall(ServerLevel world,BlockPos center,int duration,int tier,int participants) {
        int max=Math.min(52,16+8*(tier-3)+8*(participants-1));
        int count=0;
        for(int y=8;y>=-9 && count<max;y--)for(int x=-1;x<=1 && count<max;x++)for(int z=-1;z<=1 && count<max;z++) {
            if(x*x+z*z>1)continue;
            BlockPos pos=center.offset(x,y,z);
            if(!world.hasChunkAt(pos))continue;
            BlockState water=world.getBlockState(pos);
            if(!water.is(Blocks.WATER) || water.getFluidState().isSource())continue;
            if(!water.getFluidState().is(FluidTags.WATER))continue;
            WorldTemporaryState.setTemporary(world,pos,water,Blocks.PACKED_ICE.defaultBlockState(),duration);
            count++;
        }
    }
    private static void freeze(ServerLevel world,Vec3 at,int tier,int participants,Vec3 direction,double radius) {

        List<FootprintCell> patch=soloFreezeFootprint(world,at,tier);
        int duration=MagiciteConfig.ICE_DURATION.get();
        BlockPos center=BlockPos.containing(at);
        for (FootprintCell cell:patch) {
            if(!cell.valid())continue;
            double distance=Math.hypot(cell.pos().getX()-center.getX(),cell.pos().getZ()-center.getZ());
            int delay=Math.min(9,(int)Math.round(distance*2.2d));
            WorldTemporaryState.queueFreeze(world,cell.pos().immutable(),world.getGameTime()+delay,duration,false);
        }
        if(tier>=3 && MagiciteConfig.BLIZZARD_WATERFALLS.get())
            freezeWaterfall(world,center,MagiciteConfig.COOP_BRIDGE_TICKS.get(),tier,participants);
        if(!BlizzardBalance.canBridge(tier,participants))return;
        int bridgeDuration=BlizzardBalance.bridgeDurationTicks(participants);
        List<FootprintCell> extension=cooperativeFootprint(world,at,MagiciteKind.BLIZZARD,participants,tier,direction);
        for(int i=0;i<extension.size();i++) {
            FootprintCell cell=extension.get(i);
            if(!cell.valid())continue;
            int delay=Math.min(10,(int)Math.round(i*10d/Math.max(1,extension.size()-1)));
            WorldTemporaryState.queueFreeze(world,cell.pos().immutable(),world.getGameTime()+delay,bridgeDuration,true);
        }
    }

    private static void meltTerrain(ServerLevel world,Vec3 at,int tier,int participants) {
        BlockPos center=BlockPos.containing(at);
        int range=Math.min(5,Math.max(1,tier)+Math.min(2,participants-1));
        int changed=0;
        for(BlockPos scan:BlockPos.betweenClosed(center.offset(-range,-2,-range),center.offset(range,2,range))) {
            if(changed>=28+12*Math.max(0,tier-1))break;
            BlockPos pos=scan.immutable();
            if(!world.hasChunkAt(pos))continue;
            int dx=pos.getX()-center.getX(),dz=pos.getZ()-center.getZ();
            if(dx*dx+dz*dz>range*range+1)continue;
            BlockState b=world.getBlockState(pos);
            WorldTemporaryState.TemporaryState storage=WorldTemporaryState.saved(world);
            WorldTemporaryState.TemporaryBlock prior=storage.entries.get(pos);
            if(prior!=null && prior.replacement().equals(b) && (b.is(Blocks.FROSTED_ICE)||b.is(Blocks.PACKED_ICE))) {
                storage.entries.remove(pos);storage.setDirty();
                if(world.setBlock(pos,prior.original(),3))changed++;
            } else if(b.is(Blocks.SNOW)||b.is(Blocks.SNOW_BLOCK)) {
                if(world.setBlock(pos,Blocks.AIR.defaultBlockState(),3))changed++;
            } else if(b.is(Blocks.ICE)||b.is(Blocks.FROSTED_ICE)) {
                if(world.setBlock(pos,(world.dimension()==net.minecraft.world.level.Level.NETHER
                    ? Blocks.AIR : Blocks.WATER).defaultBlockState(),3))changed++;
            }
        }
    }

    private static void cookDroppedFood(ServerLevel world,Vec3 at,double radius,int tier,int participants) {
        int budget=Math.min(16,4+2*tier+2*Math.max(0,participants-1));
        List<ItemEntity> items=world.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(Math.max(1d,radius)));
        for(ItemEntity item:items) {
            if(budget<=0)break;
            ItemStack original=item.getItem();
            Item cooked=COOKED_FOODS.get(original.getItem());
            if(cooked==null || original.isEmpty() || item.distanceToSqr(at)>radius*radius)continue;
            int count=Math.min(budget,original.getCount());
            ItemStack result=new ItemStack(cooked,count);
            ItemEntity output=new ItemEntity(world,item.getX(),item.getY(),item.getZ(),result);
            output.setPickUpDelay(10);
            if(!world.addFreshEntity(output))continue;
            original.shrink(count);
            if(original.isEmpty())item.discard();else item.setItem(original);
            budget-=count;
            SpellParticles.send(world,ParticleTypes.SMOKE,item.getX(),item.getY()+.15,item.getZ(),5,.12,.12,.12,.01);
        }
    }
    private static boolean receiver(ServerLevel world,BlockPos pos,int duration) {
        if(!world.hasChunkAt(pos))return false;
        BlockState state=world.getBlockState(pos);
        if(!(state.is(Blocks.LIGHTNING_ROD)||state.is(THUNDER_RECEIVERS)) ||
                !state.hasProperty(BlockStateProperties.POWERED) || state.getValue(BlockStateProperties.POWERED))return false;
        WorldTemporaryState.setTemporary(world,pos,state,state.setValue(BlockStateProperties.POWERED,true),duration);
        return true;
    }

    public static List<FootprintCell> thunderFootprint(ServerLevel world,Vec3 at,double radius) {
        if(!MagiciteConfig.UTILITY_THUNDER.get())return List.of();
        BlockPos center=BlockPos.containing(at);
        int range=Math.min(7,Math.max(1,(int)Math.ceil(radius)));
        List<FootprintCell> result=new ArrayList<>();
        for(BlockPos probe:BlockPos.betweenClosed(center.offset(-range,-2,-range),center.offset(range,2,range))) {
            if(!world.hasChunkAt(probe))continue;
            BlockState state=world.getBlockState(probe);
            if(state.is(Blocks.LIGHTNING_ROD) || state.is(THUNDER_RECEIVERS)) {
                boolean valid=state.hasProperty(BlockStateProperties.POWERED) && !state.getValue(BlockStateProperties.POWERED);
                result.add(new FootprintCell(probe.immutable(),valid));
                if(result.size()>=120)break;
            }
        }
        return result;
    }
    private static void pulse(ServerLevel world,Vec3 at,int tier,int participants,double radius) {
        BlockPos center=BlockPos.containing(at);
        int range=Math.min(7,Math.max(1,(int)Math.ceil(radius)));
        for(BlockPos probe:BlockPos.betweenClosed(center.offset(-range,-2,-range),center.offset(range,2,range)))
            receiver(world,probe.immutable(),MagiciteConfig.THUNDER_PULSE_TICKS.get());
    }

    private static BlockHitResult minorRay(ServerPlayer player) {
        ServerLevel world=player.serverLevel();
        Vec3 eye=player.getEyePosition();
        Vec3 direction=player.getLookAngle().normalize();
        Vec3 end=eye.add(direction.scale(6d));
        Vec3 from=eye;
        for(int skip=0;skip<20 && from.distanceToSqr(eye)<36d;skip++) {
            BlockHitResult hit=world.clip(new ClipContext(from,end,ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,player));
            if(hit.getType()!=HitResult.Type.BLOCK || !world.hasChunkAt(hit.getBlockPos()))return hit;
            BlockState state=world.getBlockState(hit.getBlockPos());
            boolean foliage=state.getBlock() instanceof BushBlock || state.is(Blocks.VINE);
            if(!foliage)return hit;
            Vec3 next=hit.getLocation().add(direction.scale(.28d));
            if(next.distanceToSqr(from)<.0001d || next.distanceToSqr(eye)>=36d)break;
            from=next;
        }
        return BlockHitResult.miss(end,player.getDirection(),BlockPos.containing(end));
    }

    public static boolean canInteractMinor(ServerPlayer player, MagiciteKind kind) {
        ServerLevel world=player.serverLevel();
        if(!MagiciteConfig.MINOR_MAGIC.get())return false;
        if(kind==MagiciteKind.THUNDER && MagiciteConfig.UTILITY_THUNDER.get() && WorldUtilities.aimedMinecart(player)!=null)
            return true;
        BlockHitResult hit=minorRay(player);
        if(kind==MagiciteKind.BLIZZARD && MagiciteConfig.BLIZZARD_EXTINGUISH_ENTITIES.get()) {
            LivingEntity seen=Targeting.directEntity(player,false);
            if(seen!=null && seen.isOnFire() && seen.distanceToSqr(player)<=36d)return true;
        }
        if(hit.getType()==HitResult.Type.MISS || !world.hasChunkAt(hit.getBlockPos()))return false;
        BlockPos pos=hit.getBlockPos();
        BlockState state=world.getBlockState(pos);
        boolean fixture=state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof CandleBlock ||
                state.getBlock() instanceof CandleCakeBlock;
        if(kind==MagiciteKind.FIRE && MagiciteConfig.UTILITY_FIRE.get()) {
            if(state.is(Blocks.WET_SPONGE))return true;
            if(MagiciteConfig.FIRE_ACCELERATES_FURNACES.get() && WorldTemporaryState.canBoostFurnace(world,pos))return true;
            if(fixture && state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT))return true;

        }
        if(kind==MagiciteKind.BLIZZARD && MagiciteConfig.UTILITY_BLIZZARD.get()) {
            if((fixture && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) ||
                    state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))return true;
            if(world.getFluidState(pos).is(FluidTags.WATER) && soloIceCandidate(world,pos)!=null)return true;
        }
        if(kind==MagiciteKind.THUNDER && MagiciteConfig.UTILITY_THUNDER.get()) {
            if(WorldUtilities.previousCopper(state)!=null)return true;
            if((state.is(Blocks.LIGHTNING_ROD) || state.is(THUNDER_RECEIVERS)) &&
                    state.hasProperty(BlockStateProperties.POWERED) && !state.getValue(BlockStateProperties.POWERED))
                return true;
        }
        if(kind==MagiciteKind.CLEAR && state.is(Blocks.COBWEB))return true;
        return false;
    }

    public static boolean minor(ServerPlayer player,MagiciteKind kind) {
        ServerLevel world=player.serverLevel();
        if(!MagiciteConfig.MINOR_MAGIC.get())return false;
        if(kind==MagiciteKind.THUNDER && MagiciteConfig.UTILITY_THUNDER.get() && WorldUtilities.pushOccupiedMinecart(player))
            return true;
        BlockHitResult hit=minorRay(player);
        if(kind==MagiciteKind.BLIZZARD && MagiciteConfig.BLIZZARD_EXTINGUISH_ENTITIES.get()) {
            LivingEntity seen=Targeting.directEntity(player,false);
            if(seen!=null && seen.isOnFire() && seen.distanceToSqr(player)<=36d) {
                seen.clearFire();
                SpellParticles.send(world,ParticleTypes.CLOUD,seen.getX(),seen.getY()+1d,seen.getZ(),12,.25,.35,.25,.02);
                world.playSound(null,seen.blockPosition(),SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.55f,1.1f);
                return true;
            }
        }
        if(hit.getType()==HitResult.Type.MISS)return false;
        BlockPos pos=hit.getBlockPos();
        if(!world.hasChunkAt(pos))return false;
        return switch(kind) {
            case FIRE -> MagiciteConfig.UTILITY_FIRE.get() &&
                    (WorldUtilities.drySponge(world,pos) ||
                     (MagiciteConfig.FIRE_ACCELERATES_FURNACES.get() && WorldTemporaryState.boostFurnace(world,pos)) ||
                     WorldUtilities.lightFixture(world,pos) || WorldUtilities.fireAt(world,pos.relative(hit.getDirection())));
            case BLIZZARD -> MagiciteConfig.UTILITY_BLIZZARD.get() &&
                    (WorldUtilities.extinguish(world,pos) || (world.getFluidState(pos).is(FluidTags.WATER)
                        && iceAt(world,pos,MagiciteConfig.ICE_DURATION.get(),false)));
            case THUNDER -> MagiciteConfig.UTILITY_THUNDER.get() &&
                    (WorldUtilities.restoreCopper(world,pos) || receiver(world,pos,MagiciteConfig.THUNDER_PULSE_TICKS.get()));
            case CLEAR -> WorldUtilities.clearWeb(world,pos);
            default -> false;
        };
    }

    public static boolean villagerFire(ServerLevel world,BlockPos center) {
        for(BlockPos probe:BlockPos.betweenClosed(center.offset(-4,-2,-4),center.offset(4,2,4)))
            if(WorldUtilities.lightFixture(world,probe.immutable()))return true;
        return false;
    }
    public static boolean villagerBlizzard(ServerLevel world,BlockPos center) {
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)
            if(iceAt(world,center.offset(x,0,z),100,false))return true;
        return false;
    }
}
