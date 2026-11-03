#!/usr/bin/env python3
"""Reproducibly replace only Meteor build 15's broken Sodium fluid mixin (GPL-3.0-only)."""
import hashlib,json,sys,zipfile
from pathlib import Path

TARGET='meteordevelopment/meteorclient/mixin/sodium/SodiumDefaultFluidRendererMixin.class'
BASE_HASH='c808cfb65b1ef01f8d0cbf5059360359abf5252c8127f140a129fa03c4268480'
COMMIT='e3a2e77967629d216e127b5ceecf71fb15f9d2e5'

def patch(original,compiled,output):
    original,compiled,output=map(Path,(original,compiled,output))
    if original.resolve()==output.resolve(): raise ValueError('Input and output must be different files')
    source=Path(__file__).parent/'src'/Path(TARGET).with_suffix('.java')
    modified=compiled.read_bytes()
    if not modified.startswith(bytes.fromhex('cafebabe')): raise ValueError('Not a compiled Java class')
    with zipfile.ZipFile(original) as base:
        meta=json.loads(base.read('fabric.mod.json'))
        if meta['id']!='meteor-client' or meta['version']!='26.3-15': raise ValueError('This patch is for original Meteor Client 26.3 build 15 only')
        if hashlib.sha256(base.read(TARGET)).hexdigest()!=BASE_HASH: raise ValueError('Unexpected original mixin; refusing to overwrite an unknown/custom version')
        if any(n.upper().endswith(('.SF','.RSA','.DSA','.EC')) and n.upper().startswith('META-INF/') for n in base.namelist()): raise ValueError('Refusing to alter a signed archive')
        meta['name']='Meteor Client (Sodium Compat 1)'
        meta.setdefault('custom',{})['obs-overlay:sodium-compat']={'patch':1,'base_commit':COMMIT,'original_mixin_sha256':BASE_HASH,'source':'https://github.com/toaru4/obs-overlay-1.21.11/tree/port/minecraft-26.3/compat/meteor'}
        output.parent.mkdir(parents=True,exist_ok=True)
        with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED) as out:
            for info in base.infolist():
                data=modified if info.filename==TARGET else json.dumps(meta,indent=2,ensure_ascii=False).encode() if info.filename=='fabric.mod.json' else base.read(info.filename)
                out.writestr(info,data)
            out.writestr('META-INF/obs-sodium-compat/SodiumDefaultFluidRendererMixin.java',source.read_bytes())
            out.writestr('OBS_SODIUM_COMPAT_NOTICE.txt','Modified Meteor Client build 15; GPL-3.0-only. Fixes return-local lifetime and Sodium 0.9.3 fluid exposure descriptor. Xray logic retained. Base source commit: '+COMMIT+'\nChanged source is included in META-INF/obs-sodium-compat; complete upstream source: https://github.com/MeteorDevelopment/meteor-client/tree/'+COMMIT+'\n')
    with zipfile.ZipFile(original) as a,zipfile.ZipFile(output) as b:
        changed=[n for n in a.namelist() if a.read(n)!=b.read(n)]
        if sorted(changed)!=sorted([TARGET,'fabric.mod.json']): raise AssertionError('Unexpected archive changes: '+str(changed))
    print('METEOR_PATCH_PASS: exactly one class and provenance metadata changed; original license/nested dependencies retained')
    print(output.resolve())

if __name__=='__main__':
    if len(sys.argv)!=4: raise SystemExit('Usage: patch.py original-meteor.jar compiled-mixin.class output.jar')
    patch(*sys.argv[1:])
