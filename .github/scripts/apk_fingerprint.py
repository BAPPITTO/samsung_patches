#!/usr/bin/env python3
"""
Automatic APK analysis for Morphe fingerprinting.

Given a decompiled APK source tree (from Jadx) — or the raw APK itself, in which
case it greps the smali/raw dex strings — this script hunts for the code that
gates Samsung-only behavior and reports candidate methods so Fingerprints.kt can
be updated without doing it all by hand.

Search terms mirror the README checklist ("Things to do" > step 3):
    samsung, galaxy, ro.product.brand, Build.BRAND, only supported,
    not available, Knox, KLMS, Secure Folder, isSamsungDevice ...

In addition to generic gating methods, the script classifies candidates into
feature buckets so the specific gates that matter for this bundle are easy to
find in the report:
    - Secure Folder / Knox (KnoxSupportFingerprint)
    - OneDrive cloud-storage account check (OneDriveCheckFingerprint)
    - Network drive / SMB / FTP / WebDAV sources
    - Restriction nag dialogs ("only supported on Galaxy devices" etc.)

Usage:
    python3 apk_fingerprint.py <decompiled_src_dir_or_apk> [--json report.json]

Exit codes:
    0 - analysis completed (candidates found or not; see report)
    1 - bad input path
"""

import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path

SEARCH_TERMS = [
    "samsung",
    "galaxy",
    "ro.product.brand",
    "Build.BRAND",
    "Build.MANUFACTURER",
    "only supported",
    "not available",
    "not supported",
    "knx",  # catches knox variants/obfuscation remnants
    "knox",
    "klms",
    "secure folder",
    "securefolder",
    "isSamsung",
    "getBrand",
    "manufacturer",
    # OneDrive cloud-storage integration (gated to Samsung accounts in My Files)
    "onedrive",
    "one drive",
    "live.com",
    "microsoftaccount",
    "network drive",
    "smb",
    "ftp",
    "webdav",
]

# Feature buckets: each candidate is tagged with every bucket whose terms it hits,
# so the report groups Secure Folder / Knox gates separately from OneDrive gates.
FEATURE_BUCKETS = {
    "brandCheck": ["samsung", "galaxy", "ro.product.brand", "build.brand",
                   "build.manufacturer", "issamsung", "getbrand", "manufacturer"],
    "secureFolderKnox": ["knox", "klms", "knx", "secure folder", "securefolder",
                         "container", "vcsc"],
    "oneDrive": ["onedrive", "one drive", "live.com", "microsoftaccount",
                 "sharepoint", "wopi"],
    "networkDrive": ["network drive", "smb", "ftp", "webdav", "nfs", "samba"],
    "restrictionDialog": ["only supported", "not available", "not supported"],
}


def classify(candidate_text: str):
    """Return the list of feature buckets a candidate method belongs to."""
    low = candidate_text.lower()
    return sorted(
        bucket for bucket, terms in FEATURE_BUCKETS.items()
        if any(t in low for t in terms)
    )

# Method definition shapes we care about: boolean-returning device checks.
JAVA_METHOD_RE = re.compile(
    r"(?P<mods>(?:public|protected|private|static|final|\s)+?)\s*"
    r"(?P<ret>(?:boolean|Boolean|String|int|void))\s+"
    r"(?P<name>[A-Za-z_][\w$]*)\s*\((?P<params>[^)]*)\)"
)

SMALI_METHOD_RE = re.compile(r"^\.method\s+(?P<mods>[^\s]+(?:\s+[^\s]+)*)\s+(?P<sig>[A-Za-z_][\w$-]*\(.*\)[\w$./\[\]]+)")


def grep_java(src_dir: Path):
    """Walk Jadx-decompiled java sources and collect candidate gating methods."""
    candidates = []
    terms_lower = [t.lower() for t in SEARCH_TERMS]
    for f in src_dir.rglob("*.java"):
        try:
            text = f.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        low = text.lower()
        hits = [t for t in terms_lower if t in low]
        if not hits:
            continue
        for m in JAVA_METHOD_RE.finditer(text):
            name = m.group("name")
            ret = m.group("ret")
            mods = m.group("mods").strip()
            if ret != "boolean":
                continue
            # Grab a window of the method body for context / filter extraction
            start = m.start()
            body = text[start:start + 2000]
            body_low = body.lower()
            body_hits = sorted({t for t in terms_lower if t in body_low})
            if not body_hits:
                continue
            strings = sorted(set(re.findall(r'"([^"\n]{3,60})"', body)))[:10]
            candidates.append({
                "file": str(f.relative_to(src_dir)),
                "modifiers": mods,
                "returnType": ret,
                "methodName": name,
                "parameters": m.group("params").strip(),
                "matchedTerms": body_hits,
                "features": classify(name + " " + body),
                "stringsInBody": strings,
            })
    return candidates


