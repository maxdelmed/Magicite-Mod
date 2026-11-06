package tests;

import com.memoria.magicite.spell.SacrificeBalance;

public final class SacrificeBalanceTest {
    private static void equals(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError("Expected " + expected + " but was " + actual);
    }
    public static void main(String[] args) {

        equals(SacrificeBalance.healthCostHp(20f, .8f), 16f);
        equals(SacrificeBalance.projectedHealthHp(20f, 20f, .8f), 4f);

        equals(SacrificeBalance.projectedHealthHp(5f, 20f, .8f), -11f);

        equals(SacrificeBalance.projectedHealthHp(40f, 40f, .8f), 8f);
        equals(SacrificeBalance.projectedHealthHp(5f, 40f, .8f), -27f);

        equals(SacrificeBalance.projectedHealthHp(15f, 20f, .8f), -1f);
        System.out.println("SacrificeBalanceTest passed");
    }
}
