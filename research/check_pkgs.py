import urllib.request
import json
import re
import sys

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept-Language': 'en-US,en;q=0.9'
}

pkgs = [
    'com.splendapps.voicerecorder',
    'com.media.audiorecorder.voicerecorder',
    'com.recorder.voice',
    'com.appmind.radios.voicerecorder',
    'com.voicerecorder.audiorecorder',
    'com.speechlogger'
]

for p in pkgs:
    try:
        url = f'https://play.google.com/store/apps/details?id={p}&hl=en&gl=us'
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            html = r.read().decode('utf-8', errors='ignore')
            title_m = re.search(r'<title>([^<]+)</title>', html)
            title = title_m.group(1).split(' - Apps')[0] if title_m else p
            rate_m = re.search(r'Rated\s+([0-9\.]+)\s+stars out of five', html)
            rate = rate_m.group(1) if rate_m else '?'
            inst_m = re.search(r'class="ClM7O">([^<]+)</div><div class="g1rdde">Downloads</div>', html)
            inst = inst_m.group(1) if inst_m else '?'
            rev_m = re.search(r'([0-9\.]+[KMBkmb]?)\s+reviews', html)
            rev = rev_m.group(1) if rev_m else '?'
            print(f"[FOUND] {p} | {title} | {rate}* ({rev} rev) | {inst} dl")
    except Exception as e:
        print(f"[FAIL] {p}: {e}")
