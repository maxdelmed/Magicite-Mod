package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

public final class Targeting {
    private Targeting() {}

    private static BlockHitResult wallRay(Player caster, double max) {
        Vec3 eye = caster.getEyePosition();
        return caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().normalize().scale(max)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    }

    public static Vec3 aimPoint(Player caster, double max) {
        DirectHit direct = directHit(caster, false);
        Vec3 pos;
        if (direct != null) {
            pos = direct.point();
        } else {
            Vec3 eye = caster.getEyePosition();
            Vec3 end = eye.add(caster.getLookAngle().normalize().scale(max));
            BlockHitResult wall = wallRay(caster, max);

            BlockHitResult fluid = caster.level().clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
            boolean unobstructedFluid = fluid.getType() != HitResult.Type.MISS
                    && (wall.getType() == HitResult.Type.MISS
                        || eye.distanceToSqr(fluid.getLocation()) <= eye.distanceToSqr(wall.getLocation()) + 0.0001);
            boolean aimedWater = unobstructedFluid
                    && caster.level().getFluidState(fluid.getBlockPos()).is(FluidTags.WATER);
            boolean aimedLava = unobstructedFluid
                    && caster.level().getFluidState(fluid.getBlockPos()).is(net.minecraft.tags.FluidTags.LAVA);
            if (aimedWater || aimedLava) {

                pos = fluid.getLocation();
            } else {
                pos = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
                Vec3 top = pos.add(0, 1.8, 0);
                Vec3 bottom = pos.subtract(0, 10.0, 0);
                BlockHitResult floor = caster.level().clip(new ClipContext(top, bottom,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
                if (floor.getType() != HitResult.Type.MISS) pos = floor.getLocation();
            }
        }
        return new Vec3(pos.x, pos.y + 0.05, pos.z);
    }

    public static boolean friendly(Player caster, LivingEntity candidate) {
        if (candidate == caster || candidate.isDeadOrDying()) return false;
        if (candidate instanceof Player player) {
            return MagiciteConfig.AUTO_ALL_PLAYERS_ALLIES.get()
                    || caster.isAlliedTo(player) || !caster.canHarmPlayer(player);
        }
        return !(candidate instanceof Enemy);
    }

    public static LivingEntity findSupport(ServerPlayer caster, Vec3 target, boolean life) {
        LivingEntity direct = directEntity(caster, true);

        if (direct != null && (!life || direct instanceof ServerPlayer)) return direct;
        ServerPlayer auto = caster.serverLevel().players().stream()
                .filter(p -> friendly(caster, p) && p.distanceToSqr(target) <= 3.5 * 3.5)
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(target))).orElse(null);
        if (auto != null) return auto;
        return life ? null : caster;
    }

    public static Vec3 previewCenter(ServerPlayer caster, Vec3 target, boolean support, boolean life) {
        if (!support) return target;
        LivingEntity friend = findSupport(caster, target, life);
        if (friend == null) return target;
        return friend.position().add(0, 0.06, 0);
    }

    private record DirectHit(LivingEntity entity, Vec3 point) {}

    private static DirectHit directHit(Player caster, boolean support) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().normalize().scale(20));
        BlockHitResult wall = wallRay(caster, 20);
        double maxDist = wall.getType() == HitResult.Type.MISS
                ? eye.distanceToSqr(end) : eye.distanceToSqr(wall.getLocation());
        AABB sweep = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(20)).inflate(0.9);
        DirectHit closest = null;
        for (LivingEntity entity : caster.level().getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (entity == caster || entity.isDeadOrDying() || entity instanceof EnderDragon) continue;
            if (support && !friendly(caster, entity)) continue;
            Optional<Vec3> intersection = entity.getBoundingBox().inflate(0.24).clip(eye, end);
            if (intersection.isEmpty()) continue;
            double distance = eye.distanceToSqr(intersection.get());
            if (distance < maxDist) {
                maxDist = distance;
                closest = new DirectHit(entity, entity.position());
            }
        }

        if (!support) {
            for (EnderDragon dragon : caster.level().getEntitiesOfClass(EnderDragon.class, sweep.inflate(16))) {
                if (dragon.isDeadOrDying()) continue;
                for (EnderDragonPart part : dragon.getSubEntities()) {
                    Optional<Vec3> intersection = part.getBoundingBox().inflate(0.24).clip(eye, end);
                    if (intersection.isEmpty()) continue;
                    double distance = eye.distanceToSqr(intersection.get());
                    if (distance < maxDist) {
                        maxDist = distance;
                        closest = new DirectHit(dragon, part.getBoundingBox().getCenter());
                    }
                }
            }
        }
        return closest;
    }

    public static LivingEntity directEntity(Player caster, boolean support) {
        DirectHit hit = directHit(caster, support);
        return hit == null ? null : hit.entity();
    }
}
