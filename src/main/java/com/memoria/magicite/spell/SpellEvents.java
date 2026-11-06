package com.memoria.magicite.spell;

import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.item.MagiciteItem;
import com.memoria.magicite.item.MagiciteCharge;
import com.memoria.magicite.menu.GauntletStorage;
import com.memoria.magicite.net.MagiciteNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SpellEvents {
    private static final List<Cast> PENDING = new ArrayList<>();
    private static final java.util.Set<UUID> ACTIVE_CHARGES=new java.util.HashSet<>();
    private static final ResourceKey<DamageType> SACRIFICE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation(MemoriaMagicite.ID, "sacrifice"));
    private record Cast(ServerPlayer caster, Vec3 target, List<MagiciteKind> kinds, long releaseTick,
                        boolean sacrifice, net.minecraft.world.item.Item focusType) {}
    private record Selection(ItemStack focus, ItemStack[] crystals, List<MagiciteKind> kinds,
                             boolean gauntlet, boolean sacrifice, int required) {}
    private record Channeling(ServerPlayer player, Selection selection, Vec3 target, int elapsed) {
        boolean ready() { return elapsed >= selection.required; }
        int count() { return selection.kinds.size(); }
    }
    private record Link(List<Channeling> members, String result, Vec3 center, boolean ready) {}

    private static Selection select(ServerPlayer player, InteractionHand hand, boolean gauntlet) {
        ItemStack focus = player.getItemInHand(hand);
        List<MagiciteKind> kinds = new ArrayList<>();
        ItemStack[] crystals;
        boolean sacrifice = false;
        if (gauntlet) {
            if (focus.getItem() != MemoriaMagicite.GAUNTLET.get()) return null;
            crystals = GauntletStorage.read(focus);
            int tier = GauntletStorage.tier(focus);
            for (int i = 0; i < tier; i++) {
                if (crystals[i].getItem() instanceof MagiciteItem magic &&
                        MagiciteCharge.canAfford(crystals[i],magic.kind()==MagiciteKind.LIFE?4:2)) kinds.add(magic.kind());
            }
        } else {
            if (!(focus.getItem() instanceof MagiciteItem magic) || !MagiciteCharge.canAfford(focus,4)) return null;
            crystals = new ItemStack[]{focus, ItemStack.EMPTY, ItemStack.EMPTY};
            kinds.add(magic.kind());
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (other.getItem() instanceof MagiciteItem otherMagic && MagiciteCharge.canAfford(other,4)) {

                if (!FusionRules.result(List.of(magic.kind(), otherMagic.kind())).isEmpty()) {
                    kinds.add(otherMagic.kind()); crystals[1] = other; sacrifice = true;
                }
            }
        }
        if (kinds.isEmpty() || kinds.size() > 4) return null;
        int required = gauntlet ? SpellProfile.chargeTicks(kinds.size(), false)
                : sacrifice ? SpellProfile.SACRIFICE_CHARGE : SpellProfile.baseChargeTicks();

        if (player.getPersistentData().getLong(SpellEffects.HASTE_UNTIL) > player.serverLevel().getGameTime())
            required = Math.max(10, (required * 3 + 3) / 4);
        return new Selection(focus, crystals, List.copyOf(kinds), gauntlet, sacrifice, required);
    }

    public static int requiredCharge(ServerPlayer player, InteractionHand hand, boolean gauntlet) {
        Selection selected = select(player, hand, gauntlet);
        return selected == null ? SpellProfile.baseChargeTicks() : selected.required;
    }

    private static Selection selectMinor(ServerPlayer player,InteractionHand hand,boolean gauntlet) {
        ItemStack focus=player.getItemInHand(hand);
        if(gauntlet) {
            if(focus.getItem()!=MemoriaMagicite.GAUNTLET.get())return null;
            ItemStack[] stones=GauntletStorage.read(focus);
            for(int i=0;i<GauntletStorage.tier(focus);i++) {
                if(stones[i].getItem() instanceof MagiciteItem magic && magic.kind()!=MagiciteKind.LIFE
                        && MagiciteCharge.canAfford(stones[i],1))
                    return new Selection(focus,stones,List.of(magic.kind()),true,false,18);
            }
            return null;
        }
        if(focus.getItem() instanceof MagiciteItem magic && magic.kind()!=MagiciteKind.LIFE
                && MagiciteCharge.canAfford(focus,2))
            return new Selection(focus,new ItemStack[]{focus},List.of(magic.kind()),false,false,22);
        return null;
    }

    private static void fizzle(ServerPlayer player) {
        net.minecraft.server.level.ServerLevel world=player.serverLevel();
        Vec3 hand=player.getEyePosition().add(player.getLookAngle().scale(.5d));
        MagiciteNetwork.sendVisual(world,player.getUUID(),2,hand.x,hand.y,hand.z,0xBFA7FF,.8f);
        SpellParticles.send(world,net.minecraft.core.particles.ParticleTypes.ENCHANT,
                hand.x,hand.y,hand.z,7,.12d,.14d,.12d,.02d);
        world.playSound(null,player.blockPosition(),MemoriaMagicite.UNSTABLE_RELEASE.get(),SoundSource.PLAYERS,.19f,1.7f);
    }
    private static void castingTrail(Channeling ch) {
        if(ch.elapsed%4>1 || ch.selection.kinds.isEmpty())return;
        ServerPlayer player=ch.player();
        MagiciteKind primary=ch.selection.kinds.get((ch.elapsed/8)%ch.selection.kinds.size());
        net.minecraft.core.particles.ParticleOptions particle=switch(primary) {
            case FIRE -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case BLIZZARD -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
            case THUNDER -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
            default -> net.minecraft.core.particles.ParticleTypes.ENCHANT;
        };
        Vec3 look=player.getLookAngle();
        Vec3 eye=player.getEyePosition();
        double phase=player.serverLevel().getGameTime()*.36d;
        Vec3 hand=eye.add(look.scale(.62d)).add(Math.cos(phase)*.18d, -.38d+Math.sin(phase*.6d)*.12d, Math.sin(phase)*.18d);
        SpellParticles.send(player.serverLevel(),particle,hand.x,hand.y,hand.z,3,.08d,.09d,.08d,.018d);
    }

    public static void castFromRelease(ServerPlayer caster, InteractionHand hand, boolean gauntlet, int elapsed) {
        ACTIVE_CHARGES.remove(caster.getUUID());
        if(MagiciteConfig.MINOR_MAGIC.get() && elapsed>=0 && elapsed<=10) {
            Selection minor=selectMinor(caster,hand,gauntlet);
            if(minor!=null)minorMagic(caster,minor);
            else fizzle(caster);
            return;
        }
        Selection s = select(caster, hand, gauntlet);
        if (s == null) {
            caster.displayClientMessage(Component.literal("Insert Magicite in the Gauntlet first.").withStyle(ChatFormatting.GRAY), true);
            fizzle(caster);
            return;
        }
        if (elapsed < s.required) {
            caster.displayClientMessage(Component.literal("Spell not charged - keep holding!").withStyle(ChatFormatting.YELLOW), true);
            fizzle(caster);
            return;
        }
        if (caster.getCooldowns().isOnCooldown(s.focus.getItem())) return;
        Vec3 target = Targeting.aimPoint(caster, 20);
        String requestedFusion = FusionRules.result(s.kinds);
        boolean life = s.kinds.size() == 1 && s.kinds.get(0) == MagiciteKind.LIFE || requestedFusion.equals("full_life");
        if (life) {
            LivingEntity possible = Targeting.findSupport(caster, target, true);
            if (possible == null || possible.getPersistentData().getLong(SpellEffects.LIFE_LOCK) > caster.serverLevel().getGameTime()) {
                caster.displayClientMessage(Component.literal("Life needs an eligible allied player.").withStyle(ChatFormatting.YELLOW), true);
                fizzle(caster);
                return;
            }
        }

        net.minecraft.world.item.Item castingItemType = s.focus.getItem();
        if (s.gauntlet) {

            int unlocked = GauntletStorage.tier(s.focus);
            for (int i = 0; i < unlocked; i++) {
                ItemStack crystal = s.crystals[i];
                if (MagiciteCharge.usable(crystal)) {
                    int quarters = ((MagiciteItem) crystal.getItem()).kind() == MagiciteKind.LIFE ? 4 : 2;
                    MagiciteCharge.consume(crystal, caster, quarters);
                }
            }
            GauntletStorage.write(s.focus, s.crystals);
        } else {
            MagiciteCharge.consume(s.crystals[0], caster, 4);
            if (s.sacrifice) MagiciteCharge.consume(s.crystals[1], caster, 4);
        }
        if(s.gauntlet)MagiciteNetwork.sendVisual(caster.serverLevel(),caster.getUUID(),1,
                caster.getX(),caster.getY()+1d,caster.getZ(),SpellProfile.color(s.kinds),1f);
        int cooldown = SpellBalance.cooldownForSpell(requestedFusion, s.kinds,
                MagiciteConfig.COOLDOWN_ONE.get(),
                MagiciteConfig.COOLDOWN_TWO.get(), MagiciteConfig.COOLDOWN_THREE.get(),
                MagiciteConfig.COOLDOWN_FOUR.get(), MagiciteConfig.COOLDOWN_CURE_MIN.get(),
                MagiciteConfig.COOLDOWN_LIFE_MIN.get());
        caster.getCooldowns().addCooldown(castingItemType, cooldown);
        Cast cast = new Cast(caster, target, s.kinds, caster.serverLevel().getGameTime(), s.sacrifice, castingItemType);
        PENDING.removeIf(old -> old.caster().getUUID().equals(caster.getUUID()));
        if (s.sacrifice) {
            resolve(List.of(cast));
            if (s.sacrifice) sacrifice(caster);
            return;
        }
        PENDING.add(cast);
    }

    private static void minorMagic(ServerPlayer caster,Selection s) {
        if (caster.getCooldowns().isOnCooldown(s.focus.getItem())) return;
        ItemStack crystal=s.crystals[0];
        if(s.gauntlet) {
            for(int i=0;i<GauntletStorage.tier(s.focus);i++) {
                if(s.crystals[i].getItem() instanceof MagiciteItem choice && choice.kind()!=MagiciteKind.LIFE
                        && MagiciteCharge.canAfford(s.crystals[i],1)) {crystal=s.crystals[i];break;}
            }
        }
        if (!(crystal.getItem() instanceof MagiciteItem magic)) return;
        MagiciteKind kind=magic.kind();
        if (kind == MagiciteKind.LIFE || !MagiciteCharge.canAfford(crystal,s.gauntlet?1:2)) return;
        boolean succeeded;
        if(kind == MagiciteKind.CURE)
            succeeded=SpellEffects.minorSupport(caster,kind);
        else if(kind == MagiciteKind.CLEAR)
            succeeded=SpellWorldInteractions.minor(caster,kind) || SpellEffects.minorSupport(caster,kind);
        else succeeded=SpellWorldInteractions.minor(caster,kind);
        if (!succeeded) {fizzle(caster);return;}
        MagiciteCharge.consume(crystal,caster,s.gauntlet?1:2);
        if(s.gauntlet) GauntletStorage.write(s.focus,s.crystals);
        caster.getCooldowns().addCooldown(s.focus.getItem(),MagiciteConfig.MINOR_COOLDOWN.get());
        caster.serverLevel().playSound(null,caster.blockPosition(),
                MemoriaMagicite.ELEMENT_SOUNDS.get(kind).get(),SoundSource.PLAYERS,.45f,1.5f);
    }

    private static void sacrifice(ServerPlayer caster) {
        SpellEffects.sacrificeBacklash(caster);
        float cost = SacrificeBalance.healthCostHp(caster.getMaxHealth(),
                MagiciteConfig.SACRIFICE_HEALTH_FRACTION.get().floatValue());
        float bonusHearts = caster.getAbsorptionAmount();

        caster.setAbsorptionAmount(0.0f);
        try {
            DamageSource source = new DamageSource(caster.serverLevel().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SACRIFICE_DAMAGE));
            caster.hurt(source, cost);
        } finally {

            if (caster.isAlive()) caster.setAbsorptionAmount(Math.max(bonusHearts, caster.getAbsorptionAmount()));
        }
        caster.displayClientMessage(Component.literal("Sacrifice Fusion: the spell takes its toll.")
                .withStyle(ChatFormatting.RED), true);
    }

    private static Vec3 middle(List<Cast> casts) {
        Vec3 pos = Vec3.ZERO;
        for (Cast c : casts) pos = pos.add(c.target);
        return pos.scale(1.0 / casts.size());
    }

    private static void resolve(List<Cast> casts) {
        if (casts.isEmpty()) return;
        List<MagiciteKind> kinds = new ArrayList<>();
        for (Cast c : casts) kinds.addAll(c.kinds);

        Cast leader = casts.stream().min(Comparator.comparing(c->c.caster.getUUID())).orElse(casts.get(0));
        String fusion = FusionRules.result(kinds, casts.size()>1, isLate(casts));
        if (!fusion.isEmpty()) {
            SpellEffects.fused(leader.caster, fusion, kinds, middle(casts),
                    casts.size() == 1 && leader.sacrifice, casts.size());

            if (casts.size() > 1) {
                int syncedCooldown = SpellBalance.cooldownForSpell(fusion, kinds,
                        MagiciteConfig.COOLDOWN_ONE.get(),
                        MagiciteConfig.COOLDOWN_TWO.get(), MagiciteConfig.COOLDOWN_THREE.get(),
                        MagiciteConfig.COOLDOWN_FOUR.get(), MagiciteConfig.COOLDOWN_CURE_MIN.get(),
                        MagiciteConfig.COOLDOWN_LIFE_MIN.get());
                for (Cast contributor : casts)
                    contributor.caster.getCooldowns().addCooldown(contributor.focusType, syncedCooldown);
            }
            for (Cast c : casts) c.caster.displayClientMessage(
                    Component.literal("FUSION: " + FusionNames.display(fusion)).withStyle(ChatFormatting.GOLD), true);
        } else {
            for (Cast c : casts) for (MagiciteKind kind : c.kinds)
                SpellEffects.base(c.caster, kind, c.target);
        }
    }

    private static boolean overlap(Vec3 a, List<MagiciteKind> ak, Vec3 b, List<MagiciteKind> bk) {
        if (Math.abs(a.y - b.y) > 3.0) return false;
        double horizontal = Math.hypot(a.x - b.x, a.z - b.z);
        return SpellProfile.circlesOverlap(horizontal, SpellProfile.radius(ak), SpellProfile.radius(bk));
    }

    private static boolean overlap(Cast a, Cast b) { return overlap(a.target, a.kinds, b.target, b.kinds); }
    private static boolean overlap(Channeling a, Channeling b) {
        return overlap(a.target, a.selection.kinds, b.target, b.selection.kinds);
    }

    private static boolean canGroup(Cast a, Cast b, int syncTicks) {
        return a.caster != b.caster && a.caster.serverLevel() == b.caster.serverLevel()
                && FusionLatencyBalance.synchronizedRelease(a.releaseTick,b.releaseTick,syncTicks,
                        MagiciteConfig.FUSION_JITTER_GRACE.get(),a.caster.latency,b.caster.latency)
                && overlap(a, b);
    }

    private static List<Cast> bestGroup(Cast first, List<Cast> pending, int window) {
        List<Cast> best = new ArrayList<>();
        best.add(first);
        List<Cast> options = new ArrayList<>();
        for (Cast cast : pending) if (canGroup(first, cast, window)) options.add(cast);
        return searchCasts(best, List.of(first), options, 0, window);
    }
    private static List<Cast> searchCasts(List<Cast> best, List<Cast> group, List<Cast> options,
                                          int start, int window) {
        int total=0; List<MagiciteKind> kinds=new ArrayList<>();
        for (Cast c : group) { total+=c.kinds.size(); kinds.addAll(c.kinds); }
        if (group.size()>1 && !FusionRules.result(kinds,true,isLate(group)).isEmpty() &&
                (total>countKinds(best) || (total==countKinds(best) && group.size()>best.size())))
            best=new ArrayList<>(group);
        if(total>=4 || group.size()>=4) return best;
        for(int i=start;i<options.size();i++) {
            Cast next=options.get(i);
            if(total+next.kinds.size()>4) continue;
            boolean compatible=true;
            for(Cast current:group) if(!canGroup(current,next,window)) {compatible=false;break;}
            if(!compatible)continue;
            List<Cast> expanded=new ArrayList<>(group);expanded.add(next);
            best=searchCasts(best,expanded,options,i+1,window);
        }
        return best;
    }
    private static int countKinds(List<Cast> group) { int n=0; for(Cast c:group)n+=c.kinds.size();return n; }
    private static boolean isLate(List<Cast> group) {
        long earliest=Long.MAX_VALUE, latest=Long.MIN_VALUE;
        for(Cast c:group) { earliest=Math.min(earliest,c.releaseTick);latest=Math.max(latest,c.releaseTick); }
        return latest-earliest>=Math.max(7, MagiciteConfig.FUSION_WINDOW.get()/2+1);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        SpellWorldInteractions.tick(event.getServer());
        SpellEffects.tickCooperativeGrounding(event.getServer());
        SpellEffects.tickLifeHalos(event.getServer());
        SpellEffects.tickSanctuaries(event.getServer());
        if (PENDING.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        int window = MagiciteConfig.FUSION_WINDOW.get();
        while (!PENDING.isEmpty()) {
            Cast first = PENDING.get(0);
            if (now - first.releaseTick < window + MagiciteConfig.FUSION_JITTER_GRACE.get()) break;
            PENDING.remove(0);
            List<Cast> best = bestGroup(first, PENDING, window);
            for (int i=1;i<best.size();i++) PENDING.remove(best.get(i));
            resolve(best);
        }
    }

    private static Channeling channel(ServerPlayer player) {
        if (!player.isUsingItem() || !player.isShiftKeyDown()) return null;
        InteractionHand hand = player.getUsedItemHand();
        boolean gauntlet = player.getUseItem().getItem() == MemoriaMagicite.GAUNTLET.get();
        Selection selected = select(player, hand, gauntlet);
        if (selected == null || player.getCooldowns().isOnCooldown(selected.focus.getItem())) return null;
        int elapsed = Math.max(0, player.getUseItem().getUseDuration() - player.getUseItemRemainingTicks());
        Vec3 center = Targeting.aimPoint(player, 20);
        String spell = FusionRules.result(selected.kinds);
        boolean support = selected.kinds.size() == 1 && selected.kinds.get(0).isSupport()
                || spell.equals("haste") || spell.equals("hastega") || spell.equals("full_life")
                || spell.equals("curaga") || spell.equals("clearaga");
        center = Targeting.previewCenter(player, center, support,
                selected.kinds.size() == 1 && selected.kinds.get(0) == MagiciteKind.LIFE || spell.equals("full_life"));
        return new Channeling(player, selected, center, elapsed);
    }

    private static Vec3 middleChannel(List<Channeling> channels) {
        Vec3 sum = Vec3.ZERO;
        for (Channeling channel : channels) sum = sum.add(channel.target);
        return sum.scale(1.0 / channels.size());
    }

    private static Link linkFor(Channeling current) {
        List<Channeling> options=new ArrayList<>();
        for(ServerPlayer player:current.player.serverLevel().players()) {
            if(player==current.player || player.distanceToSqr(current.player)>64*64)continue;
            Channeling other=channel(player);
            if(other!=null && overlap(current,other))options.add(other);
        }

        if(options.size()>12) options=options.subList(0,12);
        return searchLink(null,List.of(current),options,0);
    }
    private static Link searchLink(Link best,List<Channeling> members,List<Channeling> options,int start) {
        List<MagiciteKind> merged=new ArrayList<>();
        for(Channeling c:members)merged.addAll(c.selection.kinds);
        String spell=FusionRules.result(merged);
        if(members.size()>1 && !spell.isEmpty() && (best==null || merged.size()>linkCount(best))) {
            boolean ready=true;for(Channeling c:members)ready&=c.ready();
            best=new Link(List.copyOf(members),spell,middleChannel(members),ready);
        }
        if(merged.size()>=4 || members.size()>=4)return best;
        for(int i=start;i<options.size();i++) {
            Channeling next=options.get(i);
            if(merged.size()+next.count()>4)continue;
            boolean overlapAll=true;
            for(Channeling c:members)if(!overlap(c,next)){overlapAll=false;break;}
            if(!overlapAll)continue;
            List<Channeling> extended=new ArrayList<>(members);extended.add(next);
            best=searchLink(best,extended,options,i+1);
        }
        return best;
    }
    private static int linkCount(Link link) {
        int n=0;for(Channeling c:link.members)n+=c.count();return n;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        if(player.isShiftKeyDown() && !player.isUsingItem()
                && player.serverLevel().getGameTime()%5L==0L)
            sendPassiveHint(player);

        if(!player.isUsingItem() || !player.isShiftKeyDown()) {
            if(ACTIVE_CHARGES.remove(player.getUUID())) fizzle(player);
            return;
        }
        if (player.serverLevel().getGameTime() % 2 != 0) return;
        Channeling ch = channel(player);
        if (ch == null) return;
        ACTIVE_CHARGES.add(player.getUUID());
        castingTrail(ch);
        Link link = linkFor(ch);
        Vec3 pos = ch.target;
        String spell = FusionRules.result(ch.selection.kinds);
        float radius = SpellProfile.radius(ch.selection.kinds, ch.selection.sacrifice);
        int color = SpellProfile.color(ch.selection.kinds);
        String linked = "";
        boolean gold = false;
        if (link != null) {
            linked = link.result;
            gold = link.ready;

            pos = link.center;
            radius = SpellProfile.radius(link.result)
                    + ((link.result.equals("curaga") || link.result.equals("clearaga") || link.result.equals("full_life"))
                        ? Math.max(0,linkCount(link)-2)*0.65f : 0f)
                    + CooperativeBalance.radiusBonus(
                    link.members.size(), MagiciteConfig.COOP_RADIUS_PER_PLAYER.get());

        } else if (!spell.isEmpty()) {
            linked = spell;
        }
        boolean minorInteraction=false;
        if(link==null && ch.elapsed<=10 && !ch.selection.sacrifice && !ch.selection.kinds.isEmpty()) {
            MagiciteKind first=ch.selection.kinds.get(0);
            minorInteraction=SpellWorldInteractions.canInteractMinor(player,first);
        }
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer == player || viewer.distanceToSqr(player) < 64 * 64 || viewer.position().distanceToSqr(pos) < 64 * 64) {
                MagiciteNetwork.sendRing(viewer, player.getUUID(), pos.x, pos.y, pos.z,
                        color, radius, Math.min(ch.elapsed, ch.selection.required), ch.selection.required,
                        linked, gold, ch.selection.sacrifice, minorInteraction);
            }
        }

        if(link != null && link.ready &&
                link.members.stream().map(c->c.player.getUUID()).min(UUID::compareTo).orElse(player.getUUID()).equals(player.getUUID()) &&
                player.serverLevel().getGameTime()%4L == 0L) {
            MagiciteKind element = linked.startsWith("blizz") ? MagiciteKind.BLIZZARD :
                    linked.startsWith("fira") || linked.startsWith("firaga") ? MagiciteKind.FIRE : null;
            if(element != null) {
                int tier=linked.startsWith("blizzara") || linked.startsWith("fira") && !linked.startsWith("firaga") ? 2 : 3;
                if(linkCount(link) == 4)tier=4;
                List<SpellWorldInteractions.FootprintCell> cells=SpellWorldInteractions.cooperativeFootprint(
                        player.serverLevel(),link.center,element,link.members.size(),tier,player.getLookAngle());
                if(!cells.isEmpty())for(ServerPlayer viewer:player.serverLevel().players()) {
                    if(viewer.position().distanceToSqr(link.center)<64d*64d)
                        MagiciteNetwork.sendFootprint(viewer,player.getUUID(),
                                (byte)(element==MagiciteKind.BLIZZARD?1:2),cells);
                }
            }
        }

        if(player.serverLevel().getGameTime()%4L==0L && (link==null ||
              link.members.stream().map(c->c.player.getUUID()).min(UUID::compareTo).orElse(player.getUUID()).equals(player.getUUID()))) {
            boolean thunder=link!=null ? linked.startsWith("thund") :
                    spell.startsWith("thund") || (ch.selection.kinds.size()==1 && ch.selection.kinds.get(0)==MagiciteKind.THUNDER);
            if(thunder) {
                List<SpellWorldInteractions.FootprintCell> receivers=
                    SpellWorldInteractions.thunderFootprint(player.serverLevel(),pos,radius);
                if(!receivers.isEmpty())for(ServerPlayer viewer:player.serverLevel().players())
                    if(viewer.distanceToSqr(pos)<64d*64d)
                        MagiciteNetwork.sendFootprint(viewer,player.getUUID(),(byte)3,receivers);
            }
        }

        if (link == null && player.serverLevel().getGameTime()%4L == 0L) {
            boolean rawBlizzard=ch.selection.kinds.size()==1 &&
                    ch.selection.kinds.get(0)==MagiciteKind.BLIZZARD;
            boolean fusedBlizzard=spell.startsWith("blizzara") || spell.startsWith("blizzaga");
            if(rawBlizzard || fusedBlizzard) {
                int tier=rawBlizzard ? 1 : spell.startsWith("blizzara") ? 2 : 3;
                if(ch.selection.kinds.size()==4)tier=4;
                List<SpellWorldInteractions.FootprintCell> cells=
                        ch.elapsed<=10 && rawBlizzard
                            ? SpellWorldInteractions.minorIceFootprint(player)
                            : SpellWorldInteractions.soloFreezeFootprint(player.serverLevel(),ch.target,tier);
                if(cells.stream().anyMatch(SpellWorldInteractions.FootprintCell::valid))
                    for(ServerPlayer viewer:player.serverLevel().players()) {
                    if(viewer.position().distanceToSqr(ch.target)<64d*64d)
                        MagiciteNetwork.sendFootprint(viewer,player.getUUID(),(byte)1,cells);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void protectFromLethalDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().is(SACRIFICE_DAMAGE)) return;
        if (victim.level().isClientSide || event.getAmount() < victim.getHealth()) return;
        long now = victim.level().getGameTime();
        long until = victim.getPersistentData().getLong(SpellEffects.LIFE_EXPIRY);
        if (until <= now) return;
        float restore = victim.getPersistentData().getFloat(SpellEffects.LIFE_RESTORE);
        victim.getPersistentData().remove(SpellEffects.LIFE_EXPIRY);
        victim.getPersistentData().remove(SpellEffects.LIFE_RESTORE);
        victim.getPersistentData().putLong(SpellEffects.LIFE_LOCK, now + MagiciteConfig.LIFE_TRIGGER_LOCK.get());
        event.setCanceled(true);
        victim.setHealth(Math.min(victim.getMaxHealth(), restore > 0f ? restore : 4f));

        for (MobEffectInstance fx : List.copyOf(victim.getActiveEffects())) {
            if (fx.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                victim.removeEffect(fx.getEffect());
        }
        victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0));
        victim.invulnerableTime = 40;
        victim.level().broadcastEntityEvent(victim, (byte)35);
        if (victim instanceof ServerPlayer player) {
            player.serverLevel().playSound(null, player.blockPosition(), MemoriaMagicite.LIFE_TRIGGER.get(), SoundSource.PLAYERS, 1f, 1f);
            player.displayClientMessage(Component.literal("LIFE! Fate has been turned aside.").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent
    public void onDecorativeSnowBreak(BlockEvent.BreakEvent event) {
        if(!(event.getPlayer() instanceof ServerPlayer player))return;
        if(SpellWorldInteractions.clearDecorativeSnow(player.serverLevel(),event.getPos()))
            event.setCanceled(true);
    }

    private static void sendPassiveHint(ServerPlayer player) {
        if(!MagiciteConfig.MINOR_MAGIC.get() || !player.isShiftKeyDown() || player.isUsingItem())return;
        ItemStack held=player.getMainHandItem();
        MagiciteKind kind=null;
        if(held.getItem()==MemoriaMagicite.GAUNTLET.get()) {
            ItemStack[] sockets=GauntletStorage.read(held);
            for(int i=0;i<GauntletStorage.tier(held);i++) {
                if(sockets[i].getItem() instanceof MagiciteItem item && item.kind()!=MagiciteKind.LIFE
                        && MagiciteCharge.canAfford(sockets[i],1)) {kind=item.kind();break;}
            }
        } else if(held.getItem() instanceof MagiciteItem stone && stone.kind()!=MagiciteKind.LIFE &&
                MagiciteCharge.canAfford(held,2))kind=stone.kind();
        else {
            held=player.getOffhandItem();
            if(held.getItem() instanceof MagiciteItem stone && stone.kind()!=MagiciteKind.LIFE &&
                    MagiciteCharge.canAfford(held,2))kind=stone.kind();
        }
        if(kind==null || player.getCooldowns().isOnCooldown(held.getItem()))return;

        if(!SpellWorldInteractions.canInteractMinor(player,kind))return;
        MagiciteNetwork.sendInteractionHint(player);
    }

    @SubscribeEvent
    public void repair(AnvilUpdateEvent event) {
        ItemStack original = event.getLeft();
        if (!(original.getItem() instanceof MagiciteItem)) return;
        if (!(event.getRight().is(MemoriaMagicite.REPAIR_SHARD.get()) || event.getRight().is(MemoriaMagicite.LEGACY_REPAIR_SHARD.get())) || MagiciteCharge.used(original)<=0) return;
        ItemStack output = original.copy();
        MagiciteCharge.repair(output,MagiciteConfig.REPAIR_AMOUNT.get());
        output.setRepairCost(0);
        event.setOutput(output);
        event.setCost(3);
        event.setMaterialCost(1);
    }
}
