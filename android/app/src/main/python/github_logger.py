"""
Pushes log entries to GitHub as JSON files via REST API.
No git binary needed — uses the Contents API directly.
"""
import json
import base64
import urllib.request
import urllib.error
from datetime import datetime

def push_log(token: str, owner: str, repo: str, category: str, payload: dict) -> dict:
    """
    Creates or appends a log entry to logs/{date}/{category}.json in the repo.
    Returns {"ok": True} or {"ok": False, "error": "..."}.
    """
    if not token or not owner or not repo:
        return {"ok": False, "error": "GitHub credentials not set"}

    date_str = datetime.now().strftime("%Y-%m-%d")
    path = f"logs/{date_str}/{category}.json"
    api_url = f"https://api.github.com/repos/{owner}/{repo}/contents/{path}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Content-Type": "application/json",
        "User-Agent": "DailyTask-Android/1.0",
    }

    # Try to fetch existing file to get SHA (needed for update)
    existing_content = []
    sha = None
    try:
        req = urllib.request.Request(api_url, headers=headers)
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read())
            sha = data.get("sha")
            raw = base64.b64decode(data["content"].replace("\n", "")).decode("utf-8")
            existing_content = json.loads(raw)
    except urllib.error.HTTPError as e:
        if e.code != 404:
            return {"ok": False, "error": f"fetch HTTP {e.code}"}
    except Exception as e:
        existing_content = []

    # Append new entry
    entry = {
        "ts": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
        **payload
    }
    existing_content.append(entry)

    # Encode and push
    new_content = base64.b64encode(
        json.dumps(existing_content, ensure_ascii=False, indent=2).encode("utf-8")
    ).decode("ascii")

    body = {
        "message": f"log: {category} @ {entry['ts']}",
        "content": new_content,
    }
    if sha:
        body["sha"] = sha

    try:
        req = urllib.request.Request(
            api_url,
            data=json.dumps(body).encode("utf-8"),
            headers=headers,
            method="PUT",
        )
        with urllib.request.urlopen(req, timeout=20) as resp:
            resp.read()
        return {"ok": True}
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="ignore")[:200]
        return {"ok": False, "error": f"push HTTP {e.code}: {err}"}
    except Exception as e:
        return {"ok": False, "error": str(e)}
