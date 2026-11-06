package tests;
import com.memoria.magicite.spell.QuarterChargeMath;
public final class QuarterChargeMathTest {
    public static void main(String[] args) {
        int max=60;
        if(QuarterChargeMath.total(max)!=240)throw new AssertionError("60 points -> 240 quarters");
        int used=0;
        for(int i=1;i<=239;i++) {
            if(!QuarterChargeMath.enough(used,1,max))throw new AssertionError("short early");
            used++;
            if(QuarterChargeMath.used(QuarterChargeMath.fullDamage(used),QuarterChargeMath.remainder(used))!=used)
                throw new AssertionError("roundtrip "+used);
        }
        if(!QuarterChargeMath.breaks(used,1,max))throw new AssertionError("240th minor Gauntlet cast");
        if(!QuarterChargeMath.breaks(236,4,max))throw new AssertionError("60th raw cast");
        if(!QuarterChargeMath.breaks(238,2,max))throw new AssertionError("120th gauntlet full cast");
        if(!QuarterChargeMath.breaks(56,4,15))throw new AssertionError("15th Life cast");
        if(QuarterChargeMath.enough(239,2,max))throw new AssertionError("cannot cast full spell with 0.25 left");
        System.out.println("QuarterChargeMathTest: raw60 / gauntlet120 / minor120-240 / Life15 PASS");
    }
}
