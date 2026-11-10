# Díptico: se conserva todo el código propio (el optimizador igual achica las librerías).
# allowoptimization: R8 puede optimizar (inlining, quitar código muerto) sin cambiar nombres: más fluido.
-keep,allowoptimization class cl.nikalbazu.diptico.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# IA local (MediaPipe)
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
-dontwarn com.google.auto.value.**
-dontwarn javax.annotation.**

# Kit de Discord (Social SDK): su parte en C++ llama a estas clases por nombre.
-keep class com.discord.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn com.discord.**
-dontwarn org.webrtc.**
