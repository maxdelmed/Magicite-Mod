package tests;
import com.memoria.magicite.spell.*;
import static com.memoria.magicite.spell.MagiciteKind.*;
import java.util.*;
public final class SpellProfileTest {
    private static int checks=0;
    static void chk(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        chk(SpellProfile.SINGLE_CHARGE==22);
        chk(SpellProfile.chargeTicks(1,false)==18);
        chk(SpellProfile.chargeTicks(2,false)==26);
        chk(SpellProfile.chargeTicks(3,false)==32);
        chk(SpellProfile.chargeTicks(4,false)==38);
        chk(SpellProfile.chargeTicks(1,true)==39);
        chk(SpellProfile.radius("firaga")>SpellProfile.radius("fira"));
        chk(SpellProfile.radius("firaga_plus2")>SpellProfile.radius("firaga_plus1"));
        chk(SpellProfile.radius("fira_plus2")>SpellProfile.radius("fira"));
        chk(SpellProfile.radius(List.of(FIRE,FIRE,FIRE,FIRE))==SpellProfile.radius("firaga_plus2"));
        chk(SpellProfile.radius(List.of(FIRE,FIRE),true)>SpellProfile.radius("fira"));
        chk(SpellProfile.radius("hastega")>SpellProfile.radius("haste"));
        chk(SpellProfile.circlesOverlap(3,2.2,2.2));
        chk(THUNDER.color==0xA46EFF);chk(CLEAR.color==0xF1F9F5);
        System.out.println("SpellProfileTest: "+checks+" PASS");
    }
}
