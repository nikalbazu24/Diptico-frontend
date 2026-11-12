// Receptor de Díptico. La propiedad CARPETA_REPORTES contiene el ID de una carpeta privada.
const MAXIMO_DIARIO = 120;

function doPost(e) {
  try {
    const peticion = e && e.postData;
    if (!peticion || !peticion.contents || peticion.contents.length > 3000000) return responder(false);
    const r = JSON.parse(peticion.contents);
    // Cajas 3D de la comunidad: van a su propia carpeta (ver guardarCaja).
    if (r && r.app === "diptico" && r.tipo === "caja") return guardarCaja(r);
    if (peticion.contents.length > 25000) return responder(false);
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

// ---------- Cajas 3D de la comunidad ----------
// La carpeta se crea sola la primera vez (propiedad CARPETA_CAJAS). Cada caja es un JSON <clave>.json con sus caras en JPEG (base64).
const MAXIMO_CAJAS_DIARIO = 400;
const CLAVE_CAJA = /^[a-z0-9]{1,20}-[a-z0-9-]{1,90}$/;

function carpetaCajas() {
  const propiedades = PropertiesService.getScriptProperties();
  const id = propiedades.getProperty("CARPETA_CAJAS");
  if (id) return DriveApp.getFolderById(id);
  const carpeta = DriveApp.createFolder("Díptico · Cajas de la comunidad");
  propiedades.setProperty("CARPETA_CAJAS", carpeta.getId());
  return carpeta;
}

function caraValida(t) {
  return t === undefined || (typeof t === "string" && t.length < 900000 && /^[A-Za-z0-9+/=]+$/.test(t));
}

function guardarCaja(r) {
  if (typeof r.clave !== "string" || !CLAVE_CAJA.test(r.clave) || !caraValida(r.frente) || !caraValida(r.lomo) ||
      !caraValida(r.reverso) || !(r.frente || r.lomo || r.reverso)) return responder(false);
  const propiedades = PropertiesService.getScriptProperties();
  const bloqueo = LockService.getScriptLock();
  if (!bloqueo.tryLock(6000)) return responder(false);
  try {
    const dia = new Date().toISOString().slice(0, 10);
    const cantidad = propiedades.getProperty("CAJAS_DIA") === dia ? Number(propiedades.getProperty("CAJAS_CANTIDAD")) || 0 : 0;
    if (cantidad >= MAXIMO_CAJAS_DIARIO) return responder(false);
    propiedades.setProperties({ CAJAS_DIA: dia, CAJAS_CANTIDAD: String(cantidad + 1) });
    const carpeta = carpetaCajas();
    const nombre = r.clave + ".json";
    const datos = JSON.stringify({
      clave: r.clave, titulo: String(r.titulo || "").slice(0, 120), consola: String(r.consola || "").slice(0, 60),
      frente: r.frente || "", lomo: r.lomo || "", reverso: r.reverso || "", fecha: new Date().toISOString(),
    });
    const existentes = carpeta.getFilesByName(nombre);
    if (existentes.hasNext()) existentes.next().setContent(datos);
    else carpeta.createFile(nombre, datos, MimeType.PLAIN_TEXT);
    return responder(true, r.id);
  } finally {
    bloqueo.releaseLock();
  }
}

// GET ?caja=<clave>: devuelve la caja guardada (o ok:false si no está).
function doGet(e) {
  try {
    const clave = e && e.parameter && e.parameter.caja;
    if (typeof clave !== "string" || !CLAVE_CAJA.test(clave)) return responder(false);
    const archivos = carpetaCajas().getFilesByName(clave + ".json");
    if (!archivos.hasNext()) return responder(false);
    const c = JSON.parse(archivos.next().getBlob().getDataAsString());
    return ContentService.createTextOutput(JSON.stringify({ ok: true, frente: c.frente, lomo: c.lomo, reverso: c.reverso }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return responder(false);
  }
}
