#!/usr/bin/env python3
"""Create / ensure the demo Auth accounts in the Firebase Auth emulator.

Uses FIXED uids so the Firestore `users/{uid}` profiles (imported from the
persisted emulator export) stay linked to the Auth accounts across restarts.

No third-party deps: talks to the Auth emulator's admin REST API directly.
Idempotent: existing accounts are left alone (password refreshed).
"""
import json
import sys
import urllib.request
import urllib.error

AUTH_EMU = "http://127.0.0.1:9099"
PROJECT = "medvault-11c68"
KEY = "fakeKey"
PASSWORD = "Password123!"

# (uid, email, display_name)
USERS = [
    ("uid_dr_alice",     "dr.alice@medvault.dev",     "Dr. Alice Mehta"),
    ("uid_dr_bob",       "dr.bob@medvault.dev",       "Dr. Bob Nair"),
    ("uid_bb_redcross",  "bb.redcross@medvault.dev",  "Red Cross Blood Bank"),
    ("uid_bb_lifeline",  "bb.lifeline@medvault.dev",  "LifeLine Blood Bank"),
    ("uid_bb_care",      "bb.care@medvault.dev",      "Care Blood Center"),
    ("uid_patient",      "patient@medvault.dev",     "Test Patient"),
]


def _request(method, path, payload):
    url = f"{AUTH_EMU}/{path}?key={KEY}"
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        url, data=data, method=method,
        headers={"Content-Type": "application/json",
                 "Authorization": "Bearer owner"},
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.read().decode(), None
    except urllib.error.HTTPError as e:
        return e.read().decode(), e.code


def ensure_user(uid, email, name):
    # Try to create with a fixed localId (uid).
    body = {
        "localId": uid,
        "email": email,
        "password": PASSWORD,
        "displayName": name,
        "emailVerified": False,
        "disabled": False,
    }
    _, err = _request("POST",
                      f"identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts",
                      body)
    if err is None:
        print(f"  created {email} (uid={uid})")
        return
    # Already exists (by email or uid) -> update password + display name.
    upd = {
        "localId": uid,
        "email": email,
        "password": PASSWORD,
        "displayName": name,
    }
    _, err2 = _request("POST",
                       f"identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:update",
                       upd)
    if err2 is None:
        print(f"  updated {email} (uid={uid})")
    else:
        print(f"  WARN {email}: create/update failed: {err2}")


def main():
    print(f"Ensuring {len(USERS)} demo Auth accounts in {AUTH_EMU} ...")
    for uid, email, name in USERS:
        ensure_user(uid, email, name)
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
