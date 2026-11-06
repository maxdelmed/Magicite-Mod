package com.memoria.magicite;

import com.memoria.magicite.client.GauntletScreen;
import com.memoria.magicite.client.GauntletArmLayer;
import com.memoria.magicite.item.GauntletItem;
import com.memoria.magicite.item.MagiciteItem;
import com.memoria.magicite.menu.GauntletMenu;
import com.memoria.magicite.net.MagiciteNetwork;
import com.memoria.magicite.spell.MagiciteKind;
import com.memoria.magicite.spell.SpellEvents;
import com.memoria.magicite.spell.VillagerMagicEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.EnumMap;
import java.util.Map;

@Mod(MemoriaMagicite.ID)
public final class MemoriaMagicite {
    public static final String ID = "memoria_magicite";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    public static final Map<MagiciteKind, RegistryObject<Item>> CRYSTALS = new EnumMap<>(MagiciteKind.class);

    static {
        for (MagiciteKind kind : MagiciteKind.values()) {
            CRYSTALS.put(kind, ITEMS.register(kind.id + "_magicite", () -> new MagiciteItem(kind)));
        }
    }
    public static final RegistryObject<Item> GAUNTLET = ITEMS.register("fusion_gauntlet", GauntletItem::new);
    public static final RegistryObject<Item> REPAIR_SHARD = ITEMS.register("magicite_shard", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LEGACY_REPAIR_SHARD = ITEMS.register("aether_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<MenuType<GauntletMenu>> GAUNTLET_MENU = MENUS.register("fusion_gauntlet", () -> IForgeMenuType.create(GauntletMenu::new));
    public static final RegistryObject<SoundEvent> CHARGE = sound("charge");
    public static final RegistryObject<SoundEvent> RELEASE = sound("release");
    public static final RegistryObject<SoundEvent> FUSION = sound("fusion");
    public static final RegistryObject<SoundEvent> LIFE_TRIGGER = sound("life_trigger");
    public static final RegistryObject<SoundEvent> SHATTER = sound("shatter");
    public static final RegistryObject<SoundEvent> RESONANCE_READY = sound("resonance_ready");
    public static final RegistryObject<SoundEvent> POWER_TWO = sound("power_two");
    public static final RegistryObject<SoundEvent> POWER_THREE = sound("power_three");
    public static final RegistryObject<SoundEvent> UNSTABLE_RELEASE = sound("unstable_release");
    public static final Map<MagiciteKind, RegistryObject<SoundEvent>> ELEMENT_SOUNDS = new EnumMap<>(MagiciteKind.class);
    static {
        for (MagiciteKind kind : MagiciteKind.values()) {
            ELEMENT_SOUNDS.put(kind, sound("cast_" + kind.id));
        }
    }

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ID, id)));
    }

    public MemoriaMagicite() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        MENUS.register(bus);
        SOUNDS.register(bus);
        bus.addListener(this::onCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MagiciteConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, com.memoria.magicite.client.ClientVisualConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new com.memoria.magicite.spell.AdminCommands());
        MinecraftForge.EVENT_BUS.register(new SpellEvents());
        MinecraftForge.EVENT_BUS.register(new VillagerMagicEvents());
        MagiciteNetwork.init();
    }

    private void onCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (MagiciteKind kind : MagiciteKind.values()) e.accept(CRYSTALS.get(kind).get());
            e.accept(REPAIR_SHARD.get());
            e.accept(GAUNTLET.get());
        }
    }

    @Mod.EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientRegistration {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void addPlayerLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer = e.getSkin(skin);
                if (playerRenderer != null) playerRenderer.addLayer(new GauntletArmLayer(playerRenderer));
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void screens(FMLClientSetupEvent e) {

            e.enqueueWork(() -> MenuScreens.register(GAUNTLET_MENU.get(), GauntletScreen::new));
        }
    }
}
