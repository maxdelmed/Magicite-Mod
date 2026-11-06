package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class WorldUtilities {
    private WorldUtilities() {}

    static boolean drySponge(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos) || !world.getBlockState(pos).is(Blocks.WET_SPONGE))return false;
        if(!world.setBlock(pos,Blocks.SPONGE.defaultBlockState(),3))return false;
        SpellParticles.send(world,ParticleTypes.CLOUD,pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5,
                12,.2,.22,.2,.014);
        world.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.35f,1.25f);
        return true;
    }

    static BlockState previousCopper(BlockState state) {
        if(!(state.getBlock() instanceof WeatheringCopper))return null;
        return WeatheringCopper.getPrevious(state).orElse(null);
    }

    static boolean restoreCopper(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos))return false;
        BlockState prior=previousCopper(world.getBlockState(pos));
        if(prior==null || !world.setBlock(pos,prior,3))return false;
        SpellParticles.send(world,ParticleTypes.ELECTRIC_SPARK,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,
                9,.2,.2,.2,.028);
        world.playSound(null,pos,SoundEvents.AXE_SCRAPE,SoundSource.BLOCKS,.35f,1.65f);
        return true;
    }

    static AbstractMinecart aimedMinecart(ServerPlayer player) {
        ServerLevel world=player.serverLevel();
        Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(6d));
        BlockHitResult wall=world.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,player));
        double wallDistance=wall.getType()==HitResult.Type.MISS?36d:start.distanceToSqr(wall.getLocation());
        AbstractMinecart best=null;double nearest=Double.POSITIVE_INFINITY;
        for(AbstractMinecart cart:world.getEntitiesOfClass(AbstractMinecart.class,new AABB(start,end).inflate(1d))) {
            if(!cart.isAlive() || !cart.isOnRails() || !cart.isVehicle() ||
                    cart.getDeltaMovement().horizontalDistanceSqr()>.0025d)continue;
            var crossing=cart.getBoundingBox().inflate(.18d).clip(start,end);
            if(crossing.isEmpty())continue;
            double distance=start.distanceToSqr(crossing.get());
            if(distance>wallDistance+.01d || distance>=nearest)continue;
            BlockPos railPos=cart.getCurrentRailPosition();
            if(!world.hasChunkAt(railPos))continue;
            BlockState railState=world.getBlockState(railPos);
            if(!(railState.getBlock() instanceof BaseRailBlock rail))continue;
            RailShape shape=rail.getRailDirection(railState,world,railPos,cart);

            if(!straightRail(shape))continue;
            best=cart;nearest=distance;
        }
        return best;
    }

    static boolean straightRail(RailShape shape) {
        return shape==RailShape.NORTH_SOUTH || shape==RailShape.EAST_WEST ||
                shape==RailShape.ASCENDING_NORTH || shape==RailShape.ASCENDING_SOUTH ||
                shape==RailShape.ASCENDING_EAST || shape==RailShape.ASCENDING_WEST;
    }

    static boolean pushOccupiedMinecart(ServerPlayer player) {
        AbstractMinecart cart=aimedMinecart(player);
        if(cart==null)return false;
        ServerLevel world=player.serverLevel();
        BlockPos pos=cart.getCurrentRailPosition();
        BlockState state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof BaseRailBlock rail))return false;
        RailShape shape=rail.getRailDirection(state,world,pos,cart);
        boolean northSouth=shape==RailShape.NORTH_SOUTH ||
                shape==RailShape.ASCENDING_NORTH || shape==RailShape.ASCENDING_SOUTH;
        Vec3 view=player.getLookAngle();
        double look=northSouth?view.z:view.x;
        double offset=northSouth?cart.getZ()-player.getZ():cart.getX()-player.getX();
        double sign=Math.abs(look)>.25d ? Math.signum(look)
                : Math.abs(offset)>.15d ? Math.signum(offset) : 1d;

        cart.setDeltaMovement(northSouth?0d:.23d*sign,0d,northSouth?.23d*sign:0d);
        cart.hasImpulse=true;
        SpellParticles.send(world,ParticleTypes.ELECTRIC_SPARK,cart.getX(),cart.getY()+.5d,cart.getZ(),
                12,.25d,.2d,.25d,.028d);
        world.playSound(null,pos,SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.BLOCKS,.11f,1.9f);
        return true;
    }

    static boolean clearWeb(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos) || !world.getBlockState(pos).is(Blocks.COBWEB))return false;
        if(!world.setBlock(pos,Blocks.AIR.defaultBlockState(),3))return false;
        if(world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS))
            world.addFreshEntity(new ItemEntity(world,pos.getX()+.5,pos.getY()+.45,pos.getZ()+.5,
                    new ItemStack(Items.STRING)));
        SpellParticles.send(world,ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,
                8,.2,.2,.2,.02);
        return true;
    }

    static boolean lightFixture(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos))return false;
        BlockState state=world.getBlockState(pos);
        Block block=state.getBlock();
        boolean fixture=block instanceof CampfireBlock || block instanceof CandleBlock || block instanceof CandleCakeBlock;
        if(!fixture || !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))return false;
        if(state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))return false;
        return world.setBlock(pos,state.setValue(BlockStateProperties.LIT,true),3);
    }

    static boolean extinguish(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos))return false;
        BlockState state=world.getBlockState(pos);
        Block block=state.getBlock();
        boolean fixture=block instanceof CampfireBlock || block instanceof CandleBlock || block instanceof CandleCakeBlock;
        boolean success=false;
        if(fixture && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            success=world.setBlock(pos,state.setValue(BlockStateProperties.LIT,false),3);
        } else if(state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            success=world.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        if(success) {
            world.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,0.5f,1.3f);
            SpellParticles.send(world,ParticleTypes.SMOKE,pos.getX()+0.5,pos.getY()+0.7,pos.getZ()+0.5,
                    7,0.17,0.18,0.17,0.015);
        }
        return success;
    }

    static void extinguishFireCluster(ServerLevel world,Vec3 at,int tier) {
        if(tier<2)return;
        int radius=tier>=3?2:1;
        BlockPos center=BlockPos.containing(at);
        int budget=tier>=3?16:8;
        int removed=0;
        for(BlockPos scan:BlockPos.betweenClosed(center.offset(-radius,-1,-radius),
                center.offset(radius,1,radius))) {
            if(removed>=budget)break;
            BlockPos pos=scan.immutable();
            int dx=pos.getX()-center.getX(),dz=pos.getZ()-center.getZ();
            if(dx*dx+dz*dz>radius*radius || !world.hasChunkAt(pos)
                    || !world.getBlockState(pos).is(Blocks.FIRE))continue;
            if(world.setBlock(pos,Blocks.AIR.defaultBlockState(),3)) {
                removed++;
                SpellParticles.send(world,ParticleTypes.CLOUD,pos.getX()+.5d,
                        pos.getY()+.5d,pos.getZ()+.5d,5,.12d,.16d,.12d,.01d);
            }
        }
        if(removed>0)world.playSound(null,center,SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,.45f,1.2f);
    }

    static boolean fireAt(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos))return false;
        BlockState state=world.getBlockState(pos);
        return state.canBeReplaced() && Blocks.FIRE.defaultBlockState().canSurvive(world,pos)
                && world.setBlock(pos,Blocks.FIRE.defaultBlockState(),3);
    }

    static void ignite(ServerLevel world,Vec3 at,int tier,int participants,double radius) {
        BlockPos center=BlockPos.containing(at);
        int range=Math.min(7,(int)Math.ceil(radius));
        int fires=0;
        for(BlockPos probe:BlockPos.betweenClosed(center.offset(-range,-2,-range),center.offset(range,1,range))) {
            BlockPos pos=probe.immutable();
            if(!world.hasChunkAt(pos))continue;
            if(lightFixture(world,pos))continue;

            if(tier<2 || !MagiciteConfig.FIRE_PLACE_BLOCKS.get() || fires>=48)continue;
            int dx=pos.getX()-center.getX(),dz=pos.getZ()-center.getZ();
            if(dx*dx+dz*dz>range*range || ((dx*7+dz*11)&3)==0)continue;
            if(fireAt(world,pos))fires++;
        }
    }
}
