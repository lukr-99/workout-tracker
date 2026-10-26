#!/usr/bin/env python3
"""Prepare a built web distribution for Cloudflare Pages.

Run after `./gradlew :web:wasmJsBrowserDistribution`:

    python tools/stamp_web_dist.py web/build/dist/wasmJs/productionExecutable

It writes the build id and the list of files (with SHA-256) into sw.js, appends long-lived cache
rules for content-hashed files to _headers, and checks the Cloudflare Pages limits. It refuses to
stamp a folder twice. See docs/web-edition.md.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Sequence

# Cloudflare Pages limits (free plan): https://developers.cloudflare.com/pages/platform/limits/
MAX_FILES = 20_000
MAX_FILE_BYTES = 25 * 1024 * 1024
MAX_HEADER_RULES = 100
MAX_HEADER_LINE = 2_000

BUILD_PLACEHOLDER = "'__EMBER_BUILD__'"
PRECACHE_PLACEHOLDER = "/* __EMBER_PRECACHE__ */ []"
HEADERS_MARKER = "# Content-hashed files, appended by tools/stamp_web_dist.py:"
IMMUTABLE = "public, max-age=31536000, immutable"

# Webpack names emitted assets by content hash (e.g. "dd568dbcd078c0adf7cf.wasm").
HASHED_NAME = re.compile(r"^[0-9a-f]{16,}\.[a-z0-9]+$")
# Served by Pages or the browser itself, never fetched by the app.
NOT_PRECACHED = {"_headers", "_redirects", "_routes.json", "sw.js", "404.html"}
# The app's HTML is checked with the page, not by hash: Cloudflare may rewrite HTML in transit.
UNHASHED_IN_SW = {"index.html"}


class StampError(Exception):
    """The distribution cannot be stamped or would not deploy."""


def _files(dist: Path) -> list[Path]:
    return sorted(path for path in dist.rglob("*") if path.is_file())


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check_limits(dist: Path, check_sizes: bool = True) -> None:
    files = _files(dist)
    if len(files) > MAX_FILES:
        raise StampError(f"{len(files)} files; Cloudflare Pages allows {MAX_FILES}.")
    for path in files if check_sizes else []:
        size = path.stat().st_size
        if size > MAX_FILE_BYTES:
            relative = path.relative_to(dist).as_posix()
            raise StampError(f"{relative} is {size} bytes; Cloudflare Pages allows 25 MiB per file.")


def precache_entries(dist: Path) -> list[tuple[str, str | None]]:
    entries: list[tuple[str, str | None]] = []
    for path in _files(dist):
        relative = path.relative_to(dist).as_posix()
        if relative in NOT_PRECACHED or relative.endswith(".map") or path.name.startswith("."):
            continue
        if relative in UNHASHED_IN_SW:
            entries.append(("./" if relative == "index.html" else relative, None))
        else:
            entries.append((relative, _sha256(path)))
    if not any(url == "./" for url, _ in entries):
        raise StampError("index.html is missing; is this a web distribution folder?")
    return entries


def build_id(dist: Path) -> str:
    digest = hashlib.sha256()
    for path in _files(dist):
        relative = path.relative_to(dist).as_posix()
        digest.update(f"{relative}\0{_sha256(path)}\n".encode("utf-8"))
    return digest.hexdigest()[:16]


def stamp_service_worker(dist: Path, build: str, entries: list[tuple[str, str | None]]) -> None:
    sw = dist / "sw.js"
    text = sw.read_text(encoding="utf-8")
    if BUILD_PLACEHOLDER not in text or PRECACHE_PLACEHOLDER not in text:
        raise StampError("sw.js has no placeholders; it is missing or already stamped.")
    precache = "[\n" + ",\n".join(f"  {json.dumps(list(entry))}" for entry in entries) + "\n]"
    text = text.replace(BUILD_PLACEHOLDER, f"'{build}'").replace(PRECACHE_PLACEHOLDER, precache)
    sw.write_text(text, encoding="utf-8")


def stamp_headers(dist: Path) -> list[str]:
    headers = dist / "_headers"
    text = headers.read_text(encoding="utf-8")
    if not text.rstrip().endswith(HEADERS_MARKER):
        raise StampError("_headers does not end with the stamp marker; it is already stamped.")
    hashed = [
        path.relative_to(dist).as_posix()
        for path in _files(dist)
        if HASHED_NAME.match(path.name)
    ]
    rules = "".join(f"/{name}\n  Cache-Control: {IMMUTABLE}\n\n" for name in hashed)
    text = text.rstrip() + "\n\n" + rules
    rule_count = sum(1 for line in text.splitlines() if line.startswith("/"))
    if rule_count > MAX_HEADER_RULES:
        raise StampError(f"_headers has {rule_count} rules; Cloudflare Pages allows {MAX_HEADER_RULES}.")
    for number, line in enumerate(text.splitlines(), start=1):
        if len(line) > MAX_HEADER_LINE:
            raise StampError(f"_headers line {number} is longer than {MAX_HEADER_LINE} characters.")
    headers.write_text(text.rstrip() + "\n", encoding="utf-8")
    return hashed


def stamp(dist: Path, check_sizes: bool = True) -> str:
    if not dist.is_dir():
        raise StampError(f"{dist} is not a folder.")
    check_limits(dist, check_sizes)
    build = build_id(dist)
    entries = precache_entries(dist)
    # Check both files before changing either, so a failure leaves the folder untouched.
    for name, markers in (
        ("sw.js", (BUILD_PLACEHOLDER, PRECACHE_PLACEHOLDER)),
        ("_headers", (HEADERS_MARKER,)),
    ):
        path = dist / name
        if not path.is_file() or not all(m in path.read_text(encoding="utf-8") for m in markers):
            raise StampError(f"{name} is missing or already stamped.")
    hashed = stamp_headers(dist)
    stamp_service_worker(dist, build, entries)
    total = sum(path.stat().st_size for path in _files(dist))
    print(
        f"Stamped build {build}: {len(entries)} files precached, {len(hashed)} immutable, "
        f"{total / (1024 * 1024):.1f} MiB in total."
    )
    return build


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dist", type=Path, help="the web distribution folder")
    parser.add_argument(
        "--dev",
        action="store_true",
        help="skip the 25 MiB file size check: an unoptimized development build is larger and is "
        "only for local checks, never for deploying",
    )
    args = parser.parse_args(argv)
    try:
        stamp(args.dist, check_sizes=not args.dev)
    except StampError as error:
        print(f"stamp_web_dist: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
