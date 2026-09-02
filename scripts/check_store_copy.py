#!/usr/bin/env python3

from pathlib import Path
import sys

LIMITS = {
    "app_name.txt": 30,
    "short_description.txt": 80,
    "full_description.txt": 4000,
}

def main() -> None:
    base = Path("play/store-listing/en-US")

    failed = False

    for file_name, limit in LIMITS.items():
        path = base / file_name
        if not path.is_file():
            print(f"FAIL missing {path}")
            failed = True
            continue

        text = path.read_text(encoding="utf-8").strip()
        length = len(text)
        status = "PASS" if length <= limit else "FAIL"

        print(f"{status} {file_name}: {length}/{limit}")

        if length > limit:
            failed = True

    if failed:
        sys.exit(1)

if __name__ == "__main__":
    main()
