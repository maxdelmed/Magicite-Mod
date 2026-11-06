package tests;
import com.memoria.magicite.spell.*;
import java.util.*;
public final class FusionDescriptionsTest {
    public static void main(String[] a) {
        Set<String> found=new HashSet<>();
        MagiciteKind[] kinds=MagiciteKind.values();
        for(MagiciteKind k:kinds) if(FusionDescriptions.forSingle(k).isBlank())throw new AssertionError(k);
        for(MagiciteKind a1:kinds)for(MagiciteKind b:kinds)for(MagiciteKind c:kinds)for(MagiciteKind d:kinds){
            List<MagiciteKind> recipe=List.of(a1,b,c,d);
            for(int n=2;n<=4;n++)for(boolean late:new boolean[]{false,true})for(boolean multiplayer:new boolean[]{false,true}){
                String id=FusionRules.result(recipe.subList(0,n),multiplayer,late);
                if(!id.isEmpty())found.add(id);
            }
        }
        for(String id:found) {
            String d=FusionDescriptions.forFusion(id);
            if(d.isBlank() || d.equals("Separate base spells") || (d.length()>35 && !id.equals("sanctuary_of_memories")))throw new AssertionError(id+": "+d);
            if(id.equals("phoenix_guard")||id.equals("purify")||id.equals("renewal")||id.equals("lifera"))throw new AssertionError("invented spell "+id);
        }
        System.out.println("FusionDescriptionsTest: "+found.size()+" distinct supported fusion names PASS");
    }
}
