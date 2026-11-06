package tests;
import com.memoria.magicite.spell.BlizzardBalance;
public final class BlizzardBalanceTest {
    public static void main(String[] args) {
        int[] radii={1,2,2,3};
        int[] footprints={9,13,21,37};
        for(int tier=1;tier<=4;tier++) {
            if(BlizzardBalance.freezeRadius(tier)!=radii[tier-1])throw new AssertionError("radius tier "+tier);
            if(BlizzardBalance.patchBlockCount(tier)!=footprints[tier-1])throw new AssertionError("patch cells tier "+tier);
        }
        for(int tier=1;tier<=4;tier++)
            if(BlizzardBalance.canBridge(tier,1))throw new AssertionError("solo bridges are forbidden");
        if(BlizzardBalance.canBridge(1,4) || !BlizzardBalance.canBridge(2,2) || !BlizzardBalance.canBridge(3,4))
            throw new AssertionError("cooperative bridge gating");
        if(BlizzardBalance.bridgeLength(2,2)!=16 || BlizzardBalance.bridgeLength(3,3)!=24 ||
           BlizzardBalance.bridgeLength(4,4)!=32)
            throw new AssertionError("bridge length progression");
        if(BlizzardBalance.bridgeWidth(2,2)!=1 || BlizzardBalance.bridgeWidth(3,3)!=2 ||
           BlizzardBalance.bridgeWidth(4,4)!=3)
            throw new AssertionError("bridge widths");
        if(BlizzardBalance.bridgeDurationTicks(2)!=800 || BlizzardBalance.bridgeDurationTicks(3)!=1000 ||
           BlizzardBalance.bridgeDurationTicks(4)!=1200)
            throw new AssertionError("cooperative bridge duration");
        System.out.println("BlizzardBalanceTest: solo patches, cooperative crossings PASS");
    }
}
