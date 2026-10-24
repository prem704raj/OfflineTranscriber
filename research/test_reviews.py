import urllib.request
import json

apps = {
    'LoroNote': '6749249346',
    'Otter': '1271197915',
    'Notta': '1480649572',
    'Transkriptor': '1604111368',
    'EasyVoiceRecorder': '1243127457'
}

for name, app_id in apps.items():
    url = f'https://itunes.apple.com/us/rss/customerreviews/page=1/id={app_id}/sortBy=mostRecent/json'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode('utf-8'))
            entries = data.get('feed', {}).get('entry', [])
            reviews = [e for e in entries if 'im:rating' in e]
            print(f"{name} ({app_id}): Fetched {len(reviews)} real reviews from page 1")
            if reviews:
                first = reviews[0]
                rating = first.get('im:rating', {}).get('label')
                title = first.get('title', {}).get('label')
                print(f"   Sample: {rating}* - {title}")
    except Exception as e:
        print(f"{name} ({app_id}): Error {e}")
