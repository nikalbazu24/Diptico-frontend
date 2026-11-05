// Receptor de Díptico. La propiedad CARPETA_REPORTES contiene el ID de una carpeta privada.
const MAXIMO_DIARIO = 120;

function doPost(e) {
  try {
    const peticion = e && e.postData;
    if (!peticion || !peticion.contents || peticion.contents.length > 25000) return responder(false);
    const r = JSON.parse(peticion.contents);
    if (!r || r.app !== "diptico" || typeof r.id !== "string" ||
        !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(r.id) ||
        !["manual", "caida"].includes(r.tipo) || typeof r.texto !== "string" ||
        !r.texto.trim() || r.texto.length > 7100) return responder(false);

    const propiedades = PropertiesService.getScriptProperties();
    const carpetaId = propiedades.getProperty("CARPETA_REPORTES");
    if (!carpetaId) return responder(false);
    const bloqueo = LockService.getScriptLock();
    if (!bloqueo.tryLock(4000)) return responder(false);
    try {
      const carpeta = DriveApp.getFolderById(carpetaId);
      const nombre = "Diptico-" + r.id + ".txt";
      if (carpeta.getFilesByName(nombre).hasNext()) return responder(true, r.id);
      const ahora = new Date();
      const dia = ahora.toISOString().slice(0, 10);
      const cantidad = propiedades.getProperty("REPORTES_DIA") === dia
        ? Number(propiedades.getProperty("REPORTES_CANTIDAD")) || 0 : 0;
      if (cantidad >= MAXIMO_DIARIO) return responder(false);
      propiedades.setProperties({ REPORTES_DIA: dia, REPORTES_CANTIDAD: String(cantidad + 1) });
      carpeta.createFile(nombre, "Díptico · " + r.tipo + "\nRecibido: " + ahora.toISOString() +
          "\nCódigo: " + r.id + "\n\n" + r.texto, MimeType.PLAIN_TEXT);
      return responder(true, r.id);
    } finally {
      bloqueo.releaseLock();
    }
  } catch (e) {
    return responder(false);
  }
}

function responder(ok, id) {
  return ContentService.createTextOutput(JSON.stringify(ok ? { ok: true, id } : { ok: false }))
    .setMimeType(ContentService.MimeType.JSON);
}
