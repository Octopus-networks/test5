#!/usr/bin/env python3
"""Static security checks for Firebase rules.

These checks protect the highest-risk privacy boundaries for Mithaq:
private user documents, mutual-like/chat creation, sensitive media, and
verification documents. They are intentionally conservative and complement
Firebase emulator tests.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIRESTORE = (ROOT / "firestore.rules").read_text(encoding="utf-8")
STORAGE = (ROOT / "storage.rules").read_text(encoding="utf-8")


def block(text: str, header: str, next_headers: list[str]) -> str:
    start = text.index(header)
    end = len(text)
    for marker in next_headers:
        pos = text.find(marker, start + len(header))
        if pos != -1:
            end = min(end, pos)
    return text[start:end]


def assert_contains(name: str, text: str, expected: str) -> None:
    if expected not in text:
        raise AssertionError(f"{name}: expected to find {expected!r}")


def assert_not_contains(name: str, text: str, forbidden: str) -> None:
    if forbidden in text:
        raise AssertionError(f"{name}: forbidden insecure rule {forbidden!r}")


def main() -> None:
    users = block(FIRESTORE, "match /users/{userId}", ["match /profiles/{userId}"])
    likes = block(FIRESTORE, "match /likes/{likeId}", ["match /blocks/{blockId}"])
    chat_rooms = block(FIRESTORE, "match /chatRooms/{roomId}", ["match /notifications/{notificationId}"])
    notifications = block(FIRESTORE, "match /notifications/{notificationId}", ["match /adminAuditLogs/{logId}"])
    profiles_storage = block(STORAGE, "match /profiles/{imageId}", ["match /voices/{voiceId}"])
    voices_storage = block(STORAGE, "match /voices/{voiceId}", ["match /verification/{userId}/{documentId}"])
    verification_storage = block(STORAGE, "match /verification/{userId}/{documentId}", ["}"])

    assert_not_contains("users read", users, "allow read: if isAuthenticated();")
    assert_contains("users read", users, "allow read: if isOwner(userId) || isAssignedWaliForUser(userId) || isAdmin();")

    assert_contains("likes create", likes, "request.resource.data.isMutual == false")
    assert_contains("likes update", likes, "allow update: if false;")

    assert_contains("legacy chatRooms create", chat_rooms, "allow create: if false;")
    assert_contains("notifications create", notifications, "allow create: if false;")

    assert_not_contains("profile media read", profiles_storage, "allow read: if isAuthenticated();")
    assert_contains("profile media read", profiles_storage, "canReadProfileMedia(imageId)")

    assert_not_contains("voice media read", voices_storage, "allow read: if isAuthenticated();")
    assert_contains("voice media read", voices_storage, "canReadVoiceIntro(voiceId)")

    assert_contains("verification document names", verification_storage, "validVerificationDocument(documentId)")
    # Re-submission is allowed (rejected/pending members overwrite their own evidence),
    # but approved evidence must stay immutable and updates must stay owner-scoped.
    assert_contains("verification updates owner-scoped", verification_storage, "allow update: if isOwner(userId)")
    assert_contains(
        "verification updates locked after approval",
        verification_storage,
        ".data.verificationStatus != 'VERIFIED'",
    )
    assert_contains("verification deletes", verification_storage, "allow delete: if false;")

    print("Firebase security rules static checks passed")


if __name__ == "__main__":
    main()
