#!/usr/bin/env python3
"""Build one mod JAR against an installed AsyncYouer server, without bundling its dependencies."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-root',type=Path,required=True)
    args=parser.parse_args()
    root=Path(__file__).resolve().parent
    libs=args.server_root.resolve()/'libraries'
    core=libs/'com/mohistmc/installation/data/paper-remap.jar'
    if not core.is_file(): parser.error(f'Missing AsyncYouer compile dependency: {core}')
    for tool in ('javac','jar'):
        if not shutil.which(tool): parser.error(f'{tool} not found; install JDK 21 or newer')
    version=re.search(r'^version="([^"]+)"',(root/'src/main/resources/META-INF/neoforge.mods.toml').read_text(),re.M).group(1)
    dest=root/'build/classes'
    shutil.rmtree(root/'build',ignore_errors=True)
    shutil.copytree(root/'src/main/resources',dest)
    jars=[core]+sorted(p for p in libs.rglob('*.jar') if p!=core)
    sources=sorted(p for p in (root/'src/main/java').rglob('*.java') if not p.name.startswith('._'))
    cp=':'.join(str(p) for p in jars)
    subprocess.run(['javac','--release','21','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(dest),*map(str,sources)],check=True)
    out=root/'dist';out.mkdir(exist_ok=True)
    artifact=out/f'mctg-terrainlock-{version}.jar'
    subprocess.run(['jar','--create','--file',str(artifact),'-C',str(dest),'.'],check=True)
    with zipfile.ZipFile(artifact) as jar:
        assert 'plugin.yml' not in jar.namelist()
        assert not any(n.startswith(('net/minecraft/','org/bukkit/')) for n in jar.namelist())
    digest=hashlib.sha256(artifact.read_bytes()).hexdigest()
    (out/'SHA256SUMS').write_text(f'{digest}  {artifact.name}\n')
    (out/'build-info.json').write_text(json.dumps({'version':version,'artifact':artifact.name,'sha256':digest,'compile_core_sha256':hashlib.sha256(core.read_bytes()).hexdigest(),'java_release':21},indent=2)+'\n')
    print(f'Built single mod: {artifact.name}\nSHA-256: {digest}')

if __name__=='__main__':
    try: main()
    except subprocess.CalledProcessError as error: sys.exit(error.returncode)
