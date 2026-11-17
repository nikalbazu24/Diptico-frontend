import numpy as np, cv2, sys
from PIL import Image
def cargar(f): return np.array(Image.open(f).convert('RGB')).astype(np.float32)
def guardar(a,f):
    im=Image.fromarray(np.clip(a,0,255).astype(np.uint8))
    if f.endswith('.jpg'): im.save(f,quality=92)
    else: im.save(f)
def comp(mask, cerca=None):
    m=(mask*255).astype(np.uint8); m=cv2.morphologyEx(m,cv2.MORPH_CLOSE,np.ones((5,5),np.uint8))
    n,lab,st,cen=cv2.connectedComponentsWithStats(m)
    orden=[i for i in np.argsort(-st[:,4]) if i!=0]
    return st[orden[0]] if orden else None
def pantalla_apagada(a,x0,y0,x1,y1,base,brillo=18):
    """Rellena un rectángulo con una pantalla apagada: color base con un reflejo diagonal suave."""
    h=y1-y0; w=x1-x0
    yy,xx=np.mgrid[0:h,0:w]
    t=(xx/w*0.6+ (1-yy/h)*0.4)
    ref=np.clip(1-abs(t-0.62)*5,0,1)*brillo
    c=np.array(base,np.float32)[None,None,:]+ref[...,None]
    a[y0:y1,x0:x1]=c
def sat(a): return a.max(-1)-a.min(-1)
def lum(a): return a.mean(-1)
