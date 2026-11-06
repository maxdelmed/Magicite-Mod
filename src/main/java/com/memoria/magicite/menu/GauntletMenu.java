package com.memoria.magicite.menu;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.item.MagiciteItem;
import com.memoria.magicite.spell.FusionRules;
import com.memoria.magicite.spell.FusionDescriptions;
import com.memoria.magicite.spell.MagiciteKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GauntletMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final Container crystals;
    private int clientTier = 1;
    private boolean loading = true;

    public GauntletMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public GauntletMenu(int containerId, Inventory inv, InteractionHand hand) {
        super(MemoriaMagicite.GAUNTLET_MENU.get(), containerId);
        this.owner = inv.player;
        this.hand = hand;
        this.crystals = new SimpleContainer(4) {
            @Override public void setChanged() {
                super.setChanged();
                if (!loading && !owner.level().isClientSide) {
                    GauntletStorage.write(owner.getItemInHand(GauntletMenu.this.hand), snapshot());
                }
            }
        };
        ItemStack[] initial = GauntletStorage.read(owner.getItemInHand(hand));
        for (int i = 0; i < 4; i++) crystals.setItem(i, initial[i]);
        loading = false;

        for (int i = 0; i < 4; i++) {
            final int slot = i;
            addSlot(new Slot(crystals, i, 43 + i * 42, 48) {
                @Override public boolean mayPlace(ItemStack item) {
                    return slot < currentTier() && item.getItem() instanceof MagiciteItem;
                }
                @Override public boolean mayPickup(Player player) { return slot < currentTier(); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col + row * 9 + 9, 30 + col * 18, 198 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 30 + col * 18, 258));
        addDataSlot(new DataSlot() {
            @Override public int get() { return owner.level().isClientSide ? clientTier : GauntletStorage.tier(owner.getItemInHand(GauntletMenu.this.hand)); }
            @Override public void set(int value) { clientTier = value; }
        });
    }

    private ItemStack[] snapshot() {
        return new ItemStack[]{crystals.getItem(0), crystals.getItem(1), crystals.getItem(2), crystals.getItem(3)};
    }
    public int currentTier() {
        return owner.level().isClientSide ? clientTier : GauntletStorage.tier(owner.getItemInHand(hand));
    }

    private List<MagiciteKind> installedKinds() {
        List<MagiciteKind> kinds = new ArrayList<>();
        for (int i = 0; i < currentTier(); i++) {
            ItemStack stack = crystals.getItem(i);
            if (stack.getItem() instanceof MagiciteItem magic && !stack.isEmpty()) kinds.add(magic.kind());
        }
        return kinds;
    }

    public int installedCount() { return installedKinds().size(); }
    public boolean hasFusion() { return !FusionRules.result(installedKinds()).isEmpty(); }
    public boolean pureMemoryAlone() {
        List<MagiciteKind> kinds = installedKinds();
        return kinds.size() == 1 && kinds.get(0) == MagiciteKind.PURE_MEMORY;
    }
    public String currentFusionPreview() {
        List<MagiciteKind> kinds = installedKinds();
        if (kinds.isEmpty()) return "NO MAGICITE LOADED";
        if (kinds.size() == 1) return com.memoria.magicite.spell.FusionNames.display(kinds.get(0).id) + "  (BASE SPELL)";
        String name = FusionRules.result(kinds);
        return name.isEmpty() ? "NO FUSION - SEPARATE SPELLS" : com.memoria.magicite.spell.FusionNames.display(name);
    }

    public String currentFusionDescription() {
        List<MagiciteKind> kinds = installedKinds();
        if (kinds.isEmpty()) return "Insert a Magicite to cast";
        if (kinds.size() == 1) return FusionDescriptions.forSingle(kinds.get(0));
        return FusionDescriptions.forFusion(FusionRules.result(kinds));
    }

    public void requestSwap(ServerPlayer player,int from,int to) {
        if(player!=owner || !stillValid(player) || from<0 || to<0 || from>=currentTier() || to>=currentTier() || from==to)return;
        ItemStack a=crystals.getItem(from),b=crystals.getItem(to);
        if(!a.isEmpty() && !(a.getItem() instanceof MagiciteItem))return;
        if(!b.isEmpty() && !(b.getItem() instanceof MagiciteItem))return;
        loading=true;
        crystals.setItem(from,b);crystals.setItem(to,a);
        loading=false;
        GauntletStorage.write(owner.getItemInHand(hand),snapshot());
        broadcastChanges();
    }
    public void requestUpgrade(ServerPlayer player) {
        if (player != owner || !stillValid(player)) return;
        if (GauntletStorage.upgrade(player, owner.getItemInHand(hand))) broadcastChanges();
    }
    @Override public boolean stillValid(Player p) {
        return owner == p && owner.getItemInHand(hand).getItem() == MemoriaMagicite.GAUNTLET.get();
    }
    @Override public ItemStack quickMoveStack(Player player, int clickedIndex) {
        if (clickedIndex < 0 || clickedIndex >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(clickedIndex);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack original = source.getItem();
        ItemStack result = original.copy();
        if (clickedIndex < 4) {
            if (!moveItemStackTo(original, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!(original.getItem() instanceof MagiciteItem) || !moveItemStackTo(original, 0, currentTier(), false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        return result;
    }
    @Override public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide && stillValid(p)) GauntletStorage.write(p.getItemInHand(hand), snapshot());
    }
}
