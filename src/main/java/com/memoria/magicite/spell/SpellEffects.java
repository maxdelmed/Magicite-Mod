package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.MemoriaMagicite;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import java.util.List;

public final class SpellEffects {
    private SpellEffects() {}
    private record Grounded(ServerLevel world, long until) {}
    private record Sanctuary(ServerLevel world, UUID casterId, Vec3 center, double radius,
            long started, long until, float allyPulse, float enemyPulse, int harmony,
            List<MagiciteKind> colors) {}
    private static final Map<UUID,Grounded> GROUNDED = new HashMap<>();
    private static final List<Sanctuary> SANCTUARIES = new ArrayList<>();
    public static void tickLifeHalos(MinecraftServer server) { SpellVisuals.tickLifeHalos(server); }
    public static long activeSanctuaryCount(ServerLevel world) {
        return SANCTUARIES.stream().filter(s->s.world==world && s.until>world.getGameTime()).count();
    }
    public static long activeGroundingCount(ServerLevel world) {
        return GROUNDED.values().stream().filter(g->g.world==world && g.until>world.getGameTime()).count();
    }

    public static void tickCooperativeGrounding(MinecraftServer server) {
        Iterator<Map.Entry<UUID,Grounded>> active=GROUNDED.entrySet().iterator();
        while(active.hasNext()) {
            Map.Entry<UUID,Grounded> entry=active.next();
            Grounded g=entry.getValue();
            if(server.getLevel(g.world.dimension())!=g.world || g.world.getGameTime()>=g.until) {active.remove();continue;}
            Entity e=g.world.getEntity(entry.getKey());
            if(!(e instanceof LivingEntity living) || living.isDeadOrDying() || resistantToControl(living)) {active.remove();continue;}
            if(!living.onGround()) {
                Vec3 motion=living.getDeltaMovement();
                living.setDeltaMovement(motion.x*.92d, SpellBalance.downwardVelocity(motion.y,.20d), motion.z*.92d);
                living.hasImpulse=true;
                if(living instanceof ServerPlayer)living.hurtMarked=true;
            }
        }
    }
    public static void tickSanctuaries(MinecraftServer server) {
        Iterator<Sanctuary> active = SANCTUARIES.iterator();
        while(active.hasNext()) {
            Sanctuary s = active.next();
            ServerLevel live = server.getLevel(s.world.dimension());
            if(live != s.world) { active.remove(); continue; }
            long now = live.getGameTime();
            if(now >= s.until) { sanctuaryFinale(live, s); active.remove(); continue; }
            ServerPlayer caster = live.getServer().getPlayerList().getPlayer(s.casterId);
            if(now % 20L == 0L) {
                for (LivingEntity e : live.getEntitiesOfClass(LivingEntity.class, new AABB(s.center, s.center).inflate(s.radius))) {
                    if (!insideCircle(s.center, e.position(), s.radius)) continue;
                    boolean ally = e instanceof ServerPlayer || (caster != null && (e == caster || Targeting.friendly(caster, e)));
                    if (ally) {
                        e.heal(s.allyPulse);
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, Math.min(1, Math.max(0, s.harmony / 3))));
                        if (s.harmony >= 3) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0));
                    } else if (e != caster && !(e instanceof EnderDragon)) {
                        e.hurt(live.damageSources().magic(), s.enemyPulse);
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0));
                        Vec3 away = e.position().subtract(s.center);
                        if (away.lengthSqr() > 0.001d) {
                            Vec3 push = away.normalize().scale(0.28d);
                            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.05d, push.z));
                            e.hasImpulse = true;
                        }
                    }
                }
            }
            if(now % 4L == 0L) SanctuaryVisuals.sanctuaryDomeVisuals(live, s.center, s.radius, s.harmony, now, s.colors);

            long age=now-s.started;
            if(age>=0L && age<6L*s.colors.size() && age%6L==0L)
                SanctuaryVisuals.sanctuaryFormationWave(live,s.center,s.radius,s.colors.get((int)(age/6L)),(int)(age/6L));
            if(now % 40L == 0L)
                live.playSound(null, s.center.x, s.center.y, s.center.z, MemoriaMagicite.CHARGE.get(), SoundSource.PLAYERS, 0.18f, 1.18f + 0.04f * Math.min(3, s.harmony));
        }
    }

    private static void sanctuaryFinale(ServerLevel live, Sanctuary s) {
        for (LivingEntity e : live.getEntitiesOfClass(LivingEntity.class, new AABB(s.center, s.center).inflate(s.radius))) {
            if (!insideCircle(s.center, e.position(), s.radius)) continue;
            if (e instanceof ServerPlayer) {
                e.heal(2f + s.harmony);
                clear(e);
            } else if (!(e instanceof EnderDragon)) {
                Vec3 away = e.position().subtract(s.center);
                if (away.lengthSqr() > 0.001d) {
                    Vec3 push = away.normalize().scale(0.72d + 0.12d * s.harmony);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.18d, push.z));
                    e.hasImpulse = true;
                }
            }
        }
        SpellVisuals.areaBurst(live, s.center, s.radius + 0.7d, 0xFFF6D7, null, 54, false);
        SpellParticles.send(live,ParticleTypes.FIREWORK, s.center.x, s.center.y + 0.8d, s.center.z, 28, s.radius * 0.35d, s.radius * 0.18d, s.radius * 0.35d, 0.01d);
        SpellParticles.send(live,ParticleTypes.END_ROD, s.center.x, s.center.y + 1.2d, s.center.z, 40, s.radius * 0.4d, s.radius * 0.22d, s.radius * 0.4d, 0.04d);
        live.playSound(null, s.center.x, s.center.y, s.center.z, MemoriaMagicite.RESONANCE_READY.get(), SoundSource.PLAYERS, 1.1f, 1.15f);
    }

    public static final String LIFE_EXPIRY = "MemoriaLifeUntil";
    public static final String LIFE_LOCK = "MemoriaLifeLockUntil";
    public static final String LIFE_RESTORE = "MemoriaLifeRestoreHealth";
    public static final String HASTE_UNTIL = "MemoriaHasteUntil";

    public static final TagKey<EntityType<?>> CONTROL_RESISTANT = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(MemoriaMagicite.ID, "control_resistant"));

    public static void base(ServerPlayer caster, MagiciteKind kind, Vec3 target) {
        ServerLevel world = caster.serverLevel();
        if (kind.isSupport()) {
            LivingEntity friendly = Targeting.findSupport(caster, target, kind == MagiciteKind.LIFE);
            if (friendly == null) return;
            switch (kind) {
                case PURE_MEMORY -> { heal(friendly, 8f, 1, true); clear(friendly); friendly.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0)); }
                case CURE -> {heal(friendly, 6f, 1, false);SpellVisuals.healingRibbon(caster,friendly);}
                case CLEAR -> clear(friendly);
                case LIFE -> ward(caster, friendly, MagiciteConfig.LIFE_DURATION.get(), 4f);
                default -> {}
            }
            SpellVisuals.sparkle(world, friendly.position().add(0, 1, 0), kind.color, 24);
        } else {
            attack(caster, target, kind, MagiciteConfig.DAMAGE_BASE.get().floatValue(), SpellProfile.radius(kind));
            SpellWorldInteractions.cast(world, target, kind, 1, 1, caster.getLookAngle(),SpellProfile.radius(kind));
        }
        SpellVisuals.soundSpell(world, target, kind, "", 1);
    }

    public static void fused(ServerPlayer caster, String name, List<MagiciteKind> ingredients, Vec3 target, boolean unstable) {
        fused(caster, name, ingredients, target, unstable, 1);
    }
    public static void fused(ServerPlayer caster, String name, List<MagiciteKind> ingredients, Vec3 target,
                             boolean unstable, int distinctPlayers) {
        ServerLevel world = caster.serverLevel();
        int participants = Math.max(1, Math.min(4, distinctPlayers));
        int supportExtras=(name.equals("curaga") || name.equals("clearaga") || name.equals("full_life"))
                ? Math.max(0,ingredients.size()-2) : 0;
        float size = SpellProfile.radius(name, unstable) + supportExtras*0.65f + CooperativeBalance.radiusBonus(
                participants, MagiciteConfig.COOP_RADIUS_PER_PLAYER.get());
        float power = SpellProfile.damageMultiplier(unstable) * CooperativeBalance.damageMultiplier(participants,
                MagiciteConfig.COOP_TWO_POWER.get(), MagiciteConfig.COOP_THREE_POWER.get(), MagiciteConfig.COOP_FOUR_POWER.get());
        float healingBonus = unstable ? 1.35f : 1.0f;

        if (name.startsWith("fira") || name.startsWith("blizzara") || name.startsWith("thundara")
                || name.startsWith("firaga") || name.startsWith("blizzaga") || name.startsWith("thundaga")) {
            MagiciteKind kind = name.startsWith("fir") ? MagiciteKind.FIRE :
                    name.startsWith("blizz") ? MagiciteKind.BLIZZARD : MagiciteKind.THUNDER;
            float damage = SpellBalance.elementalDamage(name,
                    MagiciteConfig.DAMAGE_TWO.get().floatValue(), MagiciteConfig.DAMAGE_THREE.get().floatValue(),
                    MagiciteConfig.DAMAGE_MINOR_PLUS_ONE.get().floatValue(), MagiciteConfig.DAMAGE_MINOR_PLUS_TWO.get().floatValue(),
                    MagiciteConfig.DAMAGE_PLUS_ONE.get().floatValue(), MagiciteConfig.DAMAGE_PLUS_TWO.get().floatValue());
            attack(caster, target, kind, damage * power, size, true);
            int tier = name.startsWith("fira") || name.startsWith("blizzara") || name.startsWith("thundara") ? 2 : 3;
            if(ingredients.size()==4) tier=4;
            SpellWorldInteractions.cast(world, target, kind, participants, tier, caster.getLookAngle(),size);
            if(kind == MagiciteKind.THUNDER && participants > 1 && MagiciteConfig.COOP_THUNDER_CHAIN.get())
                thunderChain(caster, target, size, damage * power, participants);
            if(kind == MagiciteKind.FIRE && participants > 1 && name.startsWith("firaga"))
                firagaShockwave(caster, target, size, participants);
        } else {
            switch (name) {
                case "gravity", "gravira", "graviga" -> gravity(caster, target, size, power, participants, !name.equals("gravity"));
                case "curaga" -> {
                    for (LivingEntity friend : friends(caster, target, size)) {
                        heal(friend, (9f + supportExtras*3f) * healingBonus, participants, true);
                        SpellVisuals.healingRibbon(caster,friend);
                        if(participants>1 && MagiciteConfig.COOP_REGEN_TICKS.get()>0)
                            friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                                    MagiciteConfig.COOP_REGEN_TICKS.get(),0));
                    }
                    SpellVisuals.areaBurst(world, target, size, MagiciteKind.CURE.color, MagiciteKind.CURE, 48, unstable);
                }
                case "clearaga" -> {
                    for (LivingEntity friend : friends(caster, target, size)) clear(friend);
                    SpellVisuals.areaBurst(world, target, size, MagiciteKind.CLEAR.color, MagiciteKind.CLEAR, 44, unstable);
                }
                case "full_life" -> {
                    if (participants > 1) {
                        for (LivingEntity friend : friends(caster, target, Math.max(size, 3.0f))) {
                            if (friend instanceof ServerPlayer) ward(caster, friend, MagiciteConfig.LIFE_DURATION.get()+supportExtras*80, friend.getMaxHealth());
                        }
                    } else {
                        LivingEntity friend = Targeting.findSupport(caster, target, true);
                        if (friend != null) ward(caster, friend, MagiciteConfig.LIFE_DURATION.get()+supportExtras*80, friend.getMaxHealth());
                    }
                    SpellVisuals.areaBurst(world, target, size, MagiciteKind.LIFE.color, MagiciteKind.LIFE, 48, unstable);
                }
                case "sanctuary_of_memories" -> sanctuaryOfMemories(caster, target, ingredients, participants, unstable);
                case "holy", "holyra", "holyra_plus1" -> holy(caster, target, size,
                        (name.equals("holy") ? MagiciteConfig.DAMAGE_HOLY.get().floatValue() :
                         name.equals("holyra") ? MagiciteConfig.DAMAGE_HOLYRA.get().floatValue() :
                         MagiciteConfig.DAMAGE_HOLYRA_PLUS.get().floatValue()) * power);
                case "slow", "slowga" -> controlEnemies(caster, target, size,
                        CooperativeBalance.extendedDuration(140, participants, 25), false);
                case "stop" -> controlEnemies(caster, target, size,
                        CooperativeBalance.extendedDuration(55, participants, 12), true);
                case "haste", "hastega" -> {
                    if (name.equals("haste")) {
                        LivingEntity friend = Targeting.findSupport(caster, target, false);
                        if (friend != null) speedUp(friend, world.getGameTime(), participants);
                    } else for (LivingEntity friend : friends(caster, target, size)) speedUp(friend, world.getGameTime(), participants);
                    SpellVisuals.areaBurst(world, target, size, 0xE8F0C8, null, 50, unstable);
                }
                default -> { for (MagiciteKind kind : ingredients) base(caster, kind, target); return; }
            }
        }
        if (unstable) {
            SpellVisuals.areaBurst(world, target, size, 0xE1A1FF, null, 44, true);
            world.playSound(null, target.x, target.y, target.z, MemoriaMagicite.UNSTABLE_RELEASE.get(),
                    SoundSource.PLAYERS, 1.3f, 0.88f);
        }
        if (participants > 1) {
            SpellVisuals.areaBurst(world, target, size, 0xF4C76C, null, 36 + 8 * participants, false);
            world.playSound(null, target.x, target.y, target.z, MemoriaMagicite.FUSION.get(),
                    SoundSource.PLAYERS, 0.75f + 0.12f * participants, 1.12f);
        }
        SpellVisuals.soundSpell(world, target, ingredients.isEmpty() ? null : ingredients.get(0), name, ingredients.size());
        if(!name.equals("sanctuary_of_memories")) {
            int tint=RuneSigils.color(name);
            com.memoria.magicite.net.MagiciteNetwork.sendVisual(world,caster.getUUID(),3,
                    target.x,target.y,target.z,tint,Math.max(1.5f,Math.min(size,8f)),name);
        }
    }

    private static void speedUp(LivingEntity friend, long now, int participants) {
        int duration=CooperativeBalance.extendedDuration(240,participants,70);
        friend.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        friend.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0));
        friend.getPersistentData().putLong(HASTE_UNTIL,now+duration);
    }
    private static void controlEnemies(ServerPlayer caster, Vec3 at, double radius, int ticks, boolean stop) {
        ServerLevel world=caster.serverLevel();
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(at,at).inflate(radius))) {
            if(e==caster || e instanceof EnderDragon || !insideCircle(at,e.position(),radius)) continue;
            if (resistantToControl(e)) {

                int limited = MagiciteConfig.BOSS_CONTROL_TICKS.get();
                if (limited > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, limited, 0));
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,stop ? 10 : 2));
                if(stop) e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,ticks,4));
            }
        }
        SpellVisuals.areaBurst(world,at,radius,stop ? 0xF2E8FF : 0xB2AAFF,null,55,false);
    }

    private static boolean resistantToControl(LivingEntity entity) {
        return SpellBalance.controlResistant(entity.getMaxHealth(),
                entity instanceof EnderDragon || entity instanceof WitherBoss,
                entity.getType().is(CONTROL_RESISTANT), MagiciteConfig.BOSS_HEALTH_THRESHOLD.get());
    }
    private static void holy(ServerPlayer caster, Vec3 at, double radius, float damage) {
        ServerLevel world=caster.serverLevel();
        int hitTargets = 0;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius))) {
            if(e==caster || e instanceof EnderDragon || !insideCircle(at,e.position(),radius))continue;
            hitTargets++;
            boolean undead = e.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            float amount = HolyBalance.adjustedDamage(damage, undead,
                    MagiciteConfig.HOLY_UNDEAD_MULTIPLIER.get().floatValue());
            float previousHealth = e.getHealth();
            boolean accepted = e.hurt(world.damageSources().indirectMagic(caster,caster),amount);
            if(!accepted && e.isAlive())SpellVisuals.blockedSpellImpact(world,e);
            if (MagiciteConfig.DEBUG_HOLY_HITS.get()) {
                float lost = Math.max(0f, previousHealth - e.getHealth());
                caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[Holy test] " + e.getName().getString() + " requested " + String.format(java.util.Locale.ROOT, "%.1f", amount)
                        + " HP, applied " + String.format(java.util.Locale.ROOT, "%.1f", lost)
                        + " HP, accepted=" + accepted));
            }
        }
        strikeDragons(caster,at,radius,damage);
        if (MagiciteConfig.DEBUG_HOLY_HITS.get() && hitTargets == 0)
            caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[Holy test] No ordinary living targets within the impact circle; dragon parts checked separately."));
        SpellVisuals.areaBurst(world,at,radius,0xFFFAAF,null,55,false);
    }

    private static List<LivingEntity> friends(ServerPlayer caster, Vec3 target, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : caster.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(radius))) {
            if ((e == caster || Targeting.friendly(caster, e)) && insideCircle(target, e.position(), radius)) list.add(e);
        }
        return list;
    }
    private static void heal(LivingEntity e, float base, int participants, boolean advanced) {
        float missing = Math.max(0, e.getMaxHealth() - e.getHealth());
        float amount = CooperativeBalance.healAmount(base, missing,
                MagiciteConfig.HEAL_MISSING_HEALTH_FRACTION.get(),
                advanced ? MagiciteConfig.CURAGA_HEAL_BONUS_CAP.get() : MagiciteConfig.CURE_HEAL_BONUS_CAP.get(),
                participants, MagiciteConfig.COOP_HEAL_PER_PLAYER.get());
        e.heal(amount);
    }
    private static void clear(LivingEntity e) {
        for (MobEffectInstance effect : List.copyOf(e.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(effect.getEffect());
        }

    }
    private static boolean ward(ServerPlayer caster, LivingEntity target, int duration, float restoreHealth) {
        long now = caster.serverLevel().getGameTime();
        if (target.getPersistentData().getLong(LIFE_LOCK) > now) return false;
        target.getPersistentData().putLong(LIFE_EXPIRY, now + duration);
        target.getPersistentData().putFloat(LIFE_RESTORE, restoreHealth);
        if (target instanceof ServerPlayer p) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Life ward: protected for " + (duration/20) + " seconds").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        return true;
    }

    private static float elementalDamage(ServerLevel world, LivingEntity victim, MagiciteKind kind, float base) {
        if(kind==MagiciteKind.THUNDER) {
            return EnvironmentBalance.thunder(base, victim.isInWaterRainOrBubble(),
                    world.isThundering(), MagiciteConfig.THUNDER_WET_BONUS.get(),
                    MagiciteConfig.THUNDER_STORM_BONUS.get());
        }
        if(kind==MagiciteKind.FIRE) {
            return EnvironmentBalance.fire(base, world.dimension()==Level.NETHER,
                    MagiciteConfig.FIRE_NETHER_BONUS.get());
        }
        return base;
    }

    private static void attack(ServerPlayer caster, Vec3 at, MagiciteKind kind, float damage, double radius) {
        attack(caster,at,kind,damage,radius,false);
    }
    private static void attack(ServerPlayer caster, Vec3 at, MagiciteKind kind, float damage, double radius, boolean fusionLightning) {
        ServerLevel world = caster.serverLevel();
        int lightningBudget=8;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            if (e == caster || e instanceof EnderDragon || !insideCircle(at, e.position(), radius)) continue;

            boolean damaged=e.hurt(world.damageSources().indirectMagic(caster, caster),
                    elementalDamage(world,e,kind,damage));
            if(fusionLightning && kind==MagiciteKind.THUNDER && MagiciteConfig.COSMETIC_LIGHTNING.get() && lightningBudget-->0)
                SpellVisuals.visualLightning(world,e.position());
            boolean statusApplied=false;
            switch (kind) {
                case FIRE -> {
                    if(!e.fireImmune()) {
                        e.setSecondsOnFire(3);
                        statusApplied=e.isOnFire();
                    }
                }
                case BLIZZARD -> statusApplied=e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 65, 1));
                case THUNDER -> {
                    statusApplied=e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0));
                    if (!resistantToControl(e))
                        statusApplied=e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 1)) || statusApplied;
                }
                default -> {}
            }
            if(!damaged && !statusApplied && e.isAlive())SpellVisuals.blockedSpellImpact(world,e);
        }
        strikeDragons(caster, at, radius, damage, kind);
        SpellVisuals.areaBurst(world, at, radius, kind.color, kind, Math.max(24, (int)(radius * 11)), false);
    }

    public static boolean minorSupport(ServerPlayer caster,MagiciteKind kind) {
        LivingEntity target=Targeting.directEntity(caster,true);
        if(target==null || target==caster || !Targeting.friendly(caster,target))return false;
        if(kind==MagiciteKind.CURE) {
            long now=caster.serverLevel().getGameTime();
            if(target.getHealth()>=target.getMaxHealth() ||
                    target.getPersistentData().getLong("MemoriaMinorCureUntil")>now)return false;
            target.heal(2f);
            target.getPersistentData().putLong("MemoriaMinorCureUntil",now+60);
        } else if(kind==MagiciteKind.CLEAR) {
            boolean harmed=target.getActiveEffects().stream().anyMatch(e->e.getEffect().getCategory()==MobEffectCategory.HARMFUL);
            if(!harmed)return false;
            clear(target);
        } else return false;
        SpellVisuals.sparkle(caster.serverLevel(),target.position().add(0,1,0),kind.color,12);
        return true;
    }

    private static void strikeDragons(ServerPlayer caster, Vec3 at, double radius, float damage) {
        strikeDragons(caster,at,radius,damage,null);
    }
    private static void strikeDragons(ServerPlayer caster, Vec3 at, double radius, float damage, MagiciteKind kind) {
        ServerLevel world = caster.serverLevel();
        for (EnderDragon dragon : world.getEntitiesOfClass(EnderDragon.class,
                new AABB(at, at).inflate(radius + 18.0))) {
            if (dragon.isDeadOrDying()) continue;
            EnderDragonPart closest = null;
            double best = Double.POSITIVE_INFINITY;
            for (EnderDragonPart part : dragon.getSubEntities()) {
                AABB box = part.getBoundingBox();
                if (!intersectsSpellZone(box, at, radius)) continue;
                double distance = box.getCenter().distanceToSqr(at);
                if (distance < best) { best = distance; closest = part; }
            }
            if (closest != null) {

                closest.hurt(world.damageSources().indirectMagic(caster, caster),
                        kind==null?damage:elementalDamage(world,dragon,kind,damage));
            }
        }
    }

    private static boolean intersectsSpellZone(AABB box, Vec3 at, double radius) {
        double closestX = Math.max(box.minX, Math.min(at.x, box.maxX));
        double closestZ = Math.max(box.minZ, Math.min(at.z, box.maxZ));
        double dx = at.x - closestX, dz = at.z - closestZ;
        double height = Math.max(2.4, radius);
        return dx * dx + dz * dz <= radius * radius
                && box.minY <= at.y + height && box.maxY >= at.y - height;
    }

    private static void thunderChain(ServerPlayer caster, Vec3 at, double radius, float originalDamage, int participants) {
        ServerLevel world = caster.serverLevel();
        boolean conductivity=MagiciteConfig.THUNDER_WET_CHAINS.get();
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class,
                new AABB(at,at).inflate(radius+8d), e -> e != caster && !(e instanceof EnderDragon)
                    && !e.isDeadOrDying() && !insideCircle(at,e.position(),radius)
                    && insideCircle(at,e.position(),radius+(conductivity && e.isInWaterRainOrBubble()?8d:5d)));
        candidates.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(at)));
        boolean wetPresent=conductivity && candidates.stream().anyMatch(LivingEntity::isInWaterRainOrBubble);
        int maxTargets=Math.min(4,Math.max(0,participants-1)+(wetPresent?1:0));
        for(int i=0;i<Math.min(maxTargets,candidates.size());i++) {
            LivingEntity e=candidates.get(i);
            boolean damaged=e.hurt(world.damageSources().indirectMagic(caster,caster),
                    elementalDamage(world,e,MagiciteKind.THUNDER,originalDamage * 0.45f));
            if(MagiciteConfig.COSMETIC_LIGHTNING.get()) SpellVisuals.visualLightning(world,e.position());
            boolean weakened=e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,45,0));
            if(!damaged && !weakened && e.isAlive())SpellVisuals.blockedSpellImpact(world,e);
            for(int n=1;n<=8;n++) {
                Vec3 v=at.lerp(e.position().add(0,1,0),n/9d);
                SpellParticles.send(world,ParticleTypes.ELECTRIC_SPARK,v.x,v.y,v.z,2,.06,.06,.06,.02);
            }
        }
    }

    private static void gravity(ServerPlayer caster, Vec3 at, double radius, float damageScale,
                                int participants, boolean advanced) {
        ServerLevel world = caster.serverLevel();
        double multiplier = (advanced ? MagiciteConfig.GRAVITY_ADVANCED_MULTIPLIER.get() : 1.0d)
                * (1d + .12d * (participants - 1));
        float damage = (advanced ? MagiciteConfig.DAMAGE_GRAVIRA.get().floatValue() :
                MagiciteConfig.DAMAGE_GRAVITY.get().floatValue()) * damageScale;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            if (e == caster || e instanceof EnderDragon || !insideCircle(at, e.position(), radius)) continue;
            if (!(e instanceof ServerPlayer) || MagiciteConfig.GRAVITY_AFFECTS_PLAYERS.get()) {
                if (!resistantToControl(e)) {
                    Vec3 toward = new Vec3(at.x - e.getX(), 0d, at.z - e.getZ());

                    double horizontal = toward.lengthSqr() > 0.001d
                            ? (MagiciteConfig.GRAVITY_HORIZONTAL_PULL.get() * 1.85d) * multiplier / Math.sqrt(toward.lengthSqr())
                            : 0d;
                    double downward = (MagiciteConfig.GRAVITY_DOWNWARD_IMPULSE.get() * 1.55d) * multiplier;
                    Vec3 before = e.getDeltaMovement();
                    e.setDeltaMovement(before.x + toward.x * horizontal,
                            SpellBalance.downwardVelocity(before.y, downward),
                            before.z + toward.z * horizontal);
                    e.hasImpulse = true;
                    if (e instanceof ServerPlayer) e.hurtMarked = true;

                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, advanced ? 35 : 22, 1));
                    if (participants > 1) {
                        int groundTicks = participants >= 4 ? 200 : participants == 3 ? 160 : 120;
                        GROUNDED.put(e.getUUID(), new Grounded(world, world.getGameTime() + groundTicks));
                    }
                } else {
                    int limited=MagiciteConfig.BOSS_CONTROL_TICKS.get();
                    if (limited > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, limited, 0));
                }
            }
            e.hurt(world.damageSources().indirectMagic(caster,caster), damage);
        }
        if (MagiciteConfig.GRAVITY_ATTRACT_ITEMS.get()) {

            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, new AABB(at,at).inflate(radius))) {
                if (!insideCircle(at,item.position(),radius)) continue;
                Vec3 toward = new Vec3(at.x - item.getX(), 0d, at.z - item.getZ());
                Vec3 pull = toward.lengthSqr()>0.001d ? toward.normalize().scale(0.30d * multiplier * (1d + .10d * Math.max(0,participants-1))) : Vec3.ZERO;
                Vec3 before = item.getDeltaMovement();
                item.setDeltaMovement(before.x + pull.x,
                        SpellBalance.downwardVelocity(before.y,0.38d * multiplier), before.z + pull.z);
                item.hasImpulse = true;
            }
        }
        if (MagiciteConfig.GRAVITY_XP_ORBS.get()) {

            for (ExperienceOrb orb : world.getEntitiesOfClass(ExperienceOrb.class, new AABB(at,at).inflate(radius))) {
                if(!insideCircle(at,orb.position(),radius))continue;
                Vec3 toward=at.subtract(orb.position());
                if(toward.lengthSqr()<0.001d)continue;
                Vec3 current=orb.getDeltaMovement();
                Vec3 pull=toward.normalize().scale(0.30d*multiplier);
                orb.setDeltaMovement(current.add(pull));
                orb.hasImpulse=true;
            }
        }
        strikeDragons(caster, at, radius, damage);
        SpellVisuals.areaBurst(world, at, radius, 0x9D6DDE, null, advanced ? 90 : 55, damageScale > 1);

        SpellParticles.send(world,ParticleTypes.POOF, at.x, at.y + .15d, at.z, advanced ? 28 : 14,
                radius * .55d, .20d, radius * .55d, .015d);
    }
    private static void sanctuaryOfMemories(ServerPlayer caster, Vec3 at, List<MagiciteKind> ingredients, int participants, boolean unstable) {
        ServerLevel world = caster.serverLevel();
        int harmony = (int) ingredients.stream().filter(k -> k != MagiciteKind.PURE_MEMORY).distinct().count();
        double radius = 6.0d + harmony * 1.15d + Math.max(0, participants - 1) * 0.45d + (unstable ? 0.6d : 0d);
        int duration = 180 + harmony * 40 + Math.max(0, participants - 1) * 20;
        float allyPulse = 1.25f + 0.55f * harmony;
        float enemyPulse = 1.6f + 0.7f * harmony;
        for (LivingEntity friend : friends(caster, at, radius)) {
            heal(friend, 5f + harmony * 2f, participants, true);
            clear(friend);
            friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
            friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        }
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            if (e == caster || e instanceof EnderDragon || !insideCircle(at, e.position(), radius)) continue;
            if (!Targeting.friendly(caster, e)) {
                e.hurt(world.damageSources().indirectMagic(caster, caster), enemyPulse * 1.2f);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
        }
        SpellVisuals.areaBurst(world, at, radius, 0xF7EBFF, null, 64, unstable);
        SpellParticles.send(world,ParticleTypes.END_ROD, at.x, at.y + 0.4d, at.z, 48, radius * 0.38d, radius * 0.22d, radius * 0.38d, 0.05d);
        SpellParticles.send(world,ParticleTypes.FIREWORK, at.x, at.y + 0.6d, at.z, 24, radius * 0.2d, 0.2d, radius * 0.2d, 0.01d);
        List<MagiciteKind> contributors=ingredients.stream()
                .filter(k->k!=MagiciteKind.PURE_MEMORY).distinct().toList();
        SanctuaryVisuals.sanctuaryDomeVisuals(world, at, radius, harmony, world.getGameTime(),contributors);
        world.playSound(null, at.x, at.y, at.z, MemoriaMagicite.FUSION.get(), SoundSource.PLAYERS, 1.15f, 1.08f);
        world.playSound(null, at.x, at.y, at.z, MemoriaMagicite.RESONANCE_READY.get(), SoundSource.PLAYERS, 0.75f, 1.35f);
        SANCTUARIES.add(new Sanctuary(world,caster.getUUID(),at,radius,
                world.getGameTime(),world.getGameTime()+duration,allyPulse,enemyPulse,harmony,contributors));
    }

    private static void firagaShockwave(ServerPlayer caster, Vec3 at, double radius, int participants) {
        ServerLevel world = caster.serverLevel();
        double force = participants >= 4 ? 1.05d : participants == 3 ? 0.86d : 0.72d;
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1.0d))) {
            if (e == caster || e instanceof EnderDragon || !insideCircle(at, e.position(), radius + 1.0d)) continue;
            Vec3 away = e.position().subtract(at);
            if (away.lengthSqr() < 0.001d) continue;
            Vec3 push = away.normalize().scale(force);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.12d, push.z));
            e.hasImpulse = true;
        }
        SpellVisuals.areaBurst(world, at, radius + 0.8d, 0xFFB060, MagiciteKind.FIRE, 48, false);
    }

    public static void sacrificeBacklash(ServerPlayer caster) {
        ServerLevel world = caster.serverLevel();
        Vec3 at = caster.position().add(0, 1.0, 0);

        SpellVisuals.areaBurst(world, at, 1.6, 0xC8A1FF, null, 42, true);
        SpellParticles.send(world,ParticleTypes.END_ROD, at.x, at.y, at.z, 20, .45, .65, .45, .14);
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1));
    }

    private static boolean insideCircle(Vec3 at, Vec3 entity, double radius) {
        double dx = at.x - entity.x, dz = at.z - entity.z;
        return dx * dx + dz * dz <= radius * radius && Math.abs(at.y - entity.y) <= Math.max(2.4, radius);
    }

}
