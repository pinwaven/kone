# Factory Test and Venue Mode Requirements

## Background

This document records the confirmed requirements for replacing the existing temporary operation entry with a factory test entry, adding password protection, and adding a new temporary operation entry for venue mode.

## Requirement 1: Factory Test Entry

1. Rename the existing "临时操作" entry to "工厂测试".
2. Replace its icon with a new icon that better represents factory testing.
3. Add password protection before entering "工厂测试".
4. The factory test password is `5988`.
5. After the password is entered successfully, the user can enter "工厂测试" without entering the password again for 1 hour.
6. After 1 hour, entering "工厂测试" requires the password again.
7. The 1-hour unlock state should be kept in memory only. After the app restarts, the user should need to enter the password again.

## Requirement 2: Temporary Operation Entry

1. Add a new "临时操作" entry.
2. "临时操作" and "工厂测试" are sibling entries at the same UI level.
3. The new "临时操作" entry should use the original temporary operation icon.
4. The new "临时操作" page contains only one feature: the "会场模式" switch.
5. The new "临时操作" entry does not require password protection.

## Requirement 3: Venue Mode State

1. "会场模式" is controlled by an on/off switch.
2. The venue mode state is stored in memory only.
3. After the app restarts, venue mode defaults to off.
4. Turning venue mode off resets the "first venue detection" state.
5. If venue mode is turned off and then turned on again, the next homepage venue detection is treated as the first venue detection and must wait.

## Requirement 4: Homepage Impact

1. Venue mode only affects the homepage.
2. When venue mode is off, the homepage detection button displays "开始检测".
3. When venue mode is on, the homepage detection button displays "会场检测".
4. Other detection entries or flows outside the homepage should not be affected by venue mode.

## Requirement 5: Venue Detection Flow

1. Normal detection flow remains unchanged when venue mode is off.
2. When venue mode is on, the first homepage venue detection after enabling venue mode must keep the existing wait time.
3. After the first venue detection completes, later homepage venue detections should start detection directly without the wait time.
4. If venue mode is turned off and then turned on again, the first homepage venue detection after re-enabling venue mode must wait again.

## Acceptance Criteria

1. The old "临时操作" entry is shown as "工厂测试" with the new factory test icon.
2. Entering "工厂测试" requires password `5988` unless the password was successfully entered within the past 1 hour.
3. The factory test password unlock expires after 1 hour and is cleared by app restart.
4. A new sibling "临时操作" entry exists and uses the original temporary operation icon.
5. The new "临时操作" page contains a working "会场模式" switch.
6. Venue mode is memory-only and defaults to off after app restart.
7. With venue mode on, the homepage button text changes from "开始检测" to "会场检测".
8. Venue mode affects only the homepage detection entry.
9. In venue mode, only the first homepage detection after enabling venue mode waits; subsequent homepage detections skip the wait.
10. Turning venue mode off and on again resets the first-detection wait behavior.
