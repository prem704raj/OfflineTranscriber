import urllib.request
import json
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Accept-Language': 'en-US,en;q=0.9'
}

apps = [
    ('com.aisense.otter', 'Otter.ai: Transcribe Voice Notes'),
    ('com.langogo.transcribe', 'Notta - Transcribe Audio to Text'),
    ('com.transkriptor.app', 'Transkriptor: Speech to Text'),
    ('com.coffeebeanventures.easyvoicerecorder', 'Easy Voice Recorder'),
    ('com.google.audio.hearing.visualization.accessibility.scribe', 'Live Transcribe & Notification'),
    ('ru.vsms', 'Write by Voice: Speech to Text')
]

results = []

for pkg, default_title in apps:
    url = f'https://play.google.com/store/apps/details?id={pkg}&hl=en&gl=us'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            
            title = default_title
            developer = 'UNVERIFIED'
            rating = 'UNVERIFIED'
            rating_count = 'UNVERIFIED'
            installs = 'UNVERIFIED'
            price = 'Free'
            iap = 'In-app purchases' in html
            
            title_m = re.search(r'<h1[^>]*itemprop="name"[^>]*>([^<]+)</h1>', html)
            if title_m:
                title = title_m.group(1).strip()
            elif '<title>' in html:
                t_m = re.search(r'<title>([^<]+)</title>', html)
                if t_m:
                    title = t_m.group(1).split(' - Apps on Google Play')[0].strip()
            
            rate_m = re.search(r'Rated\s+([0-9\.]+)\s+stars out of five', html, re.I)
            if not rate_m:
                rate_m = re.search(r'([0-9\.]+)\s+star(?:s)?', html, re.I)
            if rate_m:
                rating = rate_m.group(1).strip()
            
            rev_m = re.search(r'([0-9\.]+[KMBkmb]?)\s+reviews', html)
            if rev_m:
                rating_count = rev_m.group(1).strip()
            
            inst_m = re.search(r'class="ClM7O">([^<]+)</div><div class="g1rdde">Downloads</div>', html)
            if not inst_m:
                inst_m = re.search(r'([0-9\+\,MK]+)\s+Downloads', html, re.I)
            if inst_m:
                installs = inst_m.group(1).strip()
                
            dev_m = re.search(r'<div class="Vbfug">([^<]+)</div>', html)
            if not dev_m:
                dev_m = re.search(r'/store/apps/dev\?id=[^"]*"[^>]*><span>([^<]+)</span>', html)
            if dev_m:
                developer = dev_m.group(1).strip()

            updated = 'UNVERIFIED'
            up_m = re.search(r'Updated on</div><div class="xg1aae">([^<]+)</div>', html)
            if up_m:
                updated = up_m.group(1).strip()
                
            item = {
                'package': pkg,
                'title': title,
                'developer': developer,
                'rating': rating,
                'rating_count': rating_count,
                'installs': installs,
                'price': price,
                'has_iap': iap,
                'updated': updated
            }
            results.append(item)
            print(f"[OK] {pkg} -> {title} | Dev: {developer} | Rating: {rating} ({rating_count}) | Installs: {installs} | IAP: {iap}")
    except Exception as ex:
        print(f"[ERR] {pkg} -> {ex}")

with open('research/competitors_scraped.json', 'w', encoding='utf-8') as f:
    json.dump(results, f, indent=2)
print("Finished scraping.")
