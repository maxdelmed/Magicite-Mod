package tests;
import com.memoria.magicite.spell.HolyBalance;
public final class HolyBalanceTest {
    public static void main(String[] args) {
        check(HolyBalance.adjustedDamage(8f,false,1.6f)==8f);
        check(HolyBalance.adjustedDamage(8f,true,1.6f)>12f);
        check(HolyBalance.adjustedDamage(12f,true,1.6f)>19f);
        check(HolyBalance.adjustedDamage(18f,true,1.6f)>20f);
        check(HolyBalance.adjustedDamage(18f,false,1.6f)==18f);
        check(HolyBalance.adjustedDamage(18f,true,1f)==18f);
        System.out.println("HolyBalanceTest: 6 PASS");
    }
    private static void check(boolean c){ if(!c) throw new AssertionError("Holy damage math"); }
}
