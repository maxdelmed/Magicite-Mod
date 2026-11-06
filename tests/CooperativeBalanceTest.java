package tests;
import com.memoria.magicite.spell.CooperativeBalance;
public final class CooperativeBalanceTest {
    private static int n;
    private static void ok(boolean yes) {n++;if(!yes)throw new AssertionError("Cooperative test #"+n);}
    private static boolean close(double a,double b) {return Math.abs(a-b)<0.001;}
    public static void main(String[] args) {
        ok(close(CooperativeBalance.damageMultiplier(1,1.2,1.35,1.5),1));
        ok(close(CooperativeBalance.damageMultiplier(2,1.2,1.35,1.5),1.2));
        ok(close(CooperativeBalance.damageMultiplier(3,1.2,1.35,1.5),1.35));
        ok(close(CooperativeBalance.damageMultiplier(4,1.2,1.35,1.5),1.5));
        ok(close(CooperativeBalance.damageMultiplier(9,1.2,1.35,1.5),1.5));
        ok(close(CooperativeBalance.radiusBonus(1,.35),0));
        ok(close(CooperativeBalance.radiusBonus(2,.35),.35));
        ok(close(CooperativeBalance.radiusBonus(4,.35),1.05));
        ok(close(CooperativeBalance.healAmount(6,0,.15,5,1,.12),6));
        ok(close(CooperativeBalance.healAmount(6,40,.15,5,1,.12),11));
        ok(close(CooperativeBalance.healAmount(9,60,.15,7,4,.12),21.76));
        ok(close(CooperativeBalance.healAmount(6,10,.15,5,1,.12),7.5));
        ok(CooperativeBalance.extendedDuration(240,1,70)==240);
        ok(CooperativeBalance.extendedDuration(240,4,70)==450);
        System.out.println("CooperativeBalanceTest: "+n+" PASS");
    }
}
