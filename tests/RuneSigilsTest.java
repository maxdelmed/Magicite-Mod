package tests;
import com.memoria.magicite.spell.*;
import java.util.*;
public final class RuneSigilsTest {
    public static void main(String[] args){
        Set<String> used=new HashSet<>();
        Map<String,String> signatures=new HashMap<>();
        int count=0;
        for(MagiciteKind a:MagiciteKind.values())for(MagiciteKind b:MagiciteKind.values())
            for(MagiciteKind c:MagiciteKind.values())for(MagiciteKind d:MagiciteKind.values()){
                List<MagiciteKind> four=List.of(a,b,c,d);
                for(int n=2;n<=4;n++)for(boolean multi:new boolean[]{false,true})for(boolean late:new boolean[]{false,true}){
                    String id=FusionRules.result(four.subList(0,n),multi,late);
                    if(id.isEmpty()||id.equals("sanctuary_of_memories")||!used.add(id))continue;
                    String family=RuneSigils.family(id);
                    if(family.equals("generic"))throw new AssertionError("Unstyled fusion "+id);
                    var strokes=RuneSigils.forSpell(id);
                    if(strokes.size()<18||strokes.size()>240)throw new AssertionError("Bad geometry "+id+" "+strokes.size());
                    if(RuneSigils.color(id)==0)throw new AssertionError("Missing color "+id);
                    String shape=strokes.toString();
                    String old=signatures.putIfAbsent(family,shape);
                    if(old!=null && !id.endsWith("_plus1") && !id.endsWith("_plus2") && !shape.equals(old))
                        throw new AssertionError("Inconsistent family " + id);
                    count++;
                }
            }
        for(var x:Map.of("fira_plus2","fire","blizzaga_plus2","blizzard",
                "thundaga_plus2","thunder","holyra_plus1","holy","full_life","life").entrySet())
            if(!RuneSigils.family(x.getKey()).equals(x.getValue()))throw new AssertionError(x);
        if(count<20||signatures.size()<8)throw new AssertionError("Coverage too small "+count);
        System.out.println("RuneSigilsTest: "+count+" fusion IDs in "+signatures.size()+" families PASS");
    }
}
