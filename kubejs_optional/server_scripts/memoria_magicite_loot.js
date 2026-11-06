

LootJS.modifiers(event => {
  
  event.addLootTableModifier('minecraft:chests/abandoned_mineshaft').randomChance(0.09).addLoot('memoria_magicite:fire_magicite')
  event.addLootTableModifier('minecraft:chests/shipwreck_treasure').randomChance(0.08).addLoot('memoria_magicite:blizzard_magicite')
  event.addLootTableModifier('minecraft:chests/desert_pyramid').randomChance(0.08).addLoot('memoria_magicite:thunder_magicite')
  event.addLootTableModifier('minecraft:chests/simple_dungeon').randomChance(0.06).addLoot('memoria_magicite:cure_magicite')
  event.addLootTableModifier('minecraft:chests/jungle_temple').randomChance(0.06).addLoot('memoria_magicite:clear_magicite')
  
  event.addLootTableModifier('minecraft:chests/ancient_city').randomChance(0.005).addLoot('memoria_magicite:life_magicite')
  
  event.addLootTableModifier('minecraft:chests/stronghold_corridor').randomChance(0.08).addLoot('memoria_magicite:magicite_shard')
  event.addLootTableModifier('minecraft:chests/ancient_city').randomChance(0.12).addLoot('memoria_magicite:magicite_shard')
})
