# koiskApp
## Overview

**KoiskApp** is an Android kiosk-mode application built with Jetpack Compose and Material 3. It registers itself as a **Device Owner** and uses **Lock Task Mode** to restrict the device to a single app — a common requirement for point-of-sale terminals, digital signage, and dedicated-purpose devices.

Once provisioned, the app takes over as the system launcher: the Home, Back, and Recents buttons are disabled, the status bar is locked down, and users cannot exit to other apps or reach system settings. Additional user restrictions block Safe Boot, factory reset, and adding new users.

The app ships with two Compose screens demonstrating in-app navigation (handled with plain state, no navigation library), a live status card showing Device Owner and Lock Task state, and a manual exit button for testing.

### Key APIs

- `DeviceAdminReceiver` — receives device admin lifecycle callbacks
- `DevicePolicyManager.setLockTaskPackages()` — allowlists the app for lock task
- `DevicePolicyManager.addPersistentPreferredActivity()` — makes the app the persistent HOME launcher
- `Activity.startLockTask()` / `stopLockTask()` — enters and exits kiosk mode
- `DevicePolicyManager.addUserRestriction()` — blocks safe boot, factory reset, and user creation

### Setup at a glance

1. Create an AVD with a **Google APIs** system image (not Google Play) and skip account sign-in
2. Install the app, then run:
```bash
   adb shell dpm set-device-owner com.example.koiskapp/.MyDeviceAdminReceiver
```
3. Relaunch the app — kiosk mode activates automatically

> **Note:** Device Owner can only be set on an un-provisioned device with no user accounts. See the Troubleshooting section for common provisioning errors.
