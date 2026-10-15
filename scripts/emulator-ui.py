#!/usr/bin/env python3
"""Small adb UI helper: always locate controls from a fresh hierarchy, avoiding stale coordinates."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
class Ui:
    def __init__(self, serial): self.serial = serial
    def adb(self, *args): return subprocess.check_output(['adb','-s',self.serial,*args],timeout=25)
    def dump(self, name='redesign-current'):
        self.adb('shell','rm','-f','/sdcard/clock-ui.xml')
        result=self.adb('shell','uiautomator','dump','/sdcard/clock-ui.xml')
        if b'ERROR:' in result: raise RuntimeError(result.decode())
        raw=self.adb('exec-out','cat','/sdcard/clock-ui.xml')
        (ROOT/'analysis'/f'{name}.xml').write_bytes(raw)
        return list(ET.fromstring(raw).iter('node'))
    def tap(self, label, attr='text'):
        nodes=self.dump()
        matches=[n for n in nodes if n.get(attr)==label]
        if not matches: raise RuntimeError(f'No visible {attr}={label!r}')
        node=next((n for n in matches if n.get('clickable')=='true'), matches[0])
        x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
        self.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.7)
    def screenshot(self,name): (ROOT/'docs'/'redesign'/f'{name}.png').write_bytes(self.adb('exec-out','screencap','-p'))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--serial',required=True);parser.add_argument('action',choices=['tap','desc','dump','shot']);parser.add_argument('value',nargs='?',default='redesign-current');args=parser.parse_args();ui=Ui(args.serial)
    if args.action in ('tap','desc'): ui.tap(args.value,'content-desc' if args.action=='desc' else 'text')
    elif args.action=='shot': ui.screenshot(args.value)
    else:
        for n in ui.dump(args.value):
            if n.get('text') or n.get('content-desc'): print(n.get('text'),n.get('content-desc'),n.get('bounds'),n.get('class'))
