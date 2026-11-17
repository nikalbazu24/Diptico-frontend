import { NodeIO } from '@gltf-transform/core';
import { prune } from '@gltf-transform/functions';
const [,, entrada, salida, patron] = process.argv;
const io = new NodeIO();
const doc = await io.read(entrada);
const re = new RegExp(patron, 'i');
for (const n of doc.getRoot().listNodes()) {
  const m = n.getMesh();
  if ((m && re.test(m.getName())) || re.test(n.getName())) { console.log('quito', n.getName(), m && m.getName()); n.dispose(); }
}
await doc.transform(prune());
await io.write(salida, doc);
