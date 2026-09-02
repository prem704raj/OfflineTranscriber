#!/usr/bin/env python3

import argparse
import re
import sys
import zipfile
from pathlib import Path

FORBIDDEN_NAMES = [
    re.compile(r"(^|/).*\.dmp$", re.I),
    re.compile(r"(^|/)test[_-]?fixtures?/", re.I),
    re.compile(r"(^|/)sample[_-]?media/", re.I),
    re.compile(r"(^|/)ggml-(tiny|base|small|medium|large).*\.bin$", re.I),
    re.compile(r"(^|/).*speaker.*\.onnx$", re.I),
]

FORBIDDEN_TEXT = [
    b"PIN_REAL_SHA256",
    b"REPLACE_ME",
    b"example.com",
    b"ALLOW_DEBUG_PRO_OVERRIDE=true",
]

TEXT_EXTENSIONS = {
    ".xml", ".json", ".txt", ".properties",
    ".html", ".js", ".md", ".cfg", ".conf"
}

def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)

def main() -> None:
    parser = argparse.ArgumentParser(description="Static verification for release artifacts")
    parser.add_argument("artifact", help="Path to release APK or AAB file")
    args = parser.parse_args()

    artifact = Path(args.artifact)
    if not artifact.is_file():
        fail(f"Artifact missing: {artifact}")

    with zipfile.ZipFile(artifact) as z:
        names = z.namelist()

        for name in names:
            for pattern in FORBIDDEN_NAMES:
                if pattern.search(name):
                    fail(f"Forbidden packaged file in release artifact: {name}")

        for info in z.infolist():
            suffix = Path(info.filename).suffix.lower()
            if suffix not in TEXT_EXTENSIONS:
                continue

            if info.file_size > 5 * 1024 * 1024:
                continue

            data = z.read(info)
            for token in FORBIDDEN_TEXT:
                if token in data:
                    fail(f"Forbidden release token {token!r} found in {info.filename}")

    print("PASS: release artifact static gate")

if __name__ == "__main__":
    main()
