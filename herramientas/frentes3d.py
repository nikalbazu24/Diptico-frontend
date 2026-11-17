import asyncio, os, subprocess, glob
from playwright.async_api import async_playwright
from PIL import Image, ImageDraw
ids=[os.path.basename(f)[:-4] for f in sorted(glob.glob('h/*.glb'))]
hechos={f.split('_')[0] for f in os.listdir('fr4')} if os.path.isdir('fr4') else set()
todos=ids
ids=[i for i in ids if not os.path.exists(f'fr4/{i}_3.png')]
PAGE='''<html><body style="margin:0;background:#20222f">
<script type="module" src="/node_modules/@google/model-viewer/dist/model-viewer.min.js"></script>
<model-viewer id="m" src="/h/%s.glb" style="width:200px;height:150px" exposure="1.05" interaction-prompt="none" camera-orbit="0deg 80deg auto" field-of-view="26deg" environment-image="neutral"></model-viewer></body></html>'''
async def main():
    srv=subprocess.Popen(['python3','-m','http.server','8767'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    await asyncio.sleep(1); os.makedirs('fr4',exist_ok=True)
    async with async_playwright() as p:
        b=await p.chromium.launch(executable_path='/usr/bin/google-chrome',args=['--no-sandbox','--use-gl=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist'])
        for id in ids:
            open(f'q_{id}.html','w').write(PAGE%id)
            pg=await b.new_page(viewport={'width':200,'height':150})
            await pg.goto(f'http://localhost:8767/q_{id}.html')
            try: await pg.wait_for_function("document.getElementById('m').loaded===true",timeout=90000)
            except Exception: print(id,'no'); continue
            for k,a in enumerate([0,90,180,270]):
                await pg.evaluate(f"(()=>{{const m=document.getElementById('m');m.cameraOrbit='{a}deg 80deg auto';m.jumpCameraToGoal();}})()")
                await pg.wait_for_timeout(700)
                await pg.screenshot(path=f'fr4/{id}_{k}.png')
            await pg.close()
        await b.close()
    srv.terminate()
    s=Image.new('RGB',(200*4,170*len(todos)),(0,0,0)); d=ImageDraw.Draw(s)
    for i,id in enumerate(todos):
        for k in range(4):
            f=f'fr4/{id}_{k}.png'
            if os.path.exists(f): s.paste(Image.open(f),(k*200,i*170))
        d.text((4,i*170+152),id+'   0 | 90 | 180 | 270',fill=(255,255,255))
    s.save('fr4/todo.png')
asyncio.run(main())
