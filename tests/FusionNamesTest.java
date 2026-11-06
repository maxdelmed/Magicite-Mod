package tests;
import com.memoria.magicite.spell.*;
import java.util.*;
public final class FusionNamesTest {
    private static int checks = 0;
    private static void check(boolean b) { checks++; if (!b) throw new AssertionError("Display test " + checks); }
    public static void main(String[] args) {
        Map<String,String> variants = new HashMap<>();
        for (MagiciteKind a: MagiciteKind.values())
            for (MagiciteKind b: MagiciteKind.values())
                for (MagiciteKind c: MagiciteKind.values())
                    for (MagiciteKind d: MagiciteKind.values()) {
                        List<MagiciteKind> all=List.of(a,b,c,d);
                        for(int len=2;len<=4;len++)for(boolean mp:new boolean[]{false,true})for(boolean late:new boolean[]{false,true}) {
                            String id=FusionRules.result(all.subList(0,len),mp,late);
                            if(id.isEmpty())continue;
                            String display=FusionNames.display(id);
                            check(!display.isBlank() && !display.toLowerCase().contains("plus") && !display.contains("+"));
                            if(id.contains("plus")) {
                                String prev = variants.putIfAbsent(id, display);
                                check(prev == null || prev.equals(display));
                            }
                        }
                    }
        check(variants.size() == 13);
        check(new HashSet<>(variants.values()).size() == variants.size());
        check("Divine Radiance".equals(FusionNames.display("holyra_plus1")));
        check("Holyra".equals(FusionNames.display("holyra")));
        System.out.println("FusionNamesTest: " + checks + " PASS");
    }
}
