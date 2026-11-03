import xml.etree.ElementTree as ET, json, re, sys
import os
base=os.environ.get('ESDE','esde/resources/systems/android/')
sis=ET.parse(base+'es_systems.xml').getroot()
reg=ET.parse(base+'es_find_rules.xml').getroot()
lib=[l.strip() for l in open('libretro_sistemas.txt') if l.strip()]
icon_names=set()
for l in open('ra_iconos.txt'):
    n=l.strip().split('/')[-1]
    if n.endswith('.png') and not n.endswith('-content.png'): icon_names.add(n[:-4])
def norm(s): return re.sub(r'[^a-z0-9]','',s.lower())
libn={norm(x):x for x in lib}
manual={
 'nes':'Nintendo - Nintendo Entertainment System','famicom':'Nintendo - Nintendo Entertainment System',
 'snes':'Nintendo - Super Nintendo Entertainment System','sfc':'Nintendo - Super Nintendo Entertainment System',
 'genesis':'Sega - Mega Drive - Genesis','megadrive':'Sega - Mega Drive - Genesis','megadrivejp':'Sega - Mega Drive - Genesis',
 'mastersystem':'Sega - Master System - Mark III','gamegear':'Sega - Game Gear','sega32x':'Sega - 32X','segacd':'Sega - Mega-CD - Sega CD','megacd':'Sega - Mega-CD - Sega CD','megacdjp':'Sega - Mega-CD - Sega CD',
 'saturn':'Sega - Saturn','saturnjp':'Sega - Saturn','dreamcast':'Sega - Dreamcast','sg-1000':'Sega - SG-1000',
 'pcengine':'NEC - PC Engine - TurboGrafx 16','tg16':'NEC - PC Engine - TurboGrafx 16','pcenginecd':'NEC - PC Engine CD - TurboGrafx-CD','tg-cd':'NEC - PC Engine CD - TurboGrafx-CD','supergrafx':'NEC - PC Engine SuperGrafx','pcfx':'NEC - PC-FX',
 'psx':'Sony - PlayStation','ps2':'Sony - PlayStation 2','ps3':'Sony - PlayStation 3','psp':'Sony - PlayStation Portable','psvita':'Sony - PlayStation Vita',
 'gb':'Nintendo - Game Boy','gbc':'Nintendo - Game Boy Color','gba':'Nintendo - Game Boy Advance','nds':'Nintendo - Nintendo DS','n3ds':'Nintendo - Nintendo 3DS','3ds':'Nintendo - Nintendo 3DS',
 'n64':'Nintendo - Nintendo 64','n64dd':'Nintendo - Nintendo 64DD','gc':'Nintendo - GameCube','wii':'Nintendo - Wii','wiiu':'Nintendo - Wii U','virtualboy':'Nintendo - Virtual Boy','fds':'Nintendo - Family Computer Disk System','pokemini':'Nintendo - Pokemon Mini','satellaview':'Nintendo - Satellaview','sufami':'Nintendo - Sufami Turbo',
 'xbox':'Microsoft - Xbox','xbox360':'Microsoft - Xbox 360','msx':'Microsoft - MSX','msx2':'Microsoft - MSX2',
 'atari2600':'Atari - 2600','atari5200':'Atari - 5200','atari7800':'Atari - 7800','atarilynx':'Atari - Lynx','atarijaguar':'Atari - Jaguar','atarist':'Atari - ST',
 'wonderswan':'Bandai - WonderSwan','wonderswancolor':'Bandai - WonderSwan Color','ngp':'SNK - Neo Geo Pocket','ngpc':'SNK - Neo Geo Pocket Color','neogeo':'SNK - Neo Geo','neogeocd':'SNK - Neo Geo CD','neogeocdjp':'SNK - Neo Geo CD',
 'colecovision':'Coleco - ColecoVision','intellivision':'Mattel - Intellivision','vectrex':'GCE - Vectrex','c64':'Commodore - 64','amiga':'Commodore - Amiga','dos':'DOS','scummvm':'ScummVM',
 'arcade':'MAME','mame':'MAME','fbneo':'FBNeo - Arcade Games','cps1':'FBNeo - Arcade Games','cps2':'FBNeo - Arcade Games','cps3':'FBNeo - Arcade Games','zxspectrum':'Sinclair - ZX Spectrum','amstradcpc':'Amstrad - CPC',
 '3do':'The 3DO Company - 3DO','amiga1200':'Commodore - Amiga','amiga600':'Commodore - Amiga','amigacd32':'Commodore - CD32',
 'atari800':'Atari - 8-bit Family','atarixe':'Atari - 8-bit Family','snesna':'Nintendo - Super Nintendo Entertainment System','sega32xjp':'Sega - 32X','sega32xna':'Sega - 32X',
 'msx1':'Microsoft - MSX','mark3':'Sega - Master System - Mark III','gameandwatch':'Handheld Electronic Game','lcdgames':'Handheld Electronic Game','tic80':'TIC-80','videopac':'Magnavox - Odyssey2',
 'arduboy':'Arduboy Inc - Arduboy','atomiswave':'Atomiswave','naomi':'Sega - Naomi','naomigd':'Sega - Naomi','naomi2':'Sega - Naomi 2','wasm4':'WASM-4','lowresnx':'LowRes NX','vircon32':'Vircon32',
 'pc88':'NEC - PC-88','pc98':'NEC - PC-98','gamecom':'Tiger - Game.com','chailove':'ChaiLove','lutro':'Lutro','x1':'Sharp - X1','fba':'FBNeo - Arcade Games','cps':'FBNeo - Arcade Games','ps4':'Sony - PlayStation 4','x68000':'Sharp - X68000','odyssey2':'Magnavox - Odyssey2','channelf':'Fairchild - Channel F',
}
reglas={}
for e in reg.findall('emulator'):
    n=e.get('name'); ents=[]
    for r in e.findall('rule'):
        if r.get('type')=='androidpackage':
            ents+= [x.text.strip() for x in r.findall('entry') if x.text]
    if ents: reglas[n]=ents
