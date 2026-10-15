#!/usr/bin/env python3
"""Verify the actual built APK's permissions and contents, not only source manifests."""
import argparse
import hashlib
import re
import subprocess
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("apk", type=Path)
parser.add_argument("--aapt", required=True)
args = parser.parse_args()
manifest = subprocess.check_output([args.aapt, "dump", "badging", str(args.apk)], text=True)
permissions = set(re.findall(r"uses-permission: name='([^']+)'", manifest))
allowed = {
    "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_SPECIAL_USE", "android.permission.POST_NOTIFICATIONS",
    "android.permission.SCHEDULE_EXACT_ALARM", "android.permission.VIBRATE",
}
assert permissions == allowed, f"Unexpected permission set: {permissions ^ allowed}"
assert "name='local.suspendedclock'" in manifest, "Wrong application identity"
with zipfile.ZipFile(args.apk) as archive:
    names = archive.namelist()
    assert not any(n.startswith("lib/") or n.startswith("assets/") for n in names), "Unexpected packaged binaries/assets"
    dex = b"".join(archive.read(n) for n in names if n.endswith(".dex"))
    for signature in (b"Lcom/bytedance/", b"Lcom/qq/e/", b"Lcom/sigmob/", b"Lcom/qumeng/", b"Lcom/beizi/", b"Lcom/octopus/"):
        assert signature not in dex, f"Ad SDK present: {signature}"
    for signature in (b"Ljava/net/", b"Lokhttp3/", b"Landroid/webkit/WebView;"):
        assert signature not in dex, f"Unexpected networking API: {signature}"
print("PASS: APK identity, exact permission allowlist, no native libraries/assets, no known ad SDK or networking API references")
print("SHA-256:", hashlib.sha256(args.apk.read_bytes()).hexdigest())
