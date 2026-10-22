# Implementation Plan: Worker Location Tracking

## Objective
Implement continuous, 5-minute interval worker location tracking with full path history, syncing data to Firestore for manager access while maintaining user/manager separation.

## Proposed Changes

### 1. Permissions & Manifest
- Add `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `FOREGROUND_SERVICE_LOCATION` permissions to `AndroidManifest.xml`.
- Declare `LocationTrackingService` as a Foreground Service in `AndroidManifest.xml`.

### 2. Firestore Schema & Security
- Update `firebase-blueprint.json` to define the `/users/{userId}/location_history` collection.
- Update `firestore.rules` to:
    - Allow workers to `create` and `read` their own location documents.
    - Allow managers to `read` location documents of users where `managerId == request.auth.uid`.

### 3. Location Tracking Service
- Create `LocationTrackingService.kt`:
    - Use `FusedLocationProviderClient` to request location updates every 5 minutes.
    - Use a persistent Notification to satisfy Foreground Service requirements.
    - Start service upon user login and stop on logout.

### 4. Data Layer & Sync
- Implement `LocationRepository` to write `LocationPoint` data (latitude, longitude, timestamp) to Firestore.
- Use `WorkManager` (if needed for robust background sync, though Foreground Service + Firestore SDK handles offline-to-online sync automatically).

## Implementation Steps

1.  **Phase 1: Setup & Permissions**
    - Add location permissions to `AndroidManifest.xml`.
    - Create `LocationTrackingService` skeleton.
2.  **Phase 2: Schema & Rules**
    - Update `firebase-blueprint.json` and `firestore.rules` for location history.
3.  **Phase 3: Service Implementation**
    - Implement the `FusedLocationProviderClient` logic in the service to capture and persist locations.
4.  **Phase 4: Integration**
    - Integrate service start/stop with Auth flow.
    - Add UI to toggle/monitor tracking (if applicable).
