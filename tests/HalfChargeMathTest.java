package tests;
import com.memoria.magicite.spell.HalfChargeMath;
public final class HalfChargeMathTest {
    public static void main(String[] a){
        if(HalfChargeMath.totalUnits(30)!=60)throw new AssertionError();
        if(HalfChargeMath.totalUnits(15)!=30)throw new AssertionError();
        int damage=0,half=0;
        for(int i=1;i<=59;i++){
            int old=HalfChargeMath.usedUnits(damage,half);
            if(HalfChargeMath.breaks(old,1,30))throw new AssertionError("too early "+i);
            int used=old+1;
            damage=HalfChargeMath.consumedFullDamage(used);
            half=HalfChargeMath.leftoverHalf(used);
            if(damage!=i/2 || half!=i%2)throw new AssertionError("half damage " +i);
        }
        if(!HalfChargeMath.breaks(HalfChargeMath.usedUnits(damage,half),1,30))throw new AssertionError("60th break");
        if(!HalfChargeMath.breaks(58,2,30))throw new AssertionError("raw 30th break");
        System.out.println("HalfChargeMathTest: deterministic 30 raw / 60 gauntlet PASS");
    }
}
