# Conference recording pilot

AppTrack remains an ordinary default Android dialer. The Record control connects a user-configured telephone recording endpoint on the customer call's SIM and requests a carrier conference only when Telecom reports the calls conferenceable. It does not capture local call audio, require root, or claim a recording exists merely because a conference joined.

## Setup and test

1. Install the debug APK and select AppTrack as default Phone app.
2. Use a SIM/carrier with call hold and conference support.
3. Establish one customer call, notify the participant, and select Record via conference.
4. Enter a real recording-service telephone endpoint. For a conference-only test, a consenting second person's number can be used, but this produces no recording.
5. Check merge, audio on both sides, Bluetooth, service rejection, 45-second connection timeout, customer hangup, recorder hangup, and Disconnect recorder. Ensure the customer survives a recorder failure.
6. Repeat for incoming/outgoing calls and each supported carrier.

## Remaining service integration

Provision a telephone endpoint through a provider/PBX. Create authenticated sessions, correlate recording legs to sessions (assigned numbers or provider-supported DTMF), record with participant notice, validate signed provider webhooks, and associate recordings with durable call IDs. Add authenticated playback, retention/access controls, a local session database and background status reconciliation. Provider secrets must stay on the backend. No backend, provider account or audio sync is included in this pilot.

The opening conversation before merge is not recorded. Automatic recording and guaranteed recording from call start require further work or routing calls through a business telephony platform. Carrier/OEM conference behavior requires physical-device validation. Do not use emergency calls for testing.

CI builds a debug APK and runs unit tests and lint; download apptrack-debug from the Actions run.
