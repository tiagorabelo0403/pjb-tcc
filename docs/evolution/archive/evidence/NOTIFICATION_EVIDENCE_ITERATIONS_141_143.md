# Notification evidence rounds 141-143

## Round 141
- Added `NotificationPreferenceFlowIT` backed by PostgreSQL Testcontainers through `PjbIntegrationTestBase`
- Covers default preference projection without persisted row
- Covers persisted preference update with anti-spam window and channel flags

## Round 142
- Added `IntimacaoMulticanalServiceFlowIT` backed by PostgreSQL Testcontainers
- Covers real persistence of `Processo`, `Usuario`, notification preference and notification history
- Verifies tracking token persistence and signed formal document projection on multichannel dispatch

## Round 143
- Added explicit provider contract for notification preferences and multichannel dispatch
- Added `NotificationControllerProviderContractTest`
- Added `PjbNotificationProviderContractCoverageArchitectureTest`
- Added `PjbNotificationSurfaceArchitectureTest`
- Added explicit `@PjbTransactionalBudget` declarations to notification preference and multichannel dispatch services
