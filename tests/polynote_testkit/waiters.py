import requests
from tenacity import retry, stop_after_delay, wait_fixed
from polynote_testkit import FTConfig

@retry(stop=stop_after_delay(120), wait=wait_fixed(2), reraise=True)
def wait_http_ready(node_id: str, stack_name: str):
    headers = {"Host": f"{node_id}.{stack_name}.polynote.local"}
    r = requests.get(FTConfig.HEALTH_CHECK_URL, headers=headers, timeout=3)
    r.raise_for_status()
