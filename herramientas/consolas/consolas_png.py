import asyncio, os, subprocess, glob, sys
from playwright.async_api import async_playwright
from PIL import Image
fr={"gba":-90,"n64":90,"ps3":90,"switch":180,"wii":-90}
ids=[os.path.basename(f)[:-4] for f in sorted(glob.glob('h/*.glb'))]
if len(sys.argv)>1: ids=sys.argv[1:]
PAGE='''<html><body style="margin:0;background:transparent">
<script type="module" src="/node_modules/@google/model-viewer/dist/model-viewer.min.js"></script>
<model-viewer id="m" src="/h/%s.glb" style="width:1200px;height:900px;background:transparent" exposure="1.1" shadow-intensity="0" interaction-prompt="none" camera-orbit="%sdeg 72deg auto" field-of-view="24deg" environment-image="neutral" tone-mapping="commerce"></model-viewer></body></html>'''
async def main():
    srv=subprocess.Popen(['python3','-m','http.server','8768'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    await asyncio.sleep(1); os.makedirs('png',exist_ok=True)
    async with async_playwright() as p:
        b=await p.chromium.launch(executable_path='/usr/bin/google-chrome',args=['--no-sandbox','--use-gl=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist'])
        for id in ids:
            th=-(fr.get(id,0)+25)
            open(f'r_{id}.html','w').write(PAGE%(id,th))
            pg=await b.new_page(viewport={'width':1200,'height':900})
            await pg.goto(f'http://localhost:8768/r_{id}.html')
            try: await pg.wait_for_function("document.getElementById('m').loaded===true",timeout=120000)
            except Exception: print(id,'no'); continue
            await pg.wait_for_timeout(1500)
            await pg.screenshot(path=f'png/{id}_raw.png',omit_background=True)
            im=Image.open(f'png/{id}_raw.png').convert('RGBA'); bb=im.getbbox(); im=im.crop(bb)
            im.thumbnail((900,700),Image.LANCZOS); im.save(f'png/{id}.webp','WEBP',quality=88,method=6)
            print(id,im.size,os.path.getsize(f'png/{id}.webp'))
            await pg.close()
        await b.close()
    srv.terminate()
asyncio.run(main())
