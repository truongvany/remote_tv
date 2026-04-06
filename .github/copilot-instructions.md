# Remote TV Project Instructions

## Scope
This repository is an Android remote-control app built with Jetpack Compose and MVVM.
Use these instructions for any change in UI, networking, discovery, and TV protocol layers.

## Architecture Map
- App entry: app/src/main/java/com/example/remote_tv/MainActivity.kt
- Main shell and tab routing: app/src/main/java/com/example/remote_tv/ui/screens/RemoteScreen.kt
- Home remote controls: app/src/main/java/com/example/remote_tv/ui/screens/MainRemoteTab.kt
- Cast/discovery UI: app/src/main/java/com/example/remote_tv/ui/screens/CastScreen.kt
- ViewModel state orchestration: app/src/main/java/com/example/remote_tv/ui/viewmodel/TVViewModel.kt
- Repository boundary: app/src/main/java/com/example/remote_tv/data/repository/TVRepository.kt
- Repository implementation: app/src/main/java/com/example/remote_tv/data/repository/TVRepositoryImpl.kt
- Discovery service (NSD + subnet scan): app/src/main/java/com/example/remote_tv/data/discovery/TVDiscoveryService.kt
- Connection manager (timeouts/retries/protocol routing): app/src/main/java/com/example/remote_tv/data/connection/TVConnectionManager.kt
- Connection policy constants: app/src/main/java/com/example/remote_tv/data/connection/ConnectionPolicy.kt
- Protocols: app/src/main/java/com/example/remote_tv/data/protocol/
- In-app diagnostics stream: app/src/main/java/com/example/remote_tv/data/debug/InAppDiagnostics.kt

## Working Rules (Team Git Workflow)
- Never push directly to main.
- main is production-ready only.
- Work starts from develop into feature branches.
- Branch naming convention: feature/<feature-name>-<student-name>
- Commit messages must be English and conventional style: feat:, fix:, docs:, refactor:, chore:.
- Prefer small focused commits; avoid large mixed commits.
- Merge to develop only through Pull Request with at least 1 reviewer approval from a different teammate.

## Coding Conventions
- Keep MVVM separation strict: UI in ui/, state in viewmodel, network/protocol in data/.
- Do not move business logic into composables.
- Keep all network troubleshooting visible through InAppDiagnostics.
- Preserve existing design language in Cast UI (dark + orange high-contrast theme).

## TV Connection Notes
- Samsung: supports ws/wss channel negotiation on 8001/8002/8009, but model differences are common.
- Android TV remote2 pairing is partially implemented; treat TLS pairing as a known limitation.
- If connection fails, add clear in-app diagnostic lines instead of only logcat output.

## Validation Checklist
Before finishing a task, run:
1. .\\gradlew.bat :app:assembleDebug
2. Verify Cast tab behavior manually:
- device discovery
- tap-to-connect spinner
- connection result and navigation flow
- in-app logs visibility

## Safety
- Do not revert unrelated local changes.
- Stage and commit only files related to the requested fix.
