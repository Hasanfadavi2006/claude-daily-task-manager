"""
Rubika bridge — runs inside Android via Chaquopy.
All functions return plain dicts/strings (no async leakage to Java).
"""
import asyncio
import json
import os

_SESSION_FILE = None  # set by Java via set_session_path()

def set_session_path(path: str):
    global _SESSION_FILE
    _SESSION_FILE = path

def _run(coro):
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        return loop.run_until_complete(coro)
    finally:
        loop.close()

# ── Login ──────────────────────────────────────────────────────────────────

def request_otp(phone: str) -> dict:
    """Step 1: ask Rubika to send OTP."""
    try:
        from rubpy import Client
        async def _do():
            client = Client(phone, session=_SESSION_FILE)
            result = await client.send_code(phone)
            return {"ok": True, "hint": str(result)}
        return _run(_do())
    except Exception as e:
        return {"ok": False, "error": str(e)}

def verify_otp(phone: str, otp: str) -> dict:
    """Step 2: submit OTP, save session."""
    try:
        from rubpy import Client
        async def _do():
            client = Client(phone, session=_SESSION_FILE)
            await client.sign_in(phone, otp)
            return {"ok": True}
        return _run(_do())
    except Exception as e:
        return {"ok": False, "error": str(e)}

# ── Data fetching ──────────────────────────────────────────────────────────

def get_groups(phone: str, max_groups: int = 30) -> dict:
    """Return list of groups the user is member of."""
    try:
        from rubpy import Client
        async def _do():
            async with Client(phone, session=_SESSION_FILE) as client:
                chats = await client.get_chats()
                groups = []
                for chat in chats:
                    try:
                        ctype = str(getattr(chat, 'type', '')).lower()
                        if 'group' in ctype or 'channel' in ctype:
                            groups.append({
                                "guid": str(getattr(chat, 'object_guid', '')),
                                "title": str(getattr(chat, 'title', '')),
                                "type": ctype,
                            })
                    except Exception:
                        pass
                return {"ok": True, "groups": groups[:max_groups]}
        return _run(_do())
    except Exception as e:
        return {"ok": False, "error": str(e)}

def get_group_messages(phone: str, group_guid: str, count: int = 20) -> dict:
    """Fetch last N messages from a group."""
    try:
        from rubpy import Client
        async def _do():
            async with Client(phone, session=_SESSION_FILE) as client:
                msgs = await client.get_messages(group_guid, start_id=0)
                result = []
                for m in (msgs or []):
                    try:
                        result.append({
                            "msg_id": str(getattr(m, 'message_id', '')),
                            "author": str(getattr(m, 'author_title', '')),
                            "text": str(getattr(m, 'text', '') or ''),
                            "time": str(getattr(m, 'time', '')),
                        })
                    except Exception:
                        pass
                return {"ok": True, "messages": result[:count]}
        return _run(_do())
    except Exception as e:
        return {"ok": False, "error": str(e)}

def is_logged_in(phone: str) -> bool:
    """Quick check: does a valid session file exist?"""
    if not _SESSION_FILE:
        return False
    return os.path.exists(_SESSION_FILE + ".session") or os.path.exists(_SESSION_FILE)
