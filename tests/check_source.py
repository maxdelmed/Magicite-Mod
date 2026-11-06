from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
src = root / 'src/main/java/com/memoria/magicite'
world = (src/'spell/SpellWorldInteractions.java').read_text()
state = (src/'spell/WorldTemporaryState.java').read_text()
utils = (src/'spell/WorldUtilities.java').read_text()
effects = (src/'spell/SpellEffects.java').read_text()
visuals = (src/'spell/SanctuaryVisuals.java').read_text()
client = (src/'client/GauntletRenderer.java').read_text()
assert 'WorldTemporaryState.setTemporary' in world
assert 'WorldTemporaryState.freezeExact' not in state
assert 'SpellWorldInteractions.freezeExact' in state
assert 'memoria_magicite_temporary_blocks' in state
assert 'WorldUtilities.drySponge' in world
assert 'WorldUtilities.ignite' in world
assert 'SanctuaryVisuals.sanctuaryDomeVisuals' in effects
assert 'static void sanctuaryDomeVisuals' in visuals
assert 'prismaticGem(' not in client
assert 'private static boolean lavaAt(' not in world
assert 'B.comment(' not in (src/'MagiciteConfig.java').read_text()
assert 'This will have consequences.' in '\n'.join(p.read_text() for p in src.rglob('*.java'))
assert 'Snow' in state or 'SNOW' in state
assert 'ItemStack.isSameItemSameTags' in state
assert 'recipe.assemble(furnace,world.registryAccess())' in state
assert 'recipe.value().assemble' not in state
assert 'Pure Memory' in (root/'src/main/resources/assets/memoria_magicite/lang/en_us.json').read_text()
poem_line = 'and snowy fields where children run.'
assert poem_line in (src/'item/MagiciteItem.java').read_text()
assert poem_line in (src/'spell/FusionDescriptions.java').read_text()
assert 'and a white Christmas playing with your son.' not in '\n'.join(p.read_text() for p in src.rglob('*.java'))
assert 'mod_version=0.12.12' in (root/'gradle.properties').read_text()
assert not list(root.glob('CHANGELOG*'))
assert not list(root.glob('HANDOFF*'))

assets = root / 'src/main/resources/assets/memoria_magicite'
for model in (assets/'models/item').glob('*.json'):
    data = json.loads(model.read_text())
    texture = data.get('textures', {}).get('layer0')
    if texture and texture.startswith('memoria_magicite:item/'):
        assert (assets/'textures/item'/(texture.split('/')[-1]+'.png')).exists(), model
sounds=json.loads((assets/'sounds.json').read_text())
for event in sounds.values():
    for entry in event.get('sounds',[]):
        name = entry if isinstance(entry,str) else entry.get('name','')
        if name.startswith('memoria_magicite:'):
            assert (assets/'sounds'/(name.split(':')[1]+'.ogg')).exists(), name
for resource in (root/'src/main/resources').rglob('*.json'):
    json.loads(resource.read_text())

print('Source layout and resources: PASS')
