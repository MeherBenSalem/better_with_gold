#!/usr/bin/env python3
"""
Publish JARs from build/release-jars/ to CurseForge.

Docs: https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api

Auth: CURSEFORGE_API_KEY env var -> X-Api-Token header (never printed).
Upload: POST {base}/api/projects/{projectId}/upload-file
        multipart fields: metadata (JSON), file (binary)
"""

from __future__ import annotations

import json
import mimetypes
import os
import sys
import uuid
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

PROJECT_ID = "1305455"
BASE_URL = "https://minecraft.curseforge.com"
UPLOAD_URL = f"{BASE_URL}/api/projects/{PROJECT_ID}/upload-file"
EXPECTED_JAR_COUNT = 7

# Default metadata values from the CurseForge Upload API article.
DEFAULT_CHANGELOG = "Initial CurseForge release."
DEFAULT_CHANGELOG_TYPE = "markdown"
DEFAULT_RELEASE_TYPE = "release"
DEFAULT_GAME_VERSION_NAMES = ["Server", "26.1.2"]
DEFAULT_MANUAL_RELEASE = False


def repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def release_jar_dir() -> Path:
    return repo_root() / "build" / "release-jars"


def require_api_key() -> str:
    key = os.environ.get("CURSEFORGE_API_KEY", "").strip()
    if not key:
        print("ERROR: CURSEFORGE_API_KEY is not set.", file=sys.stderr)
        print(
            "Set it to your CurseForge author API token from "
            "https://console.curseforge.com/ (API Tokens).",
            file=sys.stderr,
        )
        sys.exit(1)
    return key


def find_jars(directory: Path) -> list[Path]:
    jars = sorted(p for p in directory.glob("*.jar") if p.is_file())
    return jars


def redact(text: str, secret: str) -> str:
    if not text or not secret:
        return text
    return text.replace(secret, "[REDACTED]")


def build_metadata(jar: Path) -> dict[str, Any]:
    display_name = jar.stem
    return {
        "changelog": DEFAULT_CHANGELOG,
        "changelogType": DEFAULT_CHANGELOG_TYPE,
        "displayName": display_name,
        "gameVersionNames": list(DEFAULT_GAME_VERSION_NAMES),
        "releaseType": DEFAULT_RELEASE_TYPE,
        "isMarkedForManualRelease": DEFAULT_MANUAL_RELEASE,
    }


def encode_multipart(
    fields: dict[str, tuple[str | None, bytes, str | None]],
) -> tuple[bytes, str]:
    """
    Encode multipart/form-data without third-party deps.

    fields: name -> (filename_or_None, body_bytes, content_type_or_None)
    """
    boundary = f"----CurseForgeBoundary{uuid.uuid4().hex}"
    lines: list[bytes] = []

    for name, (filename, body, content_type) in fields.items():
        lines.append(f"--{boundary}\r\n".encode("utf-8"))
        if filename is None:
            disposition = f'Content-Disposition: form-data; name="{name}"\r\n'
            lines.append(disposition.encode("utf-8"))
            if content_type:
                lines.append(f"Content-Type: {content_type}\r\n".encode("utf-8"))
            lines.append(b"\r\n")
            lines.append(body)
            lines.append(b"\r\n")
        else:
            # Escape quotes in filename for the header.
            safe_name = filename.replace('"', '\\"')
            disposition = (
                f'Content-Disposition: form-data; name="{name}"; '
                f'filename="{safe_name}"\r\n'
            )
            lines.append(disposition.encode("utf-8"))
            ctype = content_type or "application/octet-stream"
            lines.append(f"Content-Type: {ctype}\r\n".encode("utf-8"))
            lines.append(b"\r\n")
            lines.append(body)
            lines.append(b"\r\n")

    lines.append(f"--{boundary}--\r\n".encode("utf-8"))
    payload = b"".join(lines)
    content_type_header = f"multipart/form-data; boundary={boundary}"
    return payload, content_type_header


def upload_file(api_key: str, jar: Path, metadata: dict[str, Any]) -> tuple[int, dict[str, Any] | str]:
    metadata_bytes = json.dumps(metadata, separators=(",", ":")).encode("utf-8")
    file_bytes = jar.read_bytes()
    mime = mimetypes.guess_type(jar.name)[0] or "application/java-archive"

    body, content_type = encode_multipart(
        {
            "metadata": (None, metadata_bytes, "application/json"),
            "file": (jar.name, file_bytes, mime),
        }
    )

    request = Request(
        UPLOAD_URL,
        data=body,
        method="POST",
        headers={
            "X-Api-Token": api_key,
            "Content-Type": content_type,
            "User-Agent": "better-with-gold-publish-script/1.0",
            "Accept": "application/json",
        },
    )

    try:
        with urlopen(request, timeout=120) as response:
            status = getattr(response, "status", None) or response.getcode()
            raw = response.read().decode("utf-8", errors="replace")
    except HTTPError as exc:
        status = exc.code
        raw = exc.read().decode("utf-8", errors="replace")
        raw = redact(raw, api_key)
        try:
            return status, json.loads(raw)
        except json.JSONDecodeError:
            return status, raw
    except URLError as exc:
        return 0, redact(str(exc.reason if hasattr(exc, "reason") else exc), api_key)

    raw = redact(raw, api_key)
    try:
        return int(status), json.loads(raw)
    except json.JSONDecodeError:
        return int(status), raw


def main() -> int:
    api_key = require_api_key()
    jar_dir = release_jar_dir()

    if not jar_dir.is_dir():
        print(f"ERROR: release folder not found: {jar_dir}", file=sys.stderr)
        return 1

    jars = find_jars(jar_dir)
    print(f"Found {len(jars)} JAR(s) in {jar_dir}")
    for jar in jars:
        print(f"  - {jar.name}")

    if len(jars) != EXPECTED_JAR_COUNT:
        print(
            f"ERROR: expected exactly {EXPECTED_JAR_COUNT} JARs, found {len(jars)}.",
            file=sys.stderr,
        )
        return 1

    print(f"\nUploading to project {PROJECT_ID} via {UPLOAD_URL}")
    print(f"gameVersionNames={DEFAULT_GAME_VERSION_NAMES}")
    print()

    results: list[tuple[str, str, str]] = []

    for jar in jars:
        metadata = build_metadata(jar)
        status, payload = upload_file(api_key, jar, metadata)

        file_id = ""
        if isinstance(payload, dict) and "id" in payload:
            file_id = str(payload["id"])

        ok = 200 <= status < 300 and bool(file_id)
        status_label = str(status)

        if ok:
            print(f"OK  {jar.name}  status={status}  id={file_id}")
            results.append((jar.name, status_label, file_id))
            continue

        print(f"FAIL {jar.name}  status={status}", file=sys.stderr)
        print("RESPONSE_BODY=", end="", file=sys.stderr)
        if isinstance(payload, dict):
            print(json.dumps(payload, indent=2), file=sys.stderr)
        else:
            print(payload, file=sys.stderr)

        print("\n=== Partial summary before failure ===")
        print("filename | status | CurseForge file ID")
        for name, st, fid in results:
            print(f"{name} | {st} | {fid}")
        print(f"{jar.name} | {status_label} | FAILED")
        return 1

    print("\n=== CurseForge Upload Summary ===")
    print("filename | status | CurseForge file ID")
    for name, st, fid in results:
        print(f"{name} | {st} | {fid}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
