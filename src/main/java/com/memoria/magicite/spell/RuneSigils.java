package com.memoria.magicite.spell;

import java.util.ArrayList;
import java.util.List;

public final class RuneSigils {
    private RuneSigils() {}
    public record Stroke(float x1, float z1, float x2, float z2, int layer) {}

    public static String family(String id) {
        if (id == null) return "generic";
        if (id.startsWith("fir")) return "fire";
        if (id.startsWith("blizz")) return "blizzard";
        if (id.startsWith("thund")) return "thunder";
        if (id.startsWith("gravi")) return "gravity";
        if (id.startsWith("cur")) return "cure";
        if (id.startsWith("clear")) return "clear";
        if (id.startsWith("full_life")) return "life";
        if (id.startsWith("holy")) return "holy";
        if (id.startsWith("slow") || id.equals("stop")) return "time";
        if (id.startsWith("haste")) return "haste";
        return "generic";
    }

    public static int color(String id) {
        return switch (family(id)) {
            case "fire" -> 0xFF9B4D;
            case "blizzard" -> 0xB4EFFF;
            case "thunder" -> 0xF7D16A;
            case "gravity" -> 0xC9A3FF;
            case "cure" -> 0x7AE5AC;
            case "clear" -> 0xD1FAEE;
            case "life" -> 0xFFD9C2;
            case "holy" -> 0xFFE8A4;
            case "time" -> 0xC8B6FF;
            case "haste" -> 0x7DDEFF;
            default -> 0xEADFFF;
        };
    }

    public static List<Stroke> forSpell(String id) {
        Builder b=new Builder();
        String family=family(id);
        switch (family) {
            case "fire" -> fire(b);
            case "blizzard" -> snow(b);
            case "thunder" -> thunder(b);
            case "gravity" -> gravity(b);
            case "cure" -> cure(b);
            case "clear" -> clear(b);
            case "life" -> life(b);
            case "holy" -> holy(b);
            case "time" -> time(b);
            case "haste" -> haste(b);
            default -> {b.arc(0,0,.83,0,Math.PI*2,24,0);b.star(5,.59,.22,1);}
        }

        if (id != null && (id.endsWith("_plus1") || id.endsWith("_plus2"))) {
            int count=id.endsWith("_plus2")?8:4;
            for (int i=0;i<count;i++) {
                double a=2*Math.PI*i/count;
                b.diamond(Math.cos(a)*.84,Math.sin(a)*.84,.065,1);
            }
        }
        return List.copyOf(b.out);
    }

