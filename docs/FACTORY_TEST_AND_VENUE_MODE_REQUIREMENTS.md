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

## Requirement 6: Test Mode in Temporary Operation

1. Add a "测试模式" switch to the "临时操作" page.
2. "测试模式" and "会场模式" are mutually exclusive.
3. Turning on "测试模式" should turn off "会场模式".
4. Turning on "会场模式" should turn off "测试模式".
5. When "测试模式" is turned on, automatically show a numeric input dialog for configuring "反应时间".
6. "反应时间" is configured in seconds.
7. The default "反应时间" is `300` seconds.
8. "吸水时间" is configured in milliseconds, valid range `1500` to `100000`, default `3000`.
9. "扫描时间" is configured in milliseconds, valid range `1` to `60000`, default `8000`.
10. "激光强度" has no unit, valid range `-100` to `0`, default `-25`.
11. The "测试模式" configuration must be stored in the local database.
12. When "测试模式" is enabled, the homepage detection button image changes to `test_mode_btn.png`.
13. `test_mode_btn.png` is already available in the app resource folders.
14. In "测试模式", the detection flow skips QR code validation.
15. In "测试模式", the detection flow skips reagent card binding validation.
16. In "测试模式", the detection flow still fetches detection configuration from the server.
17. In "测试模式", after downloading the server configuration, directly override `xt1` with the local absorb time, `cut_off1` with the local laser strength, and `cut_off2` with the local reaction time.
18. In "测试模式", subsequent absorb, laser, and reaction logic must read the overridden card configuration instead of applying test-mode overrides at each use site.
19. In "测试模式", after reaction and scan finish, do not call `postKinoResult` or `postBiomarkers`.
20. In "测试模式", set the result type to `TYPE_BIOAGE_CRP` before generating local result data.
21. In "测试模式", the final result should navigate directly to `RouteConfig.REPORT_DETAIL` to show the chart.
22. When "测试模式" is enabled, show the configured test mode parameters as text below the "测试模式" switch.
23. When "测试模式" is disabled, hide the test mode parameter text.

## Test Mode Acceptance Criteria

1. The "临时操作" page contains both "会场模式" and "测试模式".
2. Only one of "会场模式" and "测试模式" can be enabled at the same time.
3. Enabling "测试模式" opens a seconds-based numeric input dialog for "反应时间".
4. If the user does not change the value, "反应时间" defaults to `300` seconds.
5. The configured test mode parameters persist in the local database.
6. With "测试模式" enabled, the homepage button uses `test_mode_btn.png`.
7. With "测试模式" enabled, QR code validation is skipped.
8. With "测试模式" enabled, reagent card binding validation is skipped.
9. With "测试模式" enabled, server-side detection configuration fetching still runs.
10. With "测试模式" enabled, the local database absorb time overrides the server-fetched `xt1` value.
11. With "测试模式" enabled, the local database laser strength overrides the server-fetched `cut_off1` value.
12. With "测试模式" enabled, the local database reaction time overrides the server-fetched `cut_off2` value.
13. With "测试模式" enabled, the scan result does not upload to Nano `postKinoResult` or `postBiomarkers`.
14. With "测试模式" enabled, the local result type is `TYPE_BIOAGE_CRP`.
15. With "测试模式" enabled, the final result navigates directly to `RouteConfig.REPORT_DETAIL`.
16. With "测试模式" enabled, the page shows the configured test mode parameters below the "测试模式" switch.
17. With "测试模式" disabled, the test mode parameter text is not shown.