def grep_smali(src_dir: Path):
    """If baksmali output exists instead of jadx java, mine .method signatures."""
    candidates = []
    terms_lower = [t.lower() for t in SEARCH_TERMS]
    for f in src_dir.rglob("*.smali"):
        try:
            text = f.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        low = text.lower()
        if not any(t in low for t in terms_lower):
            continue
        cls = str(f.relative_to(src_dir)).replace(os.sep, "/").rsplit(".smali", 1)[0]
        current = None
        for line in text.splitlines():
            m = SMALI_METHOD_RE.match(line.strip())
            if m:
                sig = m.group("sig")
                if sig.endswith(")Z"):  # boolean returns only
                    current = {
                        "class": "L" + cls + ";",
                        "modifiers": m.group("mods"),
                        "signature": sig,
                        "features": classify(cls + " " + sig),
                    }
                    candidates.append(current)
    return candidates


def extract_strings_from_apk(apk: Path):
    """Last-resort: list interesting strings directly from dex files in the APK."""
    found = set()
    terms_lower = [t.lower() for t in SEARCH_TERMS]
    try:
        with zipfile.ZipFile(apk) as z:
            for n in z.namelist():
                if not re.match(r"classes\d*\.dex$", n):
                    continue
                data = z.read(n)
                # crude string scan over raw dex bytes
                for s in re.findall(rb"[\x20-\x7e]{4,}", data):
                    txt = s.decode("ascii", "replace")
                    tl = txt.lower()
                    if any(t in tl for t in terms_lower):
                        found.add(txt)
                        if len(found) > 500:
                            return found
    except (zipfile.BadZipFile, OSError) as e:
        print(f"::warning::Could not read APK strings: {e}")
    return found


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("target", help="Decompiled source dir (jadx --no-res) or raw .apk")
    ap.add_argument("--json", default="fingerprint-report.json")
    args = ap.parse_args()

    target = Path(args.target)
    if not target.exists():
        print(f"::error::Target does not exist: {target}")
        sys.exit(1)

    report = {"target": str(target), "searchTerms": SEARCH_TERMS}

    if target.is_file() and target.suffix == ".apk":
        # Only raw APK available: pull interesting strings out of the dex.
        strings = sorted(extract_strings_from_apk(target))
        report["mode"] = "apk-raw-strings"
        report["interestingStrings"] = strings
        print(f"Found {len(strings)} interesting strings in dex files.")
    else:
        java_cands = grep_java(target)
        smali_cands = grep_smali(target)
        report["mode"] = "decompiled-tree"
        report["javaCandidates"] = java_cands
        report["smaliCandidates"] = smali_cands
        # Group by feature bucket so Secure Folder / OneDrive gates stand out.
        by_feature = {}
        for c in java_cands + smali_cands:
            for f in c.get("features", []):
                by_feature.setdefault(f, []).append(c)
        report["byFeature"] = {k: len(v) for k, v in by_feature.items()}
        print(f"Java boolean-method candidates: {len(java_cands)}")
        print(f"Smali boolean-method candidates: {len(smali_cands)}")
        for feat in ("brandCheck", "secureFolderKnox", "oneDrive",
                     "networkDrive", "restrictionDialog"):
            n = len(by_feature.get(feat, []))
            print(f"  feature {feat}: {n} candidate(s)")
            if n == 0 and feat in ("secureFolderKnox", "oneDrive"):
                print(f"::warning::No {feat} candidates found — the corresponding "
                      f"fingerprint cannot be verified from this APK build.")

    with open(args.json, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
    print(f"Report written to {args.json}")

    # Summary for the workflow log
    total = len(report.get("javaCandidates", [])) + len(report.get("smaliCandidates", [])) \
        + len(report.get("interestingStrings", []))
    if total == 0:
        print("::warning::No fingerprint candidates found. "
              "Verify the APK version/package or search terms.")
    print(f"SUMMARY_CANDIDATES={total}")


if __name__ == "__main__":
    main()
