import re

with open(r'C:\Users\prem7\.gemini\antigravity\brain\3b95c72f-187a-4a44-a775-ff9264cb6630\.system_generated\steps\80\content.md', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Pattern for review text in Play Store
reviews = re.findall(r'<div class="h3YV2d">(.*?)</div>', text)
print(f"Found {len(reviews)} reviews via h3YV2d class")
for r in reviews[:10]:
    print("REVIEW:", r)

# Author names: <span class="X43Kjb">Author</span>
authors = re.findall(r'<span class="X43Kjb">([^<]+)</span>', text)
print(f"Found {len(authors)} authors: {authors[:5]}")

# Dates: <span class="bp9Aid">Date</span>
dates = re.findall(r'<span class="bp9Aid">([^<]+)</span>', text)
print(f"Found {len(dates)} dates: {dates[:5]}")
