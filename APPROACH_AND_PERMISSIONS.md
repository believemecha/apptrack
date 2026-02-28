# AppTrack – Call Assistant: Approach and Permissions

## How the Call Assistant works (approach)

The Call Assistant answers incoming calls on your behalf and plays a text-to-speech (TTS) greeting to the caller, then optionally listens for their reply with speech recognition.

- **We do not inject audio into the phone call.** Normal Android apps cannot send TTS directly into the telecom audio path.
- **We use earpiece loopback:**  
  - TTS is played through the **phone earpiece** (speaker is turned **off**).  
  - The **microphone** picks up that sound (and the caller’s voice).  
  - Whatever the mic captures is sent on the **call uplink**, so the **caller hears the assistant** via this acoustic path.

So: **TTS → earpiece → mic → call uplink → caller**. Speech recognition uses the same microphone, so it can hear both the caller and the earpiece.

The overlay is a **foreground service** with a Compose UI; the service provides the LifecycleOwner and SavedStateRegistryOwner so Compose works correctly.

---

## Permissions

### Declared in the app (AndroidManifest)

| Permission | Purpose |
|------------|--------|
| `READ_PHONE_STATE` | Detect incoming/outgoing calls and phone state. |
| `CALL_PHONE` | Place outbound calls. |
| `READ_CALL_LOG` | Show and manage call history. |
| `READ_CONTACTS` | Show contact names and info. |
| `READ_PHONE_NUMBERS` | Read phone numbers (e.g. for caller ID). |
| `ANSWER_PHONE_CALLS` | Answer incoming calls (Call Assistant). |
| `MANAGE_OWN_CALLS` | Manage the app’s own calls. |
| **`RECORD_AUDIO`** | **Required for Call Assistant:** microphone for (1) speech recognition (caller + earpiece) and (2) call uplink so the caller hears the TTS. Must be granted at runtime. |
| `MODIFY_AUDIO_SETTINGS` | Set in-call audio mode, speaker, etc. |
| `SYSTEM_ALERT_WINDOW` | Draw the call/assistant overlay over other apps and lock screen. Must be granted at runtime (Settings → Display over other apps). |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_PHONE_CALL` | Run the Call Assistant as a foreground service with microphone and phone-call types. |
| `POST_NOTIFICATIONS` (Android 13+) | Show “Caller left a message” and “Call Assistant Active” notifications. Requested at runtime on Android 13+. |

### When they are requested

- **On first launch**, the app requests: `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `CALL_PHONE`, **`RECORD_AUDIO`**, and on Android 13+, **`POST_NOTIFICATIONS`**. The main screen is only shown when these are granted (so Call Assistant has microphone access).
- **Overlay (Display over other apps):** requested in a separate step; the app guides you to Settings if it’s not granted.
- **Default phone app:** the app can ask to be set as the default dialer so it can answer calls and show the assistant.

### Why “no voice” can happen

- **`RECORD_AUDIO` not granted:** TTS and speech recognition need the mic; without it you get “unable to access the voice.” The app now requires this permission before showing the main UI.
- **Overlay not granted:** The assistant overlay may not appear or work correctly; grant “Display over other apps” for the app.
- **Audio route / timing:** On some devices, earpiece loopback may be weak; the app uses earpiece by default so the mic can pick up the TTS. If the caller still doesn’t hear the assistant, try enabling speaker in Call Assistant settings as a fallback (then the caller may hear TTS from the loudspeaker via the mic).

---

## Summary

- **Approach:** Earpiece loopback — TTS plays on earpiece, mic captures it and the caller, uplink sends that to the caller. No direct injection into the call.
- **Critical for voice:** Grant **Microphone** (`RECORD_AUDIO`) and **Display over other apps** (`SYSTEM_ALERT_WINDOW`); the app now asks for microphone before you use the rest of the app so the Call Assistant can access the voice.