# Emuladores de Switch que ES-DE todavía no trae (misma forma de abrir que Eden).
EXTRA = {
    'CITRON': ['org.citron.citron_emu/org.citron.citron_emu.activities.EmulationActivity'],
    'SUDACHI': ['org.sudachi.sudachi_emu/org.sudachi.sudachi_emu.activities.EmulationActivity'],
    'YUZU': ['org.yuzu.yuzu_emu/org.yuzu.yuzu_emu.activities.EmulationActivity'],
    # Essentia: fan games de RPG Maker XP/VX/VX Ace (Pokémon Essentials). Abre los accesos .essentia.
    'ESSENTIA': ['io.github.nikalbazu24.essentia/com.hatkid.mkxpz.LauncherActivity'],
}
for k, v in EXTRA.items():
    reglas.setdefault(k, v)
out=[]; sinlib=[]
for s in sis.findall('system'):
    name=s.findtext('name'); full=s.findtext('fullname')
    ext=sorted(set(x.lower().lstrip('.') for x in (s.findtext('extension') or '').split() if x.strip()))
    cmds=[[c.get('label') or '', c.text.strip()] for c in s.findall('command')]
    if name == 'switch':
        for k in EXTRA:
            cmds.append([k.capitalize() + ' (Standalone)', '%EMULATOR_' + k + '% %ACTION%=android.nfc.action.TECH_DISCOVERED %DATA%=%ROMPROVIDER%'])
    l=manual.get(name) or libn.get(norm(full))
    if l and l not in lib and l not in ('MAME','DOS','ScummVM'):
        l = l if l in lib else None
    if l is None: sinlib.append(name)
    icono = l if (l and l in icon_names) else (full if full in icon_names else None)
    out.append({'id':name,'nombre':full,'ext':ext,'cmds':cmds,'libretro':l,'plataforma':s.findtext('platform'),'icono':icono})
# Sistemas que ES-DE no trae
out.append({'id':'rpgmaker','nombre':'RPG Maker XP / VX / VX Ace','ext':['essentia'],
            'cmds':[['Essentia','%EMULATOR_ESSENTIA% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%']],
            'libretro':None,'plataforma':'pc','icono':None})
# emuladores usados pero sin regla
usados=set(re.findall(r'%EMULATOR_([A-Z0-9_+\-]+)%',open(base+'es_systems.xml').read()))
falta=[u for u in usados if u not in reglas]
json.dump({'reglas':reglas,'sistemas':out},open(sys.argv[1],'w'),ensure_ascii=False,separators=(',',':'))
print(len(out),'sistemas',len(reglas),'reglas; sin regla:',falta)
print('sin libretro:',len(sinlib)); print([x for x in sinlib][:200])
print('iconos:',sum(1 for o in out if o['icono']))
