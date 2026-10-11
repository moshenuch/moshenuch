#!/usr/bin/env python3
"""Fail CI if Nokia Conversation's bulk read/unread stops using tested targets.

This validates production source wiring, not actual Pixel emulator UI taps.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text(
    encoding="utf-8"
)
helper = (root / "app/src/main/java/com/moshenuch/c2reborn/NativeSmsBulkSelection.java").read_text(
    encoding="utf-8"
)


def method(signature: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise AssertionError("Missing production method: " + signature)
    brace = source.find("{", start + len(signature))
    if brace < 0:
        raise AssertionError("Missing method body: " + signature)
    depth = 0
    for i in range(brace, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[brace:i + 1]
    raise AssertionError("Unbalanced source method: " + signature)


def check(label: str, passed: bool) -> None:
    if not passed:
        raise AssertionError("FAIL: " + label)
    print("PASS: " + label)


options = method("private void openOptionsForPage()")
choice = method("private void chooseOption()")
received = method("private ArrayList<Long> receivedConversationMessageIds()")
targets = method("private ArrayList<Long> receivedConversationTargets(")
locally = method("private boolean setSmsReadStateLocally(")
persist = method("private void persistSmsReadOverrides()")

check("Conversation Options evaluates incoming selected targets",
      "receivedConversationTargets(target)" in options)
check("Mixed marked states expose both read and unread actions",
      'choices.add("Mark as read")' in options and
      'choices.add("Mark as unread")' in options)
check("Conversation read/unread uses shared tested target selector",
      "receivedConversationTargets(m)" in choice)
check("Conversation applies each selected target by SMS ID",
      "for (Long id : targets)" in choice and
      "setSmsReadStateLocally(smsById(id), unread)" in choice)
check("Conversation persists changed read/unread state",
      "persistSmsReadOverrides();" in choice)
check("Conversation Mark all includes messages in current thread",
      "for (AndroidBackend.DeviceSms item : conversationMessages())" in choice and
      "markedSmsIds.add(item.id)" in choice)
check("Conversation Unmark all clears selection",
      "markedSmsIds.clear();" in choice)
check("Incoming ID extraction excludes sent items",
      "conversationMessages()" in received and
      "sms.type == Telephony.Sms.MESSAGE_TYPE_INBOX" in received)
check("Selector calls exact CI-tested utility",
      "NativeSmsBulkSelection.targetReceivedIds(" in targets)
check("Selector uses currently marked SMS and highlight",
      "markedSmsIds" in targets and "highlighted.id" in targets)
check("Helper prevents sent-only marks from falling back to highlight",
      "boolean bulkMode = markedIds != null && !markedIds.isEmpty();" in helper and
      "marked.contains(id)" in helper)
check("Read-state setter ignores sent messages",
      "message.type != Telephony.Sms.MESSAGE_TYPE_INBOX" in locally)
check("Local read-state mutation does not write to Android Telephony provider",
      "localUnreadSmsIds" in locally and "localReadSmsIds" in locally and
      "backend." not in locally)
check("Read-state persistence is app-private only",
      'saveLongSet("sms_local_read"' in persist and
      'saveLongSet("sms_local_unread"' in persist and "backend." not in persist)
print("Nokia conversation bulk read/unread production wiring: all checks passed")