    private static void fire(Builder b) {

        b.arc(0,0,.74,.08,Math.PI*1.85,32,0);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            b.line(Math.cos(a)*.81,Math.sin(a)*.81,Math.cos(a)*.98,Math.sin(a)*.98,2);
        }
        b.path(1,new double[][] {{-.48,-.42},{-.24,-.32},{-.34,-.07},{-.16,.12},{-.14,.50},{.04,.26},{.18,.58},{.34,.24},{.22,.01},{.42,-.20},{.34,-.46},{.09,-.56},{-.20,-.55},{-.48,-.42}});
        b.path(1,new double[][] {{-.11,-.38},{-.04,-.14},{.10,.07},{.18,-.18},{.07,-.37},{-.11,-.38}});
        b.arc(0,-.26,.48,.18,Math.PI-.18,12,2);
    }
    private static void snow(Builder b) {

        b.polygon(6,.82,Math.PI/6,0);
        b.polygon(6,.43,Math.PI/6,2);
        for(int i=0;i<6;i++) {
            double a=i*Math.PI/3;
            b.line(0,0,Math.cos(a)*.72,Math.sin(a)*.72,1);
            for(double d:new double[]{.40,.62})for(int sign:new int[]{-1,1}) {
                double tip=d-.16;
                b.line(Math.cos(a)*d,Math.sin(a)*d,
                        Math.cos(a)*tip+Math.cos(a+sign*Math.PI/3)*.19,
                        Math.sin(a)*tip+Math.sin(a+sign*Math.PI/3)*.19,1);
            }
        }
        b.diamond(0,0,.12,1);
    }
    private static void thunder(Builder b) {

        b.polygon(3,.57,-Math.PI/2,2);
        b.arc(0,0,.9,0.08,1.12,14,0);
        b.arc(0,0,.9,2.18,3.12,14,0);
        b.arc(0,0,.9,4.28,5.22,14,0);
        for(int k=0;k<3;k++) {
            double a=k*Math.PI*2/3d;
            b.rotatedPath(1,a,new double[][] {{-.13,-.09},{.14,-.1},{.00,.21},{.28,.17},{-.13,.69},{-.03,.32},{-.27,.34}});
            b.line(Math.cos(a)*.70,Math.sin(a)*.70,Math.cos(a+.18)*.95,Math.sin(a+.18)*.95,2);
        }
    }
    private static void gravity(Builder b) {

        for(int arm=0;arm<2;arm++) {
            double prevX=0,prevY=0;
            for(int i=0;i<39;i++) {
                double a=arm*Math.PI+i*.17,r=.08+i*.017;
                double x=Math.cos(a)*r,y=Math.sin(a)*r;
                if(i>0)b.line(prevX,prevY,x,y,1);
                prevX=x;prevY=y;
            }
        }
        b.arc(0,0,.90,.30,2.8,20,0);b.arc(0,0,.90,3.46,5.96,20,0);
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2+Math.PI/4;
            b.diamond(Math.cos(a)*.84,Math.sin(a)*.84,.09,2);
        }
        b.diamond(0,0,.15,1);
    }
    private static void cure(Builder b) {

        b.arc(0,0,.80,.1,Math.PI*1.87,32,0);
        for(int i=0;i<6;i++) {
            double a=i*2*Math.PI/6;
            b.ellipse(Math.cos(a)*.42,Math.sin(a)*.42,.19,.29,a-Math.PI/2,14,1);
        }
        b.diamond(0,0,.16,1);
        for(int i=0;i<3;i++) {
            double a=i*2*Math.PI/3;
            b.line(Math.cos(a)*.56,Math.sin(a)*.56,Math.cos(a+.22)*.87,Math.sin(a+.22)*.87,2);
        }
    }
    private static void clear(Builder b) {

        b.arc(0,0,.84,.0,Math.PI*2,36,0);
        for(int i=0;i<4;i++){
            double a=i*Math.PI/2;
            b.arc(0,0,.56,a+.16,a+Math.PI/2-.16,8,2);
            b.diamond(Math.cos(a)*.75,Math.sin(a)*.75,.11,1);
        }
        b.star(8,.47,.13,1);
    }
    private static void life(Builder b) {

        b.arc(0,0,.86,.05,Math.PI*1.9,38,0);
        b.arc(0,0,.56,.14,Math.PI*1.82,30,2);
        b.star(8,.68,.29,1);
        b.path(1,new double[][]{{0,-.28},{-.35,.04},{-.29,.25},{-.1,.29},{0,.15},{.1,.29},{.29,.25},{.35,.04},{0,-.28}});
    }
    private static void holy(Builder b) {

        b.arc(0,0,.84,.0,Math.PI*2,32,0);
        b.triangle(.58,-Math.PI/2,1);b.triangle(.58,Math.PI/2,1);
        b.star(8,.77,.26,2);
        b.diamond(0,0,.21,1);
    }
    private static void time(Builder b) {

        b.arc(0,0,.85,0,Math.PI*2,36,0);
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;
            b.line(Math.cos(a)*.69,Math.sin(a)*.69,Math.cos(a)*.78,Math.sin(a)*.78,2);
        }
        b.path(1,new double[][]{{-.38,-.54},{.38,-.54},{-.28,.0},{.38,.54},{-.38,.54},{.28,.0},{-.38,-.54}});
        b.line(-.18,-.4,.18,-.4,2);b.line(-.18,.4,.18,.4,2);
    }
    private static void haste(Builder b) {

        for(int k=0;k<3;k++){
            double a=k*2*Math.PI/3d;
            b.rotatedPath(1,a,new double[][]{{-.5,-.34},{-.28,-.07},{.02,.11},{.32,.15},{.6,.06}});
            b.rotatedPath(1,a,new double[][]{{.42,-.11},{.6,.06},{.42,.24}});
        }
        b.arc(0,0,.82,.14,2.70,20,2);b.arc(0,0,.82,3.26,5.80,20,2);
    }

    private static final class Builder {
        final List<Stroke> out=new ArrayList<>();
        void line(double x1,double y1,double x2,double y2,int layer){
            out.add(new Stroke((float)x1,(float)y1,(float)x2,(float)y2,layer));
        }
        void path(int layer,double[][] pts){for(int i=1;i<pts.length;i++)
            line(pts[i-1][0],pts[i-1][1],pts[i][0],pts[i][1],layer);}
        void rotatedPath(int layer,double angle,double[][] pts) {
            double c=Math.cos(angle),s=Math.sin(angle);
            double[][] pp=new double[pts.length][2];
            for(int i=0;i<pts.length;i++){
                pp[i][0]=pts[i][0]*c-pts[i][1]*s;
                pp[i][1]=pts[i][0]*s+pts[i][1]*c;
            }
            path(layer,pp);
        }
        void arc(double cx,double cy,double r,double start,double end,int segments,int layer){
            for(int i=0;i<segments;i++){
                double a=start+(end-start)*i/segments;
                double b=start+(end-start)*(i+1)/segments;
                line(cx+Math.cos(a)*r,cy+Math.sin(a)*r,cx+Math.cos(b)*r,cy+Math.sin(b)*r,layer);
            }
        }
        void polygon(int n,double r,double offset,int layer) {
            for(int i=0;i<n;i++){
                double a=offset+2*Math.PI*i/n, b=offset+2*Math.PI*(i+1)/n;
                line(Math.cos(a)*r,Math.sin(a)*r,Math.cos(b)*r,Math.sin(b)*r,layer);
            }
        }
        void star(int n,double outer,double inner,int layer){
            for(int i=0;i<n;i++){
                double a=i*2*Math.PI/n,b=a+Math.PI/n,c=(i+1)*2*Math.PI/n;
                line(Math.cos(a)*outer,Math.sin(a)*outer,Math.cos(b)*inner,Math.sin(b)*inner,layer);
                line(Math.cos(b)*inner,Math.sin(b)*inner,Math.cos(c)*outer,Math.sin(c)*outer,layer);
            }
        }
        void diamond(double x,double y,double size,int layer){
            path(layer,new double[][] {{x,y-size},{x+size,y},{x,y+size},{x-size,y},{x,y-size}});
        }
        void triangle(double radius,double angle,int layer) {
            for(int i=0;i<3;i++){
                double a=angle+i*2*Math.PI/3,b=angle+(i+1)*2*Math.PI/3;
                line(Math.cos(a)*radius,Math.sin(a)*radius,Math.cos(b)*radius,Math.sin(b)*radius,layer);
            }
        }
        void ellipse(double cx,double cy,double rx,double ry,double a,int n,int layer){
            double c=Math.cos(a),s=Math.sin(a);
            double prevX=0,prevY=0;
            for(int i=0;i<=n;i++) {
                double t=i*2*Math.PI/n;
                double x=cx+Math.cos(t)*rx*c-Math.sin(t)*ry*s;
                double y=cy+Math.cos(t)*rx*s+Math.sin(t)*ry*c;
                if(i>0)line(prevX,prevY,x,y,layer);
                prevX=x;prevY=y;
            }
        }
    }
}
