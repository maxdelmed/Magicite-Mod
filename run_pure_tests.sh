#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac --release 17 -d "$OUT" \
 src/main/java/com/memoria/magicite/spell/MagiciteKind.java \
 src/main/java/com/memoria/magicite/spell/FusionRules.java \
 src/main/java/com/memoria/magicite/spell/HalfChargeMath.java \
 src/main/java/com/memoria/magicite/spell/QuarterChargeMath.java \
 src/main/java/com/memoria/magicite/spell/SpellProfile.java \
 src/main/java/com/memoria/magicite/spell/SpellBalance.java \
 src/main/java/com/memoria/magicite/spell/CooperativeBalance.java \
 src/main/java/com/memoria/magicite/spell/FusionDescriptions.java \
 src/main/java/com/memoria/magicite/spell/FusionNames.java \
 src/main/java/com/memoria/magicite/spell/HolyBalance.java \
 src/main/java/com/memoria/magicite/spell/EnvironmentBalance.java \
 src/main/java/com/memoria/magicite/spell/BlizzardBalance.java \
 src/main/java/com/memoria/magicite/spell/SacrificeBalance.java \
 src/main/java/com/memoria/magicite/spell/FusionLatencyBalance.java \
 src/main/java/com/memoria/magicite/spell/RuneSigils.java \
 tests/FusionRulesTest.java tests/SpellProfileTest.java \
 tests/FusionDescriptionsTest.java tests/HalfChargeMathTest.java tests/QuarterChargeMathTest.java tests/SpellBalanceTest.java tests/CooperativeBalanceTest.java \
 tests/FusionNamesTest.java tests/HolyBalanceTest.java tests/EnvironmentBalanceTest.java tests/BlizzardBalanceTest.java tests/SacrificeBalanceTest.java tests/RuneSigilsTest.java tests/FusionLatencyBalanceTest.java
for f in FusionRulesTest SpellProfileTest FusionDescriptionsTest HalfChargeMathTest QuarterChargeMathTest SpellBalanceTest CooperativeBalanceTest FusionNamesTest HolyBalanceTest EnvironmentBalanceTest BlizzardBalanceTest SacrificeBalanceTest RuneSigilsTest FusionLatencyBalanceTest; do
  java -cp "$OUT" "tests.$f"
done
javac --release 17 -d "$OUT" tests/SourceSyntaxCheck.java
java -cp "$OUT" tests.SourceSyntaxCheck
python3 tests/check_source.py
