// Puente entre Díptico (Kotlin) y el kit oficial de Discord (Social SDK, C++).
//
// Todo se llama desde un solo hilo de Kotlin («discord», ver estado/Social.kt): el kit no es seguro entre hilos
// y sus respuestas llegan dentro de RunCallbacks(), que ese mismo hilo llama cada 200 ms.
// Los textos viajan como bytes UTF-8 (no como String de JNI) para que los emojis de los nombres no se rompan.

#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#include <android/log.h>
#include <jni.h>

#include <chrono>
#include <memory>
#include <optional>
#include <string>

namespace {

JavaVM* vm = nullptr;
jclass clase = nullptr;
jmethodID metodoEvento = nullptr;

std::shared_ptr<discordpp::Client> cliente;
std::shared_ptr<discordpp::AuthorizationCodeVerifier> verificador;
uint64_t appId = 0;

// Los cambios de amigos llegan en ráfagas: se marca «sucio» y se manda la lista como mucho una vez por segundo.
bool amigosSucios = false;
long long ultimoEnvio = 0;

long long ahoraMs() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

jbyteArray bytes(JNIEnv* env, const std::string& s) {
    jbyteArray a = env->NewByteArray(static_cast<jsize>(s.size()));
    if (a != nullptr && !s.empty()) env->SetByteArrayRegion(a, 0, static_cast<jsize>(s.size()), reinterpret_cast<const jbyte*>(s.data()));
    return a;
}

std::string texto(JNIEnv* env, jbyteArray a) {
    if (a == nullptr) return {};
    jsize n = env->GetArrayLength(a);
    std::string s(static_cast<size_t>(n), '\0');
    if (n > 0) env->GetByteArrayRegion(a, 0, n, reinterpret_cast<jbyte*>(&s[0]));
    return s;
}

/** Avisa a Kotlin: PuenteDiscord.evento(tipo, a, b, n). */
void emitir(const char* tipo, const std::string& a = "", const std::string& b = "", long long n = 0) {
    if (vm == nullptr || clase == nullptr || metodoEvento == nullptr) return;
    JNIEnv* env = nullptr;
    bool adjunto = false;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        adjunto = true;
    }
    jstring t = env->NewStringUTF(tipo);
    jbyteArray ja = bytes(env, a);
    jbyteArray jb = bytes(env, b);
    env->CallStaticVoidMethod(clase, metodoEvento, t, ja, jb, static_cast<jlong>(n));
    if (env->ExceptionCheck()) env->ExceptionClear();
    env->DeleteLocalRef(t);
    env->DeleteLocalRef(ja);
    env->DeleteLocalRef(jb);
    if (adjunto) vm->DetachCurrentThread();
}

std::string json(const std::string& s) {
    std::string r = "\"";
    for (unsigned char c : s) {
        switch (c) {
            case '"': r += "\\\""; break;
            case '\\': r += "\\\\"; break;
            case '\n': r += "\\n"; break;
            case '\r': r += "\\r"; break;
            case '\t': r += "\\t"; break;
            default:
                if (c < 0x20) { char b[8]; snprintf(b, sizeof b, "\\u%04x", c); r += b; }
                else r += static_cast<char>(c);
        }
    }
    return r + "\"";
}

const char* estadoDe(discordpp::StatusType s) {
    switch (s) {
        case discordpp::StatusType::Online: return "conectado";
        case discordpp::StatusType::Idle: return "ausente";
        case discordpp::StatusType::Dnd: return "ocupado";
        case discordpp::StatusType::Streaming: return "conectado";
        default: return "desconectado";
    }
}

std::string usuarioJson(const discordpp::UserHandle& u, const char* estado) {
    std::string actividad, detalle;
    if (auto a = u.GameActivity()) {
        actividad = a->Details().value_or(a->Name());
        detalle = a->State().value_or("");
    }
    std::string avatar = u.Avatar() ? u.AvatarUrl(discordpp::UserHandle::AvatarType::Png, discordpp::UserHandle::AvatarType::Png) : "";
    return "{\"id\":" + json(std::to_string(u.Id())) + ",\"nombre\":" + json(u.DisplayName()) + ",\"avatar\":" + json(avatar) +
           ",\"estado\":" + json(estado) + ",\"actividad\":" + json(actividad) + ",\"detalle\":" + json(detalle) + "}";
}

std::string amigosJson() {
    std::string s = "[";
    bool primero = true;
    auto grupo = [&](discordpp::RelationshipGroupType g, bool jugando) {
        for (auto& r : cliente->GetRelationshipsByGroup(g)) {
            bool amigo = r.DiscordRelationshipType() == discordpp::RelationshipType::Friend ||
                         r.GameRelationshipType() == discordpp::RelationshipType::Friend;
            if (!amigo) continue;
            auto u = r.User();
            if (!u) continue;
            if (!primero) s += ",";
            primero = false;
            s += usuarioJson(*u, jugando ? "jugando" : estadoDe(u->Status()));
        }
    };
    grupo(discordpp::RelationshipGroupType::OnlinePlayingGame, true);
    grupo(discordpp::RelationshipGroupType::OnlineElsewhere, false);
    grupo(discordpp::RelationshipGroupType::Offline, false);
    return s + "]";
}

std::string yoJson() {
    auto yo = cliente->GetCurrentUserV2();
    return yo ? usuarioJson(*yo, "conectado") : "";
}

void conectarCon(const std::string& acceso) {
    cliente->UpdateToken(discordpp::AuthorizationTokenType::Bearer, acceso, [](discordpp::ClientResult r) {
        if (r.Successful()) cliente->Connect();
        else emitir("error", r.Error(), "conectar", r.ErrorCode());
    });
}

