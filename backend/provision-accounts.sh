#!/usr/bin/env bash
set -euo pipefail

AUTH_EMU="http://127.0.0.1:9099"
API="http://127.0.0.1:9000/api/v1"
PASS="Password123!"
KEY="fakeKey"

# email|role|,key:value fragment (leading comma, no outer braces)
USERS=(
"dr.alice@medvault.dev|DOCTOR|,\"name\":\"Dr. Alice Mehta\",\"phone\":\"+91 90000 10001\",\"city\":\"Mumbai\",\"specialization\":\"Cardiology\",\"hospitalName\":\"City Heart Institute\",\"hospitalAddress\":\"Bandra, Mumbai\",\"licenseNumber\":\"MCI-1001\",\"dob\":\"1980-05-12\",\"bloodGroup\":\"O+\""
"dr.bob@medvault.dev|DOCTOR|,\"name\":\"Dr. Bob Nair\",\"phone\":\"+91 90000 10002\",\"city\":\"Delhi\",\"specialization\":\"General Medicine\",\"hospitalName\":\"Apollo Clinic\",\"hospitalAddress\":\"MG Road, Delhi\",\"licenseNumber\":\"MCI-1002\",\"dob\":\"1978-09-30\",\"bloodGroup\":\"A+\""
"bb.redcross@medvault.dev|BLOOD_BANK|,\"name\":\"Red Cross Blood Bank\",\"phone\":\"+91 90000 20001\",\"city\":\"Guntur\",\"bankName\":\"Red Cross\",\"bankAddress\":\"MG Road, Guntur\",\"bloodBankLicense\":\"BB-2001\""
"bb.lifeline@medvault.dev|BLOOD_BANK|,\"name\":\"LifeLine Blood Bank\",\"phone\":\"+91 90000 20002\",\"city\":\"Hyderabad\",\"bankName\":\"LifeLine\",\"bankAddress\":\"Banjara Hills, Hyderabad\",\"bloodBankLicense\":\"BB-2002\""
"bb.care@medvault.dev|BLOOD_BANK|,\"name\":\"Care Blood Center\",\"phone\":\"+91 90000 20003\",\"city\":\"Bangalore\",\"bankName\":\"Care\",\"bankAddress\":\"Indiranagar, Bangalore\",\"bloodBankLicense\":\"BB-2003\""
"patient@medvault.dev|PATIENT|,\"name\":\"Test Patient\",\"phone\":\"+91 90000 30001\",\"city\":\"Mumbai\",\"dob\":\"1995-02-10\",\"bloodGroup\":\"B+\""
)

for line in "${USERS[@]}"; do
  EMAIL="${line%%|*}"
  rest="${line#*|}"
  ROLE="${rest%%|*}"
  FRAG="${rest#*|}"

  echo "==> Auth account: $EMAIL ($ROLE)"
  RESP=$(python3 - "$AUTH_EMU" "$KEY" "$EMAIL" "$PASS" <<'PY'
import sys, json, urllib.request, urllib.error
emu, key, email, pw = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
def post(path, payload):
    req = urllib.request.Request(emu+path+"?key="+key, data=json.dumps(payload).encode(),
        headers={"Content-Type":"application/json"}, method="POST")
    try:
        return json.loads(urllib.request.urlopen(req).read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())
signup = post("/identitytoolkit.googleapis.com/v1/accounts:signUp",
    {"email":email,"password":pw,"returnSecureToken":True})
if "idToken" in signup:
    print(json.dumps({"idToken":signup["idToken"],"localId":signup["localId"]}))
else:
    signin = post("/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword",
        {"email":email,"password":pw,"returnSecureToken":True})
    if "idToken" in signin:
        print(json.dumps({"idToken":signin["idToken"],"localId":signin["localId"]}))
    else:
        print(json.dumps({"error": signup.get("error", signin)}))
PY
)
  ERR=$(echo "$RESP" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('error',''))")
  if [ -n "$ERR" ]; then echo "    ERROR: $ERR"; continue; fi
  IDTOKEN=$(echo "$RESP" | python3 -c "import sys,json;print(json.load(sys.stdin)['idToken'])")
  ACCT_UID=$(echo "$RESP" | python3 -c "import sys,json;print(json.load(sys.stdin)['localId'])")
  echo "    uid=$ACCT_UID"

  echo "==> Profile via backend /auth/setup-role"
  BODY="{\"role\":\"$ROLE\"$FRAG}"
  HTTP=$(curl -s -o /tmp/setupresp.json -w "%{http_code}" -X POST \
    "$API/auth/setup-role" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $IDTOKEN" \
    -d "$BODY")
  echo "    http=$HTTP body=$(cat /tmp/setupresp.json)"
  echo
done
echo "ALL DONE"
