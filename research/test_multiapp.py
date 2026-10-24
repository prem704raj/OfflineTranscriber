import urllib.request
import json
import time

storefronts = ['us', 'gb', 'ca', 'au', 'in']
targets = {
    'Notta': '1480649572',
    'Transkriptor': '1604111368',
    'EasyVoiceRecorder': '1243127457',
    'Otter': '1271197915'
}

for name, app_id in targets.items():
    revs_count = 0
    samples = []
    for sf in storefronts:
        for page in range(1, 4):
            url = f"https://itunes.apple.com/{sf}/rss/customerreviews/page={page}/id={app_id}/json"
            try:
                req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
                with urllib.request.urlopen(req, timeout=5) as r:
                    data = json.loads(r.read().decode('utf-8'))
                    entries = data.get('feed', {}).get('entry', [])
                    revs = [e for e in entries if 'im:rating' in e]
                    revs_count += len(revs)
                    for rev in revs[:2]:
                        samples.append((
                            sf,
                            rev.get('im:rating', {}).get('label'),
                            rev.get('title', {}).get('label'),
                            rev.get('content', {}).get('label', '')[:100]
                        ))
            except Exception:
                pass
            time.sleep(0.2)
    print(f"Target {name}: {revs_count} reviews collected across storefronts. Samples: {len(samples)}")
    for s in samples[:3]:
        print(f"  [{s[0]}] {s[1]}* | {s[2]} | {s[3]}...")
