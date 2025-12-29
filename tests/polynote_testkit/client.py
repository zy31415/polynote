from __future__ import annotations

import requests
from typing import Any, Dict, List, Optional

from .conf import FTConfig

class PolyNoteClient:
    """
    Thin HTTP client for a single PolyNote node.

    This is a *test client*, not production code.
    """

    def __init__(self, node_id: str, stack_name: str):
        self.node_id = node_id
        self.stack_name = stack_name
        self._session = requests.Session()
        self._timeout = 5

    # -----------------------
    # Notes API
    # -----------------------

    def create_note(self, title: str, body: str) -> Dict[str, Any]:
        return self._post(
            "/notes",
            json={
                "title": title,
                "body": body,
            },
        )

    def list_notes(self) -> List[Dict[str, Any]]:
        return self._get("/notes")

    def update_note(
        self,
        note_id: str,
        ts: int,
        *,
        title: Optional[str] = None,
        body: Optional[str] = None,
    ) -> Dict[str, Any]:
        payload: Dict[str, Any] = {}
        if title is not None:
            payload["title"] = title
        if body is not None:
            payload["body"] = body

        return self._put(f"/notes/{note_id}?ts={ts}", json=payload)

    def delete_note(self, note_id: str, ts: int) -> None:
        self._delete(f"/notes/{note_id}?ts={ts}")

    # -----------------------
    # Replication
    # -----------------------

    def sync_from(self, remote_node_id: str) -> Any:
        """
        Trigger a replication pull from another node.
        """
        return self._put(f"/replication/sync/{remote_node_id}")

    def reset(self) -> None:
        """
        Reset the node to an empty state.
        """
        self._post("/admin/reset")

    # -----------------------
    # Internal helpers
    # -----------------------

    def _url(self, path: str) -> str:
        return f"{FTConfig.BASE_URL}{path}"

    def _get(self, path: str) -> Any:
        r = self._session.get(self._url(path), timeout=self._timeout, headers=self._headers(path))
        self._raise_for_status(r)
        return self._json_or_none(r)

    def _post(self, path: str, json: Optional[Dict[str, Any]] = None) -> Any:
        r = self._session.post(self._url(path), json=json, timeout=self._timeout, headers=self._headers(path))
        self._raise_for_status(r)
        return self._json_or_none(r)

    def _put(self, path: str, json: Optional[Dict[str, Any]] = None) -> Any:
        r = self._session.put(self._url(path), json=json, timeout=self._timeout, headers=self._headers(path))
        self._raise_for_status(r)
        return self._json_or_none(r)

    def _delete(self, path: str) -> None:
        r = self._session.delete(self._url(path), timeout=self._timeout, headers=self._headers(path))
        self._raise_for_status(r)

    def _headers(self, path: str) -> Dict[str, str]:
        return {"Host": f"{self.node_id}.{self.stack_name}.polynote.local"}

    @staticmethod
    def _json_or_none(response: requests.Response) -> Any:
        if not response.content:
            return None
        return response.json()

    @staticmethod
    def _raise_for_status(response: requests.Response) -> None:
        try:
            response.raise_for_status()
        except requests.HTTPError as e:
            raise RuntimeError(
                f"HTTP {response.status_code}: {response.text}"
            ) from e