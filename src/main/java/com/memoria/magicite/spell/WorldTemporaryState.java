package com.memoria.magicite.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class WorldTemporaryState {
    private WorldTemporaryState() {}
    record TemporaryBlock(BlockState original,BlockState replacement,long expire) {}
    record PendingFreeze(BlockPos pos, long due, int duration, boolean bridge) {}
    static final class TemporaryState extends SavedData {
        final Map<BlockPos,TemporaryBlock> entries=new HashMap<>();
        final Map<BlockPos,PendingFreeze> pending=new HashMap<>();

        final Map<BlockPos,Long> furnaceBoosts=new HashMap<>();
        private static TemporaryState load(CompoundTag tag) {
            TemporaryState state=new TemporaryState();
            ListTag list=tag.getList("Entries",10);
            for(int i=0;i<list.size();i++) {
                CompoundTag row=list.getCompound(i);
                BlockState original=readState(row.getCompound("Original"));
                BlockState replacement=readState(row.getCompound("Replacement"));
                if(original!=null && replacement!=null)
                    state.entries.put(BlockPos.of(row.getLong("Pos")),new TemporaryBlock(original,replacement,row.getLong("Expire")));
            }
            ListTag waves=tag.getList("PendingWaves",10);
            for(int i=0;i<waves.size();i++) {
                CompoundTag row=waves.getCompound(i);
                BlockPos pos=BlockPos.of(row.getLong("Pos"));
                state.pending.put(pos,new PendingFreeze(pos,row.getLong("Due"),row.getInt("Duration"),row.getBoolean("Bridge")));
            }
            ListTag furnaces=tag.getList("FurnaceBoosts",10);
            for(int i=0;i<furnaces.size();i++) {
                CompoundTag row=furnaces.getCompound(i);
                state.furnaceBoosts.put(BlockPos.of(row.getLong("Pos")),row.getLong("Until"));
            }
            return state;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            ListTag list=new ListTag();
            for(Map.Entry<BlockPos,TemporaryBlock> e:entries.entrySet()) {
                CompoundTag row=new CompoundTag();
                row.putLong("Pos",e.getKey().asLong());
                row.putLong("Expire",e.getValue().expire);
                row.put("Original",writeState(e.getValue().original));
                row.put("Replacement",writeState(e.getValue().replacement));
                list.add(row);
            }
            tag.put("Entries",list);
            ListTag waves=new ListTag();
            for(PendingFreeze wave:pending.values()) {
                CompoundTag row=new CompoundTag();
                row.putLong("Pos",wave.pos.asLong());row.putLong("Due",wave.due);
                row.putInt("Duration",wave.duration);row.putBoolean("Bridge",wave.bridge);
                waves.add(row);
            }
            tag.put("PendingWaves",waves);
            ListTag furnaces=new ListTag();
            for(Map.Entry<BlockPos,Long> entry:furnaceBoosts.entrySet()) {
                CompoundTag row=new CompoundTag();
                row.putLong("Pos",entry.getKey().asLong());
                row.putLong("Until",entry.getValue());
                furnaces.add(row);
            }
            tag.put("FurnaceBoosts",furnaces);
            return tag;
        }
    }
    static TemporaryState saved(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TemporaryState::load,TemporaryState::new,"memoria_magicite_temporary_blocks");
    }
    private static CompoundTag writeState(BlockState state) {
        CompoundTag tag=new CompoundTag();
        ResourceLocation id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        tag.putString("Block",id==null?"minecraft:air":id.toString());
        CompoundTag properties=new CompoundTag();
        for(Property<?> property:state.getProperties())
            properties.putString(property.getName(),propertyValue(state,property));
        tag.put("Properties",properties);
        return tag;
    }
    private static <T extends Comparable<T>> String propertyValue(BlockState state,Property<T> property) {
        return property.getName(state.getValue(property));
    }
    private static BlockState readState(CompoundTag tag) {
        if(!tag.contains("Block"))return null;
        ResourceLocation name=ResourceLocation.tryParse(tag.getString("Block"));
        if(name==null)return null;
        Block block=ForgeRegistries.BLOCKS.getValue(name);
        if(block==null)return null;
        BlockState state=block.defaultBlockState();
        CompoundTag values=tag.getCompound("Properties");
        for(String key:values.getAllKeys()) {
            Property<?> property=state.getBlock().getStateDefinition().getProperty(key);
            if(property!=null) state=applyProperty(state,property,values.getString(key));
        }
        return state;
    }
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state,Property<T> property,String value) {
        return property.getValue(value).map(v->state.setValue(property,v)).orElse(state);
    }
    static void setTemporary(ServerLevel world,BlockPos pos,BlockState original,BlockState replacement,int ticks) {
        if(!world.hasChunkAt(pos))return;
        TemporaryState storage=saved(world);
        TemporaryBlock prior=storage.entries.get(pos);
        BlockState restore=prior==null?original:prior.original;
        storage.entries.put(pos.immutable(),new TemporaryBlock(restore,replacement,world.getGameTime()+ticks));
        storage.setDirty();
        world.setBlock(pos,replacement,3);
        if(replacement.hasProperty(BlockStateProperties.POWERED))world.updateNeighborsAt(pos,replacement.getBlock());
    }
    static void queueFreeze(ServerLevel world,BlockPos pos,long due,int duration,boolean bridge) {
        TemporaryState store=saved(world);
        PendingFreeze prior=store.pending.get(pos);

        if(prior!=null && prior.bridge && !bridge)return;
        store.pending.put(pos.immutable(),new PendingFreeze(pos.immutable(),due,duration,bridge));
        store.setDirty();
    }

    public static int cleanupExpired(ServerLevel world) {
        TemporaryState storage=saved(world);
        int restored=0;
        Iterator<Map.Entry<BlockPos,TemporaryBlock>> it=storage.entries.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<BlockPos,TemporaryBlock> entry=it.next();
            TemporaryBlock state=entry.getValue();
            if(world.getGameTime()<state.expire || !world.hasChunkAt(entry.getKey()))continue;
            if(world.getBlockState(entry.getKey()).equals(state.replacement)) {
                world.setBlock(entry.getKey(),state.original,3);
                if(state.original.hasProperty(BlockStateProperties.POWERED))
                    world.updateNeighborsAt(entry.getKey(),state.original.getBlock());
                restored++;
            }
            it.remove();storage.setDirty();
        }
        return restored;
    }

    public static boolean clearDecorativeSnow(ServerLevel world, BlockPos pos) {
        TemporaryState storage=saved(world);
        TemporaryBlock entry=storage.entries.get(pos);
        if(entry==null || !entry.replacement.is(Blocks.SNOW)
                || !world.hasChunkAt(pos) || !world.getBlockState(pos).equals(entry.replacement))return false;
        storage.entries.remove(pos);
        storage.setDirty();
        world.setBlock(pos,entry.original,3);
        return true;
    }

    public static String diagnostic(ServerLevel world) {
        TemporaryState storage=saved(world);
        long now=world.getGameTime();
        long expired=storage.entries.entrySet().stream().filter(e->e.getValue().expire<=now).count();
        return "tracked=" + storage.entries.size() + ", expired=" + expired
                + ", queued=" + storage.pending.size() + ", boostedFurnaces=" + storage.furnaceBoosts.size();
    }
    public static void tick(MinecraftServer server) {
        for(ServerLevel world:server.getAllLevels()) {
            TemporaryState storage=saved(world);
            tickFurnaceBoosts(world,storage);
            Iterator<PendingFreeze> waves=storage.pending.values().iterator();
            while(waves.hasNext()) {
                PendingFreeze pending=waves.next();
                if(world.getGameTime()<pending.due || !world.hasChunkAt(pending.pos))continue;
                waves.remove();storage.setDirty();

                if(world.getGameTime()-pending.due>40L)continue;
                if(SpellWorldInteractions.freezeExact(world,pending.pos,pending.duration,pending.bridge))
                    SpellParticles.send(world,ParticleTypes.SNOWFLAKE,
                        pending.pos.getX()+.5d,pending.pos.getY()+1.08d,pending.pos.getZ()+.5d,
                        4,.16d,.07d,.16d,.025d);
            }
            if(world.getGameTime()%10L==0L) {
                BlockPos soundAt=null;
                for(ServerPlayer viewer:world.players()) {
                    List<BlockPos> warnings=new ArrayList<>();
                    for(Map.Entry<BlockPos,TemporaryBlock> e:storage.entries.entrySet()) {
                        BlockPos pos=e.getKey();TemporaryBlock state=e.getValue();
                        long left=state.expire-world.getGameTime();
                        if(left<=0 || left>160 || !state.replacement.is(Blocks.PACKED_ICE)
                                || !world.hasChunkAt(pos) || !world.getBlockState(pos).equals(state.replacement)
                                || pos.distSqr(viewer.blockPosition())>40d*40d)continue;
                        warnings.add(pos);
                        if(warnings.size()>=72)break;
                    }
                    if(!warnings.isEmpty()) {
                        com.memoria.magicite.net.MagiciteNetwork.sendIceWarnings(viewer,warnings);
                        if(soundAt==null)soundAt=warnings.get(0);
                    }
                }

                if(soundAt!=null && world.getGameTime()%40L==0L)
                    world.playSound(null,soundAt,SoundEvents.GLASS_HIT,SoundSource.BLOCKS,.20f,.65f);
            }

            if(world.getGameTime()%20L==0L) {
                int snowBudget=12;
                for(Map.Entry<BlockPos,TemporaryBlock> e:storage.entries.entrySet()) {
                    if(snowBudget<=0)break;
                    long remaining=e.getValue().expire-world.getGameTime();
                    BlockPos pos=e.getKey();
                    if(remaining<=0L || remaining>100L || !e.getValue().replacement.is(Blocks.SNOW)
                            || !world.hasChunkAt(pos) || !world.getBlockState(pos).equals(e.getValue().replacement))continue;
                    if(remaining<25L || Math.floorMod(pos.asLong()+world.getGameTime()/20L,3L)!=0L)continue;
                    SpellParticles.send(world,ParticleTypes.SNOWFLAKE,pos.getX()+.5d,
                            pos.getY()+.15d,pos.getZ()+.5d,1,.12d,.04d,.12d,.001d);
                    snowBudget--;
                }
            }
            cleanupExpired(world);
        }
    }

    private static final int MAX_BOOSTED_FURNACES=64;
    private static final int FIRE_BOOST_DURATION_TICKS=120;

    private static <T extends AbstractCookingRecipe> boolean canCook(
            ServerLevel world, AbstractFurnaceBlockEntity furnace, RecipeType<T> type) {
        return world.getRecipeManager().getRecipeFor(type,furnace,world).map(recipe -> {
            ItemStack result=recipe.assemble(furnace,world.registryAccess());
            if(result.isEmpty())return false;
            ItemStack output=furnace.getItem(2);
            return output.isEmpty() ||
                    (ItemStack.isSameItemSameTags(output,result) &&
                     output.getCount()+result.getCount()<=Math.min(output.getMaxStackSize(),furnace.getMaxStackSize()));
        }).orElse(false);
    }

    static boolean canBoostFurnace(ServerLevel world,BlockPos pos) {
        if(!world.hasChunkAt(pos))return false;
        BlockState state=world.getBlockState(pos);
        if(!state.hasProperty(BlockStateProperties.LIT) || !state.getValue(BlockStateProperties.LIT))return false;
        var tile=world.getBlockEntity(pos);
        if(!(tile instanceof AbstractFurnaceBlockEntity furnace) || tile.isRemoved())return false;

        if(tile.getClass()==FurnaceBlockEntity.class) return canCook(world,furnace,RecipeType.SMELTING);
        if(tile.getClass()==BlastFurnaceBlockEntity.class) return canCook(world,furnace,RecipeType.BLASTING);
        if(tile.getClass()==SmokerBlockEntity.class) return canCook(world,furnace,RecipeType.SMOKING);
        return false;
    }

    static boolean boostFurnace(ServerLevel world,BlockPos pos) {
        if(!canBoostFurnace(world,pos))return false;
        TemporaryState storage=saved(world);
        if(!storage.furnaceBoosts.containsKey(pos) && storage.furnaceBoosts.size()>=MAX_BOOSTED_FURNACES)
            return false;
        storage.furnaceBoosts.put(pos.immutable(),world.getGameTime()+FIRE_BOOST_DURATION_TICKS);
        storage.setDirty();
        SpellParticles.send(world,ParticleTypes.FLAME,pos.getX()+.5d,pos.getY()+.9d,pos.getZ()+.5d,
                7,.22d,.12d,.22d,.012d);
        world.playSound(null,pos,SoundEvents.FIRECHARGE_USE,SoundSource.BLOCKS,.32f,1.3f);
        return true;
    }

    static void boostNearbyFurnaces(ServerLevel world,Vec3 center,double radius,int max) {
        BlockPos at=BlockPos.containing(center);
        int r=(int)Math.ceil(radius);
        int boosted=0;

        for(BlockPos scan:BlockPos.betweenClosed(at.offset(-r,-1,-r),at.offset(r,1,r))) {
            if(boosted>=max)break;
            BlockPos pos=scan.immutable();
            if(pos.distToCenterSqr(center.x,center.y,center.z)>radius*radius)continue;
            if(boostFurnace(world,pos))boosted++;
        }
    }

    private static void tickFurnaceBoosts(ServerLevel world,TemporaryState storage) {
        Iterator<Map.Entry<BlockPos,Long>> entries=storage.furnaceBoosts.entrySet().iterator();
        while(entries.hasNext()) {
            Map.Entry<BlockPos,Long> e=entries.next();
            BlockPos pos=e.getKey();
            if(world.getGameTime()>=e.getValue()) {
                entries.remove();storage.setDirty();
                continue;
            }
            if(!world.hasChunkAt(pos))continue;
            if(!canBoostFurnace(world,pos)) {
                entries.remove();storage.setDirty();
                continue;
            }
            AbstractFurnaceBlockEntity furnace=(AbstractFurnaceBlockEntity)world.getBlockEntity(pos);
            if(furnace!=null)
                AbstractFurnaceBlockEntity.serverTick(world,pos,world.getBlockState(pos),furnace);
        }
    }

}
