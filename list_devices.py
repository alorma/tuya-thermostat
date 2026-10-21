import json
from pathlib import Path

import tinytuya

HERE = Path(__file__).parent
creds = json.loads((HERE / "tinytuya.json").read_text())

cloud = tinytuya.Cloud(
    apiRegion=creds["apiRegion"],
    apiKey=creds["apiKey"],
    apiSecret=creds["apiSecret"],
)
if cloud.error:
    raise SystemExit(f"Cloud error: {cloud.error}")

devices = cloud.getdevices(verbose=False)
if not isinstance(devices, list):
    raise SystemExit(f"Unexpected response: {devices}")

(HERE / "devices.json").write_text(json.dumps(devices, indent=2, ensure_ascii=False))

for d in devices:
    key = d.get("key") or ""
    print(f"- {d.get('name')} | id={d.get('id')} | mac={d.get('mac')} | "
          f"category={d.get('category')} | product={d.get('product_name')} | "
          f"key={key[:3]}…({len(key)} chars)")
    status = cloud.getstatus(d["id"])
    for s in status.get("result", []):
        print(f"    {s['code']} = {s['value']}")
