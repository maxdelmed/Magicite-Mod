package tests;
import com.memoria.magicite.spell.EnvironmentBalance;
public final class EnvironmentBalanceTest {
    private static void close(float actual,float expected) {
        if(Math.abs(actual-expected)>0.00001f)throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        close(EnvironmentBalance.thunder(10f,false,false,.25,.15),10f);
        close(EnvironmentBalance.thunder(10f,true,false,.25,.15),12.5f);
        close(EnvironmentBalance.thunder(10f,false,true,.25,.15),11.5f);
        close(EnvironmentBalance.thunder(10f,true,true,.25,.15),14f);
        close(EnvironmentBalance.fire(10f,false,.2),10f);
        close(EnvironmentBalance.fire(10f,true,.2),12f);
        System.out.println("EnvironmentBalanceTest PASS");
    }
}
