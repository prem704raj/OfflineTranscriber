#!/usr/bin/env python3

import re
import sys
from pathlib import Path

ROOT = Path(".")

INCLUDED = {
    ".kt", ".kts", ".java", ".cpp",
    ".c", ".h", ".hpp", ".xml",
    ".gradle", ".properties", ".json"
}

SKIP_PARTS = {
    ".git",
    ".gradle",
    "build",
    ".idea",
    ".gemini",
    "node_modules",
}

FORBIDDEN = {
    "PIN_REAL_SHA256": "unresolved checksum placeholder",
    "REPLACE_ME": "unresolved placeholder",
    "YOUR_API_KEY": "API-key placeholder",
    "fallbackToDestructiveMigration": "destructive Room migration",
    "GlobalScope.": "unstructured production coroutine",
}

# Suspicious patterns that must not appear in production sources outside allowed schemas/licenses
URL_PATTERN = re.compile(r'http://[a-zA-Z0-9]', re.I)
ALLOWED_HTTP = [
    "http://schemas.android.com",
    "http://www.w3.org",
    "http://apache.org",
    "http://www.apache.org",
    "http://schemas.openxmlformats.org",
    "http://www.gradle.org"
]

def should_skip(path: Path) -> bool:
    return any(part in SKIP_PARTS for part in path.parts)

def main() -> None:
    failures = []

    for path in ROOT.rglob("*"):
        if not path.is_file() or should_skip(path) or path.suffix.lower() not in INCLUDED:
            continue

        # Skip test files and scripts
        if "test" in path.parts or "androidTest" in path.parts or "scripts" in path.parts:
            continue

        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        for token, reason in FORBIDDEN.items():
            if token in text:
                failures.append(f"{path}: {reason}: {token}")

        if "MANAGE_EXTERNAL_STORAGE" in text:
            failures.append(f"{path}: broad storage permission detected")

        if "ALLOW_DEBUG_PRO_OVERRIDE = true" in text:
            failures.append(f"{path}: debug Pro override hardcoded in production")

        for line_num, line in enumerate(text.splitlines(), start=1):
            stripped = line.strip()
            if "http://" in line:
                if stripped.startswith("#") or stripped.startswith("//") or stripped.startswith("*"):
                    continue
                is_allowed = any(allowed in line for allowed in ALLOWED_HTTP)
                if not is_allowed:
                    failures.append(f"{path}:{line_num}: cleartext HTTP found: {stripped}")

    if failures:
        print("\n".join(f"FAIL {item}" for item in failures))
        sys.exit(1)

    print("PASS: release source gate")

if __name__ == "__main__":
    main()
