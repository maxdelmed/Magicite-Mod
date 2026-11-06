package tests;
import com.memoria.magicite.spell.FusionLatencyBalance;
public final class FusionLatencyBalanceTest {
    private static void eq(boolean expected,boolean actual) {if(expected!=actual)throw new AssertionError("expected "+expected+" got "+actual);}
    public static void main(String[] args) {
        eq(true,FusionLatencyBalance.synchronizedRelease(100,116,16,2,20,80));
        eq(false,FusionLatencyBalance.synchronizedRelease(100,117,16,2,20,80));
        eq(true,FusionLatencyBalance.synchronizedRelease(100,118,16,2,130,80));
        eq(false,FusionLatencyBalance.synchronizedRelease(100,119,16,2,130,80));
        eq(false,FusionLatencyBalance.synchronizedRelease(100,117,16,0,150,150));
        eq(true,FusionLatencyBalance.synchronizedRelease(100,118,16,100,150,150));
        eq(false,FusionLatencyBalance.synchronizedRelease(100,119,16,100,150,150));
        eq(true,FusionLatencyBalance.synchronizedRelease(118,100,16,2,130,80));
        System.out.println("FusionLatencyBalanceTest: 8 PASS");
    }
}
