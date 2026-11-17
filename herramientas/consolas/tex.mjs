import { NodeIO } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import fs from 'fs';
const [,, modo, id, ...rest] = process.argv;
const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);
const doc = await io.read(`h/${id}.glb`);
const texs = doc.getRoot().listTextures();
if (modo === 'sacar') {
  texs.forEach((t, i) => { const ext = t.getMimeType() === 'image/png' ? 'png' : 'jpg'; fs.writeFileSync(`tx/${id}_${i}.${ext}`, t.getImage()); console.log(i, t.getMimeType(), doc.getRoot().listMaterials().filter(m=>m.getBaseColorTexture()===t).map(m=>m.getName()).join(',')); });
} else if (modo === 'poner') {
  // rest: pares indice=archivo ; salida h2/<id>.glb
  for (const par of rest) { const [i, f] = par.split('='); const t = texs[+i]; t.setImage(fs.readFileSync(f)); t.setMimeType(f.endsWith('.png') ? 'image/png' : 'image/jpeg'); }
  fs.mkdirSync('h2', { recursive: true });
  await io.write(`h2/${id}.glb`, doc);
  console.log('ok');
}
