package tests;
import com.memoria.magicite.spell.SpellBalance;
import java.util.List;
import static com.memoria.magicite.spell.MagiciteKind.*;
public final class SpellBalanceTest {
    private static int checks=0;
    private static void test(boolean condition){checks++;if(!condition)throw new AssertionError("Balance test #"+checks);}
    private static float damage(String spell){ return SpellBalance.elementalDamage(spell,7,10,9,11,12,14); }
    public static void main(String[] args){
        test(damage("fira")==7);
        test(damage("blizzara")==7);
        test(damage("thundara") == 7);
        test(damage("firaga")==10);
        test(damage("blizzaga")==10);
        test(damage("thundaga")==10);
        test(damage("fira_plus1")==9);
        test(damage("fira_plus2")==11);
        test(damage("firaga_plus1")==12);
        test(damage("firaga_plus2")==14);
        test(damage("blizzaga_plus2")==14);
        test(damage("thundara_plus2")==11);
        test(SpellBalance.cooldownTicks(1,false,false,60,80,100,140,120,180)==60);
        test(SpellBalance.cooldownTicks(2,false,false,60,80,100,140,120,180)==80);
        test(SpellBalance.cooldownTicks(3,false,false,60,80,100,140,120,180)==100);
        test(SpellBalance.cooldownTicks(4,false,false,60,80,100,140,120,180)==140);
        test(SpellBalance.cooldownTicks(1,true,false,60,80,100,140,120,180)==120);
        test(SpellBalance.cooldownTicks(4,true,false,60,80,100,140,120,180)==140);
        test(SpellBalance.cooldownTicks(2,false,true,60,80,100,140,120,180)==180);
        test(SpellBalance.cooldownTicks(4,false,true,60,80,100,140,120,180)==180);

        test(SpellBalance.cooldownForSpell("", List.of(FIRE),36,46,56,68,70,100)==36);
        test(SpellBalance.cooldownForSpell("fira", List.of(FIRE,FIRE),36,46,56,68,70,100)==46);
        test(SpellBalance.cooldownForSpell("firaga", List.of(FIRE,FIRE,FIRE),36,46,56,68,70,100)==56);
        test(SpellBalance.cooldownForSpell("firaga_plus2", List.of(FIRE,FIRE,FIRE,FIRE),36,46,56,68,70,100)==68);
        test(SpellBalance.cooldownForSpell("holy", List.of(FIRE,LIFE),36,46,56,68,70,100)==46);
        test(SpellBalance.cooldownForSpell("holyra", List.of(FIRE,LIFE,LIFE),36,46,56,68,70,100)==56);
        test(SpellBalance.cooldownForSpell("holyra_plus1", List.of(FIRE,LIFE,LIFE,LIFE),36,46,56,68,70,100)==68);
        test(SpellBalance.cooldownForSpell("slow", List.of(LIFE,FIRE),36,46,56,68,70,100)==46);
        test(SpellBalance.cooldownForSpell("haste", List.of(LIFE,CURE,CURE),36,46,56,68,70,100)==56);
        test(SpellBalance.cooldownForSpell("hastega", List.of(LIFE,CURE,CURE,CURE),36,46,56,68,70,100)==68);
        test(SpellBalance.cooldownForSpell("", List.of(CURE),36,46,56,68,70,100)==70);
        test(SpellBalance.cooldownForSpell("curaga", List.of(CURE,CURE),36,46,56,68,70,100)==70);
        test(SpellBalance.cooldownForSpell("", List.of(LIFE),36,46,56,68,70,100)==100);
        test(SpellBalance.cooldownForSpell("full_life", List.of(LIFE,LIFE),36,46,56,68,70,100)==100);
        test(SpellBalance.cooldownForSpell("clearaga", List.of(CLEAR,CLEAR),36,46,56,68,70,100)==46);
        test(SpellBalance.downwardVelocity(0,.55)<0);
        test(SpellBalance.downwardVelocity(.3,.55)<0);
        test(SpellBalance.downwardVelocity(-.99,.55)>=-1.25);
        test(SpellBalance.downwardVelocity(0,0)==0);
        test(SpellBalance.controlResistant(20,true,false,80));
        test(SpellBalance.controlResistant(20,false,true,80));
        test(SpellBalance.controlResistant(80,false,false,80));
        test(!SpellBalance.controlResistant(79,false,false,80));
        System.out.println("SpellBalanceTest: "+checks+" PASS");
    }
}
