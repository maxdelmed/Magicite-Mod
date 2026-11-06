package tests;
import com.memoria.magicite.spell.*;
import java.util.List;
import static com.memoria.magicite.spell.MagiciteKind.*;
public final class FusionRulesTest {
    private static int checks=0;
    static void eq(String actual,String expected){checks++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] a) {
        eq(FusionRules.result(List.of(FIRE)),"");
        eq(FusionRules.result(List.of(FIRE,FIRE)),"fira");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE)),"firaga");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE,FIRE)),"firaga_plus2");
        eq(FusionRules.result(List.of(BLIZZARD,BLIZZARD)),"blizzara");
        eq(FusionRules.result(List.of(THUNDER,THUNDER,THUNDER,THUNDER)),"thundaga_plus2");
        eq(FusionRules.result(List.of(FIRE,BLIZZARD)),"gravity");
        eq(FusionRules.result(List.of(FIRE,BLIZZARD,THUNDER)),"graviga");
        eq(FusionRules.result(List.of(FIRE,BLIZZARD,THUNDER,FIRE)),"gravira");
        eq(FusionRules.result(List.of(CURE,CURE)),"curaga");
        eq(FusionRules.result(List.of(CLEAR,CLEAR)),"clearaga");
        eq(FusionRules.result(List.of(LIFE,LIFE)),"full_life");
        eq(FusionRules.result(List.of(LIFE,CURE,CURE)),"haste");
        eq(FusionRules.result(List.of(LIFE,CURE,CURE,CURE)),"hastega");
        eq(FusionRules.result(List.of(FIRE,LIFE)),"holy");
        eq(FusionRules.result(List.of(LIFE,FIRE)),"slow");
        eq(FusionRules.result(List.of(FIRE,LIFE,LIFE)),"holyra");
        eq(FusionRules.result(List.of(FIRE,LIFE,LIFE,LIFE)),"holyra_plus1");
        eq(FusionRules.result(List.of(LIFE,FIRE,BLIZZARD)),"stop");
        eq(FusionRules.result(List.of(LIFE,FIRE,BLIZZARD),true,false),"slowga");
        eq(FusionRules.result(List.of(LIFE,FIRE,BLIZZARD),true,true),"stop");
        eq(FusionRules.result(List.of(FIRE,FIRE),true,true),"firaga");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE),true,false),"fira_plus1");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE),true,true),"firaga_plus1");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE,FIRE),true,false),"fira_plus2");
        eq(FusionRules.result(List.of(FIRE,FIRE,FIRE,FIRE),true,true),"firaga_plus2");
        eq(FusionRules.result(List.of(FIRE,CURE)),"");
        eq(FusionRules.result(List.of(LIFE,CLEAR)),"");
        eq(FusionRules.result(List.of(LIFE,LIFE,CURE)),"");
        eq(FusionRules.result(List.of(CURE,CURE,CURE,CURE,CURE)),"");
        if (FIRE.durability!=60 || LIFE.durability!=15) throw new AssertionError("durability mismatch");
        System.out.println("FusionRulesTest: "+checks+" PASS");
    }
}
