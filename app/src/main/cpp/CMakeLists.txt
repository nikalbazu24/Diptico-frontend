# Puente entre Díptico y el kit oficial de Discord (Social SDK).
# KIT apunta a la carpeta «prefab» del discord_partner_sdk.aar ya descomprimido (lo hace app/build.gradle.kts).
cmake_minimum_required(VERSION 3.22.1)
project(diptico_discord CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(MODULO "${KIT}/prefab/modules/discord_partner_sdk")
add_library(discord_partner_sdk SHARED IMPORTED)
set_target_properties(discord_partner_sdk PROPERTIES
    IMPORTED_LOCATION "${MODULO}/libs/android.${ANDROID_ABI}/libdiscord_partner_sdk.so")

add_library(diptico_discord SHARED puente_discord.cpp)
target_include_directories(diptico_discord PRIVATE "${MODULO}/include")
target_link_libraries(diptico_discord discord_partner_sdk log)