void alToken(discordpp::ClientResult r, std::string acceso, std::string refresco, discordpp::AuthorizationTokenType,
             int32_t vence, std::string) {
    if (!r.Successful()) { emitir("error", r.Error(), "token", r.ErrorCode()); return; }
    emitir("token", acceso, refresco, vence);
    conectarCon(acceso);
}

}  // namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* v, void*) {
    vm = v;
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_iniciar(JNIEnv* env, jclass k, jlong app) {
    if (cliente) return JNI_TRUE;
    clase = static_cast<jclass>(env->NewGlobalRef(k));
    metodoEvento = env->GetStaticMethodID(k, "evento", "(Ljava/lang/String;[B[BJ)V");
    if (metodoEvento == nullptr) { env->ExceptionClear(); return JNI_FALSE; }
    appId = static_cast<uint64_t>(app);
    cliente = std::make_shared<discordpp::Client>();
    cliente->SetApplicationId(appId);
    cliente->AddLogCallback([](std::string m, discordpp::LoggingSeverity) {
        __android_log_print(ANDROID_LOG_INFO, "DipticoDiscord", "%s", m.c_str());
    }, discordpp::LoggingSeverity::Warning);
    cliente->SetStatusChangedCallback([](discordpp::Client::Status s, discordpp::Client::Error e, int32_t detalle) {
        emitir("estado", discordpp::Client::StatusToString(s), discordpp::Client::ErrorToString(e), static_cast<long long>(s));
        if (s == discordpp::Client::Status::Ready) {
            emitir("yo", yoJson());
            emitir("amigos", amigosJson());
            ultimoEnvio = ahoraMs();
            amigosSucios = false;
        }
        (void)detalle;
    });
    cliente->SetRelationshipGroupsUpdatedCallback([](uint64_t) { amigosSucios = true; });
    cliente->SetUserUpdatedCallback([](uint64_t) { amigosSucios = true; });
    cliente->SetRelationshipCreatedCallback([](uint64_t, bool) { amigosSucios = true; });
    cliente->SetRelationshipDeletedCallback([](uint64_t, bool) { amigosSucios = true; });
    cliente->SetTokenExpirationCallback([]() { emitir("vence"); });
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_correr(JNIEnv*, jclass) {
    if (!cliente) return;
    discordpp::RunCallbacks();
    if (amigosSucios && ahoraMs() - ultimoEnvio > 1000 &&
        cliente->GetStatus() == discordpp::Client::Status::Ready) {
        amigosSucios = false;
        ultimoEnvio = ahoraMs();
        emitir("amigos", amigosJson());
    }
}

JNIEXPORT jbyteArray JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_permisos(JNIEnv* env, jclass) {
    return bytes(env, discordpp::Client::GetDefaultPresenceScopes());
}

/** Inicio de sesión en la misma consola: abre la app de Discord (o el navegador) para tocar «Autorizar». */
JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_autorizar(JNIEnv*, jclass) {
    if (!cliente) return;
    verificador = std::make_shared<discordpp::AuthorizationCodeVerifier>(cliente->CreateAuthorizationCodeVerifier());
    discordpp::AuthorizationArgs args;
    args.SetClientId(appId);
    args.SetScopes(discordpp::Client::GetDefaultPresenceScopes());
    args.SetCodeChallenge(verificador->Challenge());
    cliente->Authorize(args, [](discordpp::ClientResult r, std::string codigo, std::string redireccion) {
        if (!r.Successful()) { emitir("error", r.Error(), "autorizar", r.ErrorCode()); return; }
        cliente->GetToken(appId, codigo, verificador->Verifier(), redireccion, alToken);
    });
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_cancelar(JNIEnv*, jclass) {
    if (cliente) cliente->AbortAuthorize();
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_usarToken(JNIEnv* env, jclass, jbyteArray acceso) {
    if (cliente) conectarCon(texto(env, acceso));
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_renovar(JNIEnv* env, jclass, jbyteArray refresco) {
    if (cliente) cliente->RefreshToken(appId, texto(env, refresco), alToken);
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_salir(JNIEnv*, jclass) {
    if (!cliente) return;
    cliente->ClearRichPresence();
    cliente->Disconnect();
}

/** «Jugando a…» en tu perfil de Discord: el juego, la consola y desde cuándo. */
JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_presencia(
    JNIEnv* env, jclass, jbyteArray titulo, jbyteArray consola, jbyteArray imagen, jlong inicio) {
    if (!cliente || cliente->GetStatus() != discordpp::Client::Status::Ready) return;
    discordpp::Activity a;
    a.SetType(discordpp::ActivityTypes::Playing);
    a.SetDetails(texto(env, titulo));
    std::string c = texto(env, consola);
    if (!c.empty()) a.SetState(c);
    std::string img = texto(env, imagen);
    if (!img.empty()) {
        discordpp::ActivityAssets assets;
        assets.SetLargeImage(img);
        assets.SetLargeText(texto(env, titulo));
        a.SetAssets(assets);
    }
    if (inicio > 0) {
        discordpp::ActivityTimestamps ts;
        ts.SetStart(static_cast<uint64_t>(inicio));
        a.SetTimestamps(ts);
    }
    cliente->UpdateRichPresence(a, [](discordpp::ClientResult r) {
        if (!r.Successful()) emitir("error", r.Error(), "presencia", r.ErrorCode());
    });
}

JNIEXPORT void JNICALL Java_cl_nikalbazu_diptico_estado_PuenteDiscord_sinPresencia(JNIEnv*, jclass) {
    if (cliente && cliente->GetStatus() == discordpp::Client::Status::Ready) cliente->ClearRichPresence();
}

}  // extern "C"
