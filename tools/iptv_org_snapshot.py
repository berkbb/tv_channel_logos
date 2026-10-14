#!/usr/bin/env python3
"""Builds app/src/main/assets/iptv-org-channels.tsv.gz from the iptv-org API.

The app ships this snapshot so the first start needs no 14 MB download; it then refreshes the
list in the background once a week (IptvOrgSource). Rows are "name<TAB>logo URL<TAB>country",
with the same filters as IptvOrgSource.download: the channel's own logo (no feed logos), no SVG.
iptv-org's data is public domain (Unlicense); the logos themselves are only linked.

Usage: python3 tools/iptv_org_snapshot.py
"""
import gzip
import json
import pathlib
import urllib.request

API = "https://iptv-org.github.io/api"
OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/iptv-org-channels.tsv.gz"


def fetch(name):
    request = urllib.request.Request(f"{API}/{name}", headers={"User-Agent": "tv-channel-logos"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def clean(text):
    return text.replace("\t", " ").replace("\n", " ")


def main():
    logos = {}
    for logo in fetch("logos.json"):
        if logo.get("channel") and logo.get("url") and logo.get("feed") is None \
                and (logo.get("format") or "").upper() != "SVG":
            logos.setdefault(logo["channel"], logo["url"])
    lines = []
    for channel in fetch("channels.json"):
        url = logos.get(channel.get("id"))
        if not url:
            continue
        names = [channel.get("name")] + (channel.get("alt_names") or [])
        for name in dict.fromkeys(n for n in names if n):
            lines.append(f"{clean(name)}\t{url}\t{channel.get('country') or ''}\n")
    data = "".join(lines).encode()
    OUT.write_bytes(gzip.compress(data, 9, mtime=0))
    print(f"{len(lines)} rows, {len(data)} bytes, {OUT.stat().st_size} bytes gzipped -> {OUT}")


if __name__ == "__main__":
    main()
