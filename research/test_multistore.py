import urllib.request
import json
import time

storefronts = ['us', 'gb', 'ca', 'au', 'in']
app_id = '6749249346' # LoroNote

all_loronote_reviews = []

for sf in storefronts:
    for page in range(1, 3):
        url = f"https://itunes.apple.com/{sf}/rss/customerreviews/page={page}/id={app_id}/sortBy=mostRecent/json"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=8) as r:
                data = json.loads(r.read().decode('utf-8'))
                entries = data.get('feed', {}).get('entry', [])
                revs = [e for e in entries if 'im:rating' in e]
                for rev in revs:
                    all_loronote_reviews.append({
                        'country': sf,
                        'rating': rev.get('im:rating', {}).get('label'),
                        'title': rev.get('title', {}).get('label'),
                        'content': rev.get('content', {}).get('label'),
                        'version': rev.get('im:version', {}).get('label'),
                        'author': rev.get('author', {}).get('name', {}).get('label')
                    })
        except Exception as e:
            pass
        time.sleep(0.3)

print(f"Total LoroNote reviews collected across storefronts: {len(all_loronote_reviews)}")
for r in all_loronote_reviews:
    print(f"[{r['country']}] {r['rating']}* | {r['title']} | {r['content'][:80]}...")
