#!/usr/bin/env python3
"""Tests for stamp_web_dist.py. Run: python tools/test_stamp_web_dist.py"""

from __future__ import annotations

import hashlib
import json
import re
import shutil
import tempfile
import unittest
from pathlib import Path

import stamp_web_dist as stamp

RESOURCES = Path(__file__).resolve().parent.parent / "web" / "src" / "wasmJsMain" / "resources"


class StampWebDistTest(unittest.TestCase):
    def setUp(self) -> None:
        self.dist = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.dist)
        for name in ("sw.js", "_headers", "index.html", "404.html"):
            shutil.copy(RESOURCES / name, self.dist / name)
        (self.dist / "ember.js").write_text("// app\n", encoding="utf-8")
        (self.dist / "ember.wasm").write_bytes(b"\0asm-app")
        (self.dist / "dd568dbcd078c0adf7cf.wasm").write_bytes(b"\0asm-skiko")
        (self.dist / "fonts").mkdir(parents=True)
        (self.dist / "fonts" / "barlow_regular.ttf").write_bytes(b"font")

    def _precache(self) -> list[list[str | None]]:
        text = (self.dist / "sw.js").read_text(encoding="utf-8")
        match = re.search(r"const PRECACHE = (\[.*?\n\]);", text, re.DOTALL)
        self.assertIsNotNone(match)
        return json.loads(match.group(1))

    def test_stamps_build_and_file_hashes_into_the_service_worker(self) -> None:
        build = stamp.stamp(self.dist)

        text = (self.dist / "sw.js").read_text(encoding="utf-8")
        self.assertIn(f"const BUILD = '{build}';", text)
        entries = dict((url, sha) for url, sha in self._precache())
        self.assertIsNone(entries["./"])
        self.assertEqual(entries["fonts/barlow_regular.ttf"], hashlib.sha256(b"font").hexdigest())
        self.assertIn("ember.wasm", entries)
        for skipped in ("sw.js", "_headers", "404.html", "index.html"):
            self.assertNotIn(skipped, entries)

    def test_marks_only_content_hashed_files_immutable(self) -> None:
        stamp.stamp(self.dist)

        headers = (self.dist / "_headers").read_text(encoding="utf-8")
        self.assertIn("/dd568dbcd078c0adf7cf.wasm\n  Cache-Control: public, max-age=31536000, immutable", headers)
        self.assertNotIn("/ember.wasm\n", headers)

    def test_build_id_follows_content(self) -> None:
        first = stamp.build_id(self.dist)
        self.assertEqual(first, stamp.build_id(self.dist))
        (self.dist / "ember.wasm").write_bytes(b"\0asm-app-2")
        self.assertNotEqual(first, stamp.build_id(self.dist))

    def test_refuses_to_stamp_twice(self) -> None:
        stamp.stamp(self.dist)
        with self.assertRaises(stamp.StampError):
            stamp.stamp(self.dist)

    def test_rejects_a_file_over_the_pages_size_limit(self) -> None:
        with (self.dist / "huge.bin").open("wb") as handle:
            handle.truncate(stamp.MAX_FILE_BYTES + 1)
        before = (self.dist / "sw.js").read_text(encoding="utf-8")
        with self.assertRaises(stamp.StampError):
            stamp.stamp(self.dist)
        self.assertEqual(before, (self.dist / "sw.js").read_text(encoding="utf-8"))

    def test_dev_mode_skips_only_the_size_check(self) -> None:
        with (self.dist / "huge.bin").open("wb") as handle:
            handle.truncate(stamp.MAX_FILE_BYTES + 1)
        stamp.stamp(self.dist, check_sizes=False)
        self.assertNotIn("__EMBER_BUILD__", (self.dist / "sw.js").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
