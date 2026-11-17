import { NodeIO } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune } from '@gltf-transform/functions';
const [,, f] = process.argv;
const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);
const doc = await io.read(f);
for (const m of doc.getRoot().listMaterials()) { m.setEmissiveTexture(null); m.setEmissiveFactor([0,0,0]); }
await doc.transform(prune());
await io.write(f, doc); console.log('sin emisión', f);
