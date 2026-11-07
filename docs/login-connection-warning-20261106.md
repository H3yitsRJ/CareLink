# Login connection warning

A patient reported a red connection warning for less than a second after signing in, followed by normal loading. The login route initialized Firebase availability to false, so the global banner treated a pending check as a confirmed failure.

Firebase availability now starts as unknown for each account and network change. The banner waits for a failed check, while a confirmed loss of Android network connectivity still shows it immediately. Saves and server reads remain protected until Firebase reachability succeeds. Periodic checks retain their last result while running. Remembered repositories read the latest source selection when connection state changes.

The regression test reproduces the pending-check warning under the previous decision rule. It also covers confirmed network/backend failures, successful checks, and signed-out behavior. No banner styling or copy changed.

Validation: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain --max-workers=1 --no-configuration-cache` passed, with 116 unit tests and zero failures or errors. `git diff --check` passed. The pending-check regression failed under the previous warning rule and passed after the fix. Existing compile-SDK compatibility and Gradle deprecation warnings remain.

Limits: Timothy's live login and physical phone are not available here; the reported timing has not been reproduced against his account. The code-level regression establishes the startup-state defect, not every possible cause of a short connection warning. No Jira update or Git publishing is part of this change.
