package com.memoria.magicite.spell;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AdminCommands {
    @SubscribeEvent public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("magicite")
            .requires(src->src.hasPermission(2))
            .then(Commands.literal("fusion")
                .then(Commands.argument("ingredients",StringArgumentType.greedyString())
                    .executes(ctx->{
                        String[] ids=StringArgumentType.getString(ctx,"ingredients").toLowerCase(java.util.Locale.ROOT).trim().split("\\s+");
                        if(ids.length<1 || ids.length>4) {
                            ctx.getSource().sendFailure(Component.literal("Enter 1-4 ingredients, e.g. fire fire or pure_memory cure."));
                            return 0;
                        }
                        List<MagiciteKind> kinds=new ArrayList<>();
                        for(String id:ids) {
                            MagiciteKind kind=MagiciteKind.find(id);
                            if(kind==null){ctx.getSource().sendFailure(Component.literal("Unknown Magicite: "+id));return 0;}
                            kinds.add(kind);
                        }
                        String solo=FusionRules.result(kinds,false,false);
                        String early=FusionRules.result(kinds,true,false);
                        String late=FusionRules.result(kinds,true,true);
                        ctx.getSource().sendSuccess(()->Component.literal("Solo: "+(solo.isEmpty()?"separate spells":FusionNames.display(solo))
                                +" | co-op early: "+(early.isEmpty()?"none":FusionNames.display(early))
                                +" | co-op late: "+(late.isEmpty()?"none":FusionNames.display(late))),false);
                        return 1;
                    })))
            .then(Commands.literal("status").executes(ctx->{
                ServerLevel world=ctx.getSource().getLevel();
                ctx.getSource().sendSuccess(()->Component.literal("Temporary magic: "+SpellWorldInteractions.diagnostic(world)
                    +" | active sanctuaries="+SpellEffects.activeSanctuaryCount(world)
                    +" | grounded targets="+SpellEffects.activeGroundingCount(world)),false);
                return 1;
            }))
            .then(Commands.literal("ice")
                .then(Commands.literal("inspect").executes(ctx->{
                    String diagnostic=SpellWorldInteractions.diagnostic(ctx.getSource().getLevel());
                    ctx.getSource().sendSuccess(()->Component.literal("Magicite temporary blocks: "+diagnostic),false);
                    return 1;
                }))
                .then(Commands.literal("cleanup").executes(ctx->{
                    int restored=SpellWorldInteractions.cleanupExpired(ctx.getSource().getLevel());
                    ctx.getSource().sendSuccess(()->Component.literal("Restored "+restored+" eligible expired blocks in loaded chunks. Modified blocks are untouched."),false);
                    return restored;
                }))));
    }
}
