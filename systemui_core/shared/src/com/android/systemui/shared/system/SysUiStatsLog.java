package com.android.systemui.shared.system;

/**
 * Created by yuchuan
 * DATE 2024/9/26
 * TIME 23:10
 */
public class SysUiStatsLog {

    // Constants for atom codes.

    /**
     * LauncherUIChanged launcher_event<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_EVENT, int action, int src_state, int dst_state, byte[] extension, boolean is_swipe_up_enabled, int event_id, int target_id, int instance_id, int uid, java.lang.String package_name, java.lang.String component_name, int grid_x, int grid_y, int page_id, int grid_x_parent, int grid_y_parent, int page_id_parent, int hierarchy, boolean is_work_profile, int rank, int from_state, int to_state, java.lang.String edittext, int cardinality, int features, int search_attributes, byte[] attributes, int input_type, int user_type, int display_rotation, int recents_orientation_handler);<br>
     */
    public static final int LAUNCHER_EVENT = 19;

    /**
     * KeyguardStateChanged keyguard_state_changed<br>
     * Usage: StatsLog.write(StatsLog.KEYGUARD_STATE_CHANGED, int state);<br>
     */
    public static final int KEYGUARD_STATE_CHANGED = 62;

    /**
     * KeyguardBouncerStateChanged keyguard_bouncer_state_changed<br>
     * Usage: StatsLog.write(StatsLog.KEYGUARD_BOUNCER_STATE_CHANGED, int state);<br>
     */
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED = 63;

    /**
     * KeyguardBouncerPasswordEntered keyguard_bouncer_password_entered<br>
     * Usage: StatsLog.write(StatsLog.KEYGUARD_BOUNCER_PASSWORD_ENTERED, int result, int side);<br>
     */
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED = 64;

    /**
     * UiEventReported ui_event_reported<br>
     * Usage: StatsLog.write(StatsLog.UI_EVENT_REPORTED, int event_id, int uid, java.lang.String package_name, int instance_id);<br>
     */
    public static final int UI_EVENT_REPORTED = 90;

    /**
     * AssistGestureStageReported assist_gesture_stage_reported<br>
     * Usage: StatsLog.write(StatsLog.ASSIST_GESTURE_STAGE_REPORTED, int gesture_stage);<br>
     */
    public static final int ASSIST_GESTURE_STAGE_REPORTED = 174;

    /**
     * AssistGestureFeedbackReported assist_gesture_feedback_reported<br>
     * Usage: StatsLog.write(StatsLog.ASSIST_GESTURE_FEEDBACK_REPORTED, int feedback_type);<br>
     */
    public static final int ASSIST_GESTURE_FEEDBACK_REPORTED = 175;

    /**
     * AssistGestureProgressReported assist_gesture_progress_reported<br>
     * Usage: StatsLog.write(StatsLog.ASSIST_GESTURE_PROGRESS_REPORTED, int progress);<br>
     */
    public static final int ASSIST_GESTURE_PROGRESS_REPORTED = 176;

    /**
     * StyleUIChanged style_ui_changed<br>
     * Usage: StatsLog.write(StatsLog.STYLE_UI_CHANGED, int action, int color_package_hash, int font_package_hash, int shape_package_hash, int clock_package_hash, int launcher_grid, int wallpaper_category_hash, int wallpaper_id_hash, int color_preference, int location_preference, int date_preference, int launched_preference, int effect_preference, int effect_id_hash, int lock_wallpaper_category_hash, int lock_wallpaper_id_hash, int first_launch_date_since_setup, int first_wallpaper_apply_date_since_setup, int app_launch_count, int color_variant, long time_elapsed_millis, int effect_status_code, int app_session_id, int set_wallpaper_entry_point, int wallpaper_destination, int color_source, int seed_color, int clock_size, boolean toggle_on, java.lang.String shortcut, java.lang.String shortcut_slot_id, int lock_effect_id_hash);<br>
     */
    public static final int STYLE_UI_CHANGED = 179;

    /**
     * BackGesture back_gesture_reported_reported<br>
     * Usage: StatsLog.write(StatsLog.BACK_GESTURE_REPORTED_REPORTED, int type, int y_coordinate, int x_location, int start_x, int start_y, int end_x, int end_y, int left_boundary, int right_boundary, float ml_model_score, java.lang.String package_name, int input_type);<br>
     */
    public static final int BACK_GESTURE_REPORTED_REPORTED = 224;

    /**
     * NotificationPanelReported notification_panel_reported<br>
     * Usage: StatsLog.write(StatsLog.NOTIFICATION_PANEL_REPORTED, int event_id, int num_notifications, byte[] notifications);<br>
     */
    public static final int NOTIFICATION_PANEL_REPORTED = 245;

    /**
     * RankingSelected ranking_selected<br>
     * Usage: StatsLog.write(StatsLog.RANKING_SELECTED, int event_id, java.lang.String package_name, int instance_id, int position_picked, boolean is_pinned);<br>
     */
    public static final int RANKING_SELECTED = 260;

    /**
     * LauncherStaticLayout launcher_snapshot<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_SNAPSHOT, int event_id, int target_id, int instance_id, int uid, java.lang.String package_name, java.lang.String component_name, int grid_x, int grid_y, int page_id, int grid_x_parent, int grid_y_parent, int page_id_parent, int hierarchy, boolean is_work_profile, int origin, int cardinality, int span_x, int span_y, int features, byte[] attributes);<br>
     */
    public static final int LAUNCHER_SNAPSHOT = 262;

    /**
     * MediaOutputOpSwitchReported mediaoutput_op_switch_reported<br>
     * Usage: StatsLog.write(StatsLog.MEDIAOUTPUT_OP_SWITCH_REPORTED, int source, int target, int result, int subresult, java.lang.String media_session_package_name, int available_wired_device_count, int available_bt_device_count, int available_remote_device_count, int applied_device_count_within_remote_group, boolean target_is_suggested, boolean target_has_ongoing_session);<br>
     */
    public static final int MEDIAOUTPUT_OP_SWITCH_REPORTED = 277;

    /**
     * ImeTouchReported ime_touch_reported<br>
     * Usage: StatsLog.write(StatsLog.IME_TOUCH_REPORTED, int x_coordinate, int y_coordinate);<br>
     */
    public static final int IME_TOUCH_REPORTED = 304;

    /**
     * DeviceControlChanged device_control_changed<br>
     * Usage: StatsLog.write(StatsLog.DEVICE_CONTROL_CHANGED, int event_id, int instance_id, int device_type, int uid, boolean is_locked);<br>
     */
    public static final int DEVICE_CONTROL_CHANGED = 349;

    /**
     * SmartSpaceCardReported smartspace_card_reported<br>
     * Usage: StatsLog.write(StatsLog.SMARTSPACE_CARD_REPORTED, int event_id, int instance_id, int card_type, int display_surface, int rank, int cardinality, int card_type_id, int uid, int interacted_subcard_rank, int interacted_subcard_cardinality, int received_latency_millis, byte[] subcards_info, byte[] dimensional_info);<br>
     */
    public static final int SMARTSPACE_CARD_REPORTED = 352;

    /**
     * AccessibilityFloatingMenuUIChanged accessibility_floating_menu_ui_changed<br>
     * Usage: StatsLog.write(StatsLog.ACCESSIBILITY_FLOATING_MENU_UI_CHANGED, float normalized_x_position, float normalized_y_position, int orientation);<br>
     */
    public static final int ACCESSIBILITY_FLOATING_MENU_UI_CHANGED = 393;

    /**
     * LauncherLatency launcher_latency<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_LATENCY, int event_id, int instance_id, int package_id, long latency_in_millis, int type, int query_length, int sub_event_type, int cardinality);<br>
     */
    public static final int LAUNCHER_LATENCY = 426;

    /**
     * TaskManagerEventReported task_manager_event_reported<br>
     * Usage: StatsLog.write(StatsLog.TASK_MANAGER_EVENT_REPORTED, int uid, int event, long time_running_ms);<br>
     */
    public static final int TASK_MANAGER_EVENT_REPORTED = 450;

    /**
     * MediaOutputOpInteractionReported mediaoutput_op_interaction_report<br>
     * Usage: StatsLog.write(StatsLog.MEDIAOUTPUT_OP_INTERACTION_REPORT, int interaction_type, int target, java.lang.String media_session_package_name, boolean target_is_suggested);<br>
     */
    public static final int MEDIAOUTPUT_OP_INTERACTION_REPORT = 466;

    /**
     * LauncherImpressionEvent launcher_impression_event<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_IMPRESSION_EVENT, int event_id, int instance_id, int state, int query_length, int[] result_type, int[] result_count, boolean[] is_above_keyboard, int[] uid);<br>
     */
    public static final int LAUNCHER_IMPRESSION_EVENT = 547;

    /**
     * BiometricTouchReported biometric_touch_reported<br>
     * Usage: StatsLog.write(StatsLog.BIOMETRIC_TOUCH_REPORTED, int touch_type, int touch_config_id, int session_id, float x, float y, float minor, float major, float orientation, long time, long gesture_start, boolean is_aod);<br>
     */
    public static final int BIOMETRIC_TOUCH_REPORTED = 577;

    /**
     * DeviceLogAccessEventReported device_log_access_event_reported<br>
     * Usage: StatsLog.write(StatsLog.DEVICE_LOG_ACCESS_EVENT_REPORTED, int uid, int event, long time_running_ms);<br>
     */
    public static final int DEVICE_LOG_ACCESS_EVENT_REPORTED = 592;

    /**
     * LockscreenShortcutSelected lockscreen_shortcut_selected<br>
     * Usage: StatsLog.write(StatsLog.LOCKSCREEN_SHORTCUT_SELECTED, java.lang.String slot_id, java.lang.String affordance_id);<br>
     */
    public static final int LOCKSCREEN_SHORTCUT_SELECTED = 611;

    /**
     * LockscreenShortcutTriggered lockscreen_shortcut_triggered<br>
     * Usage: StatsLog.write(StatsLog.LOCKSCREEN_SHORTCUT_TRIGGERED, java.lang.String slot_id, java.lang.String affordance_id);<br>
     */
    public static final int LOCKSCREEN_SHORTCUT_TRIGGERED = 612;

    /**
     * LauncherImpressionEventV2 launcher_impression_event_v2<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_IMPRESSION_EVENT_V2, int event_id, int instance_id, int state, int query_length, int result_type, boolean is_visible, int uid, int result_source);<br>
     */
    public static final int LAUNCHER_IMPRESSION_EVENT_V2 = 716;

    /**
     * DisplaySwitchLatencyTracked display_switch_latency_tracked<br>
     * Usage: StatsLog.write(StatsLog.DISPLAY_SWITCH_LATENCY_TRACKED, int latency_ms, int from_foldable_device_state, int from_state, int from_focused_app_uid, int from_pip_app_uid, int[] from_visible_apps_uid, int from_density_dpi, int to_state, int to_foldable_device_state, int to_focused_app_uid, int to_pip_app_uid, int[] to_visible_apps_uid, int to_density_dpi, int notification_count, int external_display_count, int throttling_level, int vskin_temperature_c, int hall_sensor_to_first_hinge_angle_change_ms, int hall_sensor_to_device_state_change_ms, int onscreenturningon_to_ondrawn_ms, int ondrawn_to_onscreenturnedon_ms);<br>
     */
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED = 753;

    /**
     * NotificationListenerService notification_listener_service<br>
     * Usage: StatsLog.write(StatsLog.NOTIFICATION_LISTENER_SERVICE, int uid, boolean is_enabled, boolean is_preinstalled, boolean is_pregranted);<br>
     */
    public static final int NOTIFICATION_LISTENER_SERVICE = 829;

    /**
     * NavHandleTouchPoints nav_handle_touch_points<br>
     * Usage: StatsLog.write(StatsLog.NAV_HANDLE_TOUCH_POINTS, int gesture_event_id, int instance_id, int[] x_points, int[] y_points, int[] delay, int[] action);<br>
     */
    public static final int NAV_HANDLE_TOUCH_POINTS = 869;

    /**
     * LauncherLayoutSnapshot launcher_layout_snapshot<br>
     * Usage: StatsLog.write(StatsLog.LAUNCHER_LAYOUT_SNAPSHOT, int event_id, int item_id, int instance_id, int uid, java.lang.String package_name, java.lang.String component_name, int grid_x, int grid_y, int page_id, int grid_x_parent, int grid_y_parent, int page_id_parent, int container_id, boolean is_work_profile, int attribute_id, int cardinality, int span_x, int span_y, byte[] attributes, boolean is_kids_mode);<br>
     */
    public static final int LAUNCHER_LAYOUT_SNAPSHOT = 10108;

    /**
     * NotificationMemoryUse notification_memory_use<br>
     * Usage: StatsLog.write(StatsLog.NOTIFICATION_MEMORY_USE, int uid, int style, int count, int countWithInflatedViews, int smallIconObject, int smallIconBitmapCount, int largeIconObject, int largeIconBitmapCount, int bigPictureObject, int bigPictureBitmapCount, int extras, int extenders, int smallIconViews, int largeIconViews, int systemIconViews, int styleViews, int customViews, int softwareBitmaps, int seenCount);<br>
     */
    public static final int NOTIFICATION_MEMORY_USE = 10174;

    // Constants for enum values.

    // Values for LauncherUIChanged.action
    public static final int LAUNCHER_UICHANGED__ACTION__DEFAULT_ACTION = 0;
    public static final int LAUNCHER_UICHANGED__ACTION__LAUNCH_APP = 1;
    public static final int LAUNCHER_UICHANGED__ACTION__LAUNCH_TASK = 2;
    public static final int LAUNCHER_UICHANGED__ACTION__DISMISS_TASK = 3;
    public static final int LAUNCHER_UICHANGED__ACTION__LONGPRESS = 4;
    public static final int LAUNCHER_UICHANGED__ACTION__DRAGDROP = 5;
    public static final int LAUNCHER_UICHANGED__ACTION__SWIPE_UP = 6;
    public static final int LAUNCHER_UICHANGED__ACTION__SWIPE_DOWN = 7;
    public static final int LAUNCHER_UICHANGED__ACTION__SWIPE_LEFT = 8;
    public static final int LAUNCHER_UICHANGED__ACTION__SWIPE_RIGHT = 9;

    // Values for LauncherUIChanged.src_state
    public static final int LAUNCHER_UICHANGED__SRC_STATE__LAUNCHER_STATE_UNSPECIFIED = 0;
    public static final int LAUNCHER_UICHANGED__SRC_STATE__BACKGROUND = 1;
    public static final int LAUNCHER_UICHANGED__SRC_STATE__HOME = 2;
    public static final int LAUNCHER_UICHANGED__SRC_STATE__OVERVIEW = 3;
    public static final int LAUNCHER_UICHANGED__SRC_STATE__ALLAPPS = 4;
    public static final int LAUNCHER_UICHANGED__SRC_STATE__UNCHANGED = 5;

    // Values for LauncherUIChanged.dst_state
    public static final int LAUNCHER_UICHANGED__DST_STATE__LAUNCHER_STATE_UNSPECIFIED = 0;
    public static final int LAUNCHER_UICHANGED__DST_STATE__BACKGROUND = 1;
    public static final int LAUNCHER_UICHANGED__DST_STATE__HOME = 2;
    public static final int LAUNCHER_UICHANGED__DST_STATE__OVERVIEW = 3;
    public static final int LAUNCHER_UICHANGED__DST_STATE__ALLAPPS = 4;
    public static final int LAUNCHER_UICHANGED__DST_STATE__UNCHANGED = 5;

    // Values for LauncherUIChanged.input_type
    public static final int LAUNCHER_UICHANGED__INPUT_TYPE__UNKNOWN = 0;
    public static final int LAUNCHER_UICHANGED__INPUT_TYPE__TOUCH = 1;
    public static final int LAUNCHER_UICHANGED__INPUT_TYPE__TRACKPAD = 2;

    // Values for LauncherUIChanged.user_type
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_UNKNOWN = 0;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_MAIN = 1;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_WORK = 2;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_CLONED = 3;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_PRIVATE = 4;

    // Values for LauncherUIChanged.display_rotation
    public static final int LAUNCHER_UICHANGED__DISPLAY_ROTATION__ROTATION_0 = 0;
    public static final int LAUNCHER_UICHANGED__DISPLAY_ROTATION__ROTATION_90 = 1;
    public static final int LAUNCHER_UICHANGED__DISPLAY_ROTATION__ROTATION_180 = 2;
    public static final int LAUNCHER_UICHANGED__DISPLAY_ROTATION__ROTATION_270 = 3;

    // Values for LauncherUIChanged.recents_orientation_handler
    public static final int LAUNCHER_UICHANGED__RECENTS_ORIENTATION_HANDLER__PORTRAIT = 0;
    public static final int LAUNCHER_UICHANGED__RECENTS_ORIENTATION_HANDLER__LANDSCAPE = 1;
    public static final int LAUNCHER_UICHANGED__RECENTS_ORIENTATION_HANDLER__SEASCAPE = 2;

    // Values for KeyguardStateChanged.state
    public static final int KEYGUARD_STATE_CHANGED__STATE__UNKNOWN = 0;
    public static final int KEYGUARD_STATE_CHANGED__STATE__HIDDEN = 1;
    public static final int KEYGUARD_STATE_CHANGED__STATE__SHOWN = 2;
    public static final int KEYGUARD_STATE_CHANGED__STATE__OCCLUDED = 3;

    // Values for KeyguardBouncerStateChanged.state
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__UNKNOWN = 0;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__HIDDEN = 1;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__SHOWN = 2;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__SHOWN_LEFT = 3;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__SHOWN_RIGHT = 4;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__SWITCH_LEFT = 5;
    public static final int KEYGUARD_BOUNCER_STATE_CHANGED__STATE__SWITCH_RIGHT = 6;

    // Values for KeyguardBouncerPasswordEntered.result
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__RESULT__UNKNOWN = 0;
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__RESULT__FAILURE = 1;
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__RESULT__SUCCESS = 2;

    // Values for KeyguardBouncerPasswordEntered.side
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__SIDE__DEFAULT = 0;
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__SIDE__LEFT = 1;
    public static final int KEYGUARD_BOUNCER_PASSWORD_ENTERED__SIDE__RIGHT = 2;

    // Values for AssistGestureStageReported.gesture_stage
    public static final int ASSIST_GESTURE_STAGE_REPORTED__GESTURE_STAGE__ASSIST_GESTURE_STAGE_UNKNOWN = 0;
    public static final int ASSIST_GESTURE_STAGE_REPORTED__GESTURE_STAGE__ASSIST_GESTURE_STAGE_PROGRESS = 1;
    public static final int ASSIST_GESTURE_STAGE_REPORTED__GESTURE_STAGE__ASSIST_GESTURE_STAGE_PRIMED = 2;
    public static final int ASSIST_GESTURE_STAGE_REPORTED__GESTURE_STAGE__ASSIST_GESTURE_STAGE_DETECTED = 3;

    // Values for AssistGestureFeedbackReported.feedback_type
    public static final int ASSIST_GESTURE_FEEDBACK_REPORTED__FEEDBACK_TYPE__ASSIST_GESTURE_FEEDBACK_UNKNOWN = 0;
    public static final int ASSIST_GESTURE_FEEDBACK_REPORTED__FEEDBACK_TYPE__ASSIST_GESTURE_FEEDBACK_NOT_USED = 1;
    public static final int ASSIST_GESTURE_FEEDBACK_REPORTED__FEEDBACK_TYPE__ASSIST_GESTURE_FEEDBACK_USED = 2;

    // Values for StyleUIChanged.action
    public static final int STYLE_UICHANGED__ACTION__DEFAULT_ACTION = 0;
    public static final int STYLE_UICHANGED__ACTION__ONRESUME = 1;
    public static final int STYLE_UICHANGED__ACTION__ONSTOP = 2;
    public static final int STYLE_UICHANGED__ACTION__PICKER_SELECT = 3;
    public static final int STYLE_UICHANGED__ACTION__PICKER_APPLIED = 4;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_OPEN_CATEGORY = 5;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_SELECT = 6;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_APPLIED = 7;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_EXPLORE = 8;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_DOWNLOAD = 9;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_REMOVE = 10;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_DOWNLOAD_SUCCESS = 11;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_DOWNLOAD_FAILED = 12;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_DOWNLOAD_CANCELLED = 13;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_DELETE_SUCCESS = 14;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_DELETE_FAILED = 15;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_APPLIED = 16;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_INFO_SELECT = 17;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_CUSTOMIZE_SELECT = 18;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_QUESTIONNAIRE_SELECT = 19;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_QUESTIONNAIRE_APPLIED = 20;
    public static final int STYLE_UICHANGED__ACTION__LIVE_WALLPAPER_EFFECT_SHOW = 21;
    public static final int STYLE_UICHANGED__ACTION__APP_LAUNCHED = 22;
    public static final int STYLE_UICHANGED__ACTION__COLOR_WALLPAPER_HOME_APPLIED = 23;
    public static final int STYLE_UICHANGED__ACTION__COLOR_WALLPAPER_LOCK_APPLIED = 24;
    public static final int STYLE_UICHANGED__ACTION__COLOR_WALLPAPER_HOME_LOCK_APPLIED = 25;
    public static final int STYLE_UICHANGED__ACTION__COLOR_PRESET_APPLIED = 26;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_EFFECT_APPLIED = 27;
    public static final int STYLE_UICHANGED__ACTION__SNAPSHOT = 28;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_EFFECT_PROBE = 29;
    public static final int STYLE_UICHANGED__ACTION__WALLPAPER_EFFECT_FG_DOWNLOAD = 30;
    public static final int STYLE_UICHANGED__ACTION__THEME_COLOR_APPLIED = 31;
    public static final int STYLE_UICHANGED__ACTION__GRID_APPLIED = 32;
    public static final int STYLE_UICHANGED__ACTION__CLOCK_APPLIED = 33;
    public static final int STYLE_UICHANGED__ACTION__CLOCK_COLOR_APPLIED = 34;
    public static final int STYLE_UICHANGED__ACTION__CLOCK_SIZE_APPLIED = 35;
    public static final int STYLE_UICHANGED__ACTION__THEMED_ICON_APPLIED = 36;
    public static final int STYLE_UICHANGED__ACTION__LOCK_SCREEN_NOTIFICATION_APPLIED = 37;
    public static final int STYLE_UICHANGED__ACTION__SHORTCUT_APPLIED = 38;
    public static final int STYLE_UICHANGED__ACTION__DARK_THEME_APPLIED = 39;
    public static final int STYLE_UICHANGED__ACTION__RESET_APPLIED = 40;

    // Values for StyleUIChanged.location_preference
    public static final int STYLE_UICHANGED__LOCATION_PREFERENCE__LOCATION_PREFERENCE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__LOCATION_PREFERENCE__LOCATION_UNAVAILABLE = 1;
    public static final int STYLE_UICHANGED__LOCATION_PREFERENCE__LOCATION_CURRENT = 2;
    public static final int STYLE_UICHANGED__LOCATION_PREFERENCE__LOCATION_MANUAL = 3;

    // Values for StyleUIChanged.date_preference
    public static final int STYLE_UICHANGED__DATE_PREFERENCE__DATE_PREFERENCE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__DATE_PREFERENCE__DATE_UNAVAILABLE = 1;
    public static final int STYLE_UICHANGED__DATE_PREFERENCE__DATE_MANUAL = 2;

    // Values for StyleUIChanged.launched_preference
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_PREFERENCE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_LAUNCHER = 1;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SETTINGS = 2;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SUW = 3;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_TIPS = 4;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_LAUNCH_ICON = 5;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_CROP_AND_SET_ACTION = 6;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_DEEP_LINK = 7;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_SETTINGS_SEARCH = 8;
    public static final int STYLE_UICHANGED__LAUNCHED_PREFERENCE__LAUNCHED_KEYGUARD = 9;

    // Values for StyleUIChanged.effect_preference
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_PREFERENCE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_APPLIED_ON_SUCCESS = 1;
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_APPLIED_ON_FAILED = 2;
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_APPLIED_OFF = 3;
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_APPLIED_ABORTED = 4;
    public static final int STYLE_UICHANGED__EFFECT_PREFERENCE__EFFECT_APPLIED_STARTED = 5;

    // Values for StyleUIChanged.set_wallpaper_entry_point
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW = 1;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_WALLPAPER_QUICK_SWITCHER = 2;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_LAUNCHER_WALLPAPER_QUICK_SWITCHER = 3;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_ROTATION_WALLPAPER = 4;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_RESET = 5;
    public static final int STYLE_UICHANGED__SET_WALLPAPER_ENTRY_POINT__SET_WALLPAPER_ENTRY_POINT_RESTORE = 6;

    // Values for StyleUIChanged.wallpaper_destination
    public static final int STYLE_UICHANGED__WALLPAPER_DESTINATION__WALLPAPER_DESTINATION_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__WALLPAPER_DESTINATION__WALLPAPER_DESTINATION_HOME_SCREEN = 1;
    public static final int STYLE_UICHANGED__WALLPAPER_DESTINATION__WALLPAPER_DESTINATION_LOCK_SCREEN = 2;
    public static final int STYLE_UICHANGED__WALLPAPER_DESTINATION__WALLPAPER_DESTINATION_HOME_AND_LOCK_SCREEN = 3;

    // Values for StyleUIChanged.color_source
    public static final int STYLE_UICHANGED__COLOR_SOURCE__COLOR_SOURCE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__COLOR_SOURCE__COLOR_SOURCE_HOME_SCREEN_WALLPAPER = 1;
    public static final int STYLE_UICHANGED__COLOR_SOURCE__COLOR_SOURCE_LOCK_SCREEN_WALLPAPER = 2;
    public static final int STYLE_UICHANGED__COLOR_SOURCE__COLOR_SOURCE_PRESET_COLOR = 3;

    // Values for StyleUIChanged.clock_size
    public static final int STYLE_UICHANGED__CLOCK_SIZE__CLOCK_SIZE_UNSPECIFIED = 0;
    public static final int STYLE_UICHANGED__CLOCK_SIZE__CLOCK_SIZE_DYNAMIC = 1;
    public static final int STYLE_UICHANGED__CLOCK_SIZE__CLOCK_SIZE_SMALL = 2;

    // Values for BackGesture.type
    public static final int BACK_GESTURE__TYPE__DEFAULT_BACK_TYPE = 0;
    public static final int BACK_GESTURE__TYPE__COMPLETED = 1;
    public static final int BACK_GESTURE__TYPE__COMPLETED_REJECTED = 2;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE_EXCLUDED = 3;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE = 4;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE_FAR_FROM_EDGE = 5;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE_MULTI_TOUCH = 6;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE_LONG_PRESS = 7;
    public static final int BACK_GESTURE__TYPE__INCOMPLETE_VERTICAL_MOVE = 8;

    // Values for BackGesture.x_location
    public static final int BACK_GESTURE__X_LOCATION__DEFAULT_LOCATION = 0;
    public static final int BACK_GESTURE__X_LOCATION__LEFT = 1;
    public static final int BACK_GESTURE__X_LOCATION__RIGHT = 2;

    // Values for BackGesture.input_type
    public static final int BACK_GESTURE__INPUT_TYPE__UNKNOWN = 0;
    public static final int BACK_GESTURE__INPUT_TYPE__TOUCH = 1;
    public static final int BACK_GESTURE__INPUT_TYPE__TRACKPAD = 2;

    // Values for MediaOutputOpSwitchReported.source
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__UNKNOWN_TYPE = 0;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BUILTIN_SPEAKER = 1;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__WIRED_3POINT5_MM_AUDIO = 100;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__WIRED_3POINT5_MM_HEADSET = 101;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__WIRED_3POINT5_MM_HEADPHONES = 102;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_AUDIO = 200;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_DEVICE = 201;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_HEADSET = 202;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_ACCESSORY = 203;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_DOCK = 204;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__USB_C_HDMI = 205;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BLUETOOTH = 300;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BLUETOOTH_HEARING_AID = 301;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__BLUETOOTH_A2DP = 302;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_SINGLE = 400;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_TV = 401;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_SPEAKER = 402;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_GROUP = 500;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__REMOTE_DYNAMIC_GROUP = 501;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SOURCE__AVR = 600;

    // Values for MediaOutputOpSwitchReported.target
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__UNKNOWN_TYPE = 0;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BUILTIN_SPEAKER = 1;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__WIRED_3POINT5_MM_AUDIO = 100;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__WIRED_3POINT5_MM_HEADSET = 101;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__WIRED_3POINT5_MM_HEADPHONES = 102;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_AUDIO = 200;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_DEVICE = 201;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_HEADSET = 202;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_ACCESSORY = 203;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_DOCK = 204;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__USB_C_HDMI = 205;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BLUETOOTH = 300;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BLUETOOTH_HEARING_AID = 301;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__BLUETOOTH_A2DP = 302;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_SINGLE = 400;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_TV = 401;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_SPEAKER = 402;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_GROUP = 500;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__REMOTE_DYNAMIC_GROUP = 501;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__TARGET__AVR = 600;

    // Values for MediaOutputOpSwitchReported.result
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__RESULT__ERROR = 0;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__RESULT__OK = 1;

    // Values for MediaOutputOpSwitchReported.subresult
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__UNKNOWN_ERROR = 0;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__NO_ERROR = 1;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__REJECTED = 2;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__NETWORK_ERROR = 3;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__ROUTE_NOT_AVAILABLE = 4;
    public static final int MEDIA_OUTPUT_OP_SWITCH_REPORTED__SUBRESULT__INVALID_COMMAND = 5;

    // Values for SmartSpaceCardReported.card_type
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__UNKNOWN_CARD = 0;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__COMMUTE = 1;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__CALENDAR = 2;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__FLIGHT = 3;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__WEATHER = 4;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__WEATHER_ALERT = 5;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__AT_A_STORE_SHOPPING_LIST = 6;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__AT_A_STORE_LOYALTY_CARD = 7;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__HEADPHONE_RESUME_MEDIA = 8;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__HEADPHONE_MEDIA_RECOMMENDATIONS = 9;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__TIMER = 10;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__STOPWATCH = 11;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__FITNESS_ACTIVITY = 12;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__UPCOMING_REMINDER = 13;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__UPCOMING_BEDTIME = 14;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__TIME_TO_LEAVE = 15;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__PACKAGE_DELIVERED = 16;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__TIPS = 17;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__DOORBELL = 18;
    public static final int SMART_SPACE_CARD_REPORTED__CARD_TYPE__CROSS_DEVICE_TIMER = 19;

    // Values for SmartSpaceCardReported.display_surface
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__DEFAULT_SURFACE = 0;
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__HOMESCREEN = 1;
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__LOCKSCREEN = 2;
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__AOD = 3;
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__SHADE = 4;
    public static final int SMART_SPACE_CARD_REPORTED__DISPLAY_SURFACE__DREAM_OVERLAY = 5;

    // Values for AccessibilityFloatingMenuUIChanged.orientation
    public static final int ACCESSIBILITY_FLOATING_MENU_UICHANGED__ORIENTATION__UNKNOWN = 0;
    public static final int ACCESSIBILITY_FLOATING_MENU_UICHANGED__ORIENTATION__PORTRAIT = 1;
    public static final int ACCESSIBILITY_FLOATING_MENU_UICHANGED__ORIENTATION__LANDSCAPE = 2;

    // Values for TaskManagerEventReported.event
    public static final int TASK_MANAGER_EVENT_REPORTED__EVENT__VIEWED = 1;
    public static final int TASK_MANAGER_EVENT_REPORTED__EVENT__STOPPED = 2;

    // Values for MediaOutputOpInteractionReported.interaction_type
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__INTERACTION_TYPE__EXPANSION = 0;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__INTERACTION_TYPE__ADJUST_VOLUME = 1;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__INTERACTION_TYPE__STOP_CASTING = 2;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__INTERACTION_TYPE__MUTE = 3;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__INTERACTION_TYPE__UNMUTE = 4;

    // Values for MediaOutputOpInteractionReported.target
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__UNKNOWN_TYPE = 0;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__BUILTIN_SPEAKER = 1;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__WIRED_3POINT5_MM_AUDIO = 100;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__WIRED_3POINT5_MM_HEADSET = 101;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__WIRED_3POINT5_MM_HEADPHONES = 102;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_AUDIO = 200;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_DEVICE = 201;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_HEADSET = 202;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_ACCESSORY = 203;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_DOCK = 204;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__USB_C_HDMI = 205;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__BLUETOOTH = 300;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__BLUETOOTH_HEARING_AID = 301;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__BLUETOOTH_A2DP = 302;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__REMOTE_SINGLE = 400;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__REMOTE_TV = 401;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__REMOTE_SPEAKER = 402;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__REMOTE_GROUP = 500;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__REMOTE_DYNAMIC_GROUP = 501;
    public static final int MEDIA_OUTPUT_OP_INTERACTION_REPORTED__TARGET__AVR = 600;

    // Values for BiometricTouchReported.touch_type
    public static final int BIOMETRIC_TOUCH_REPORTED__TOUCH_TYPE__TOUCH_TYPE_UNCHANGED = 0;
    public static final int BIOMETRIC_TOUCH_REPORTED__TOUCH_TYPE__TOUCH_TYPE_DOWN = 1;
    public static final int BIOMETRIC_TOUCH_REPORTED__TOUCH_TYPE__TOUCH_TYPE_UP = 2;
    public static final int BIOMETRIC_TOUCH_REPORTED__TOUCH_TYPE__TOUCH_TYPE_CANCEL = 3;

    // Values for DeviceLogAccessEventReported.event
    public static final int DEVICE_LOG_ACCESS_EVENT_REPORTED__EVENT__UNKNOWN = 0;
    public static final int DEVICE_LOG_ACCESS_EVENT_REPORTED__EVENT__ALLOWED = 1;
    public static final int DEVICE_LOG_ACCESS_EVENT_REPORTED__EVENT__DENIED = 2;

    // Values for DisplaySwitchLatencyTracked.from_foldable_device_state
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_FOLDABLE_DEVICE_STATE__STATE_UNKNOWN = 0;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_FOLDABLE_DEVICE_STATE__STATE_CLOSED = 1;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_FOLDABLE_DEVICE_STATE__STATE_HALF_OPENED = 2;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_FOLDABLE_DEVICE_STATE__STATE_OPENED = 3;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_FOLDABLE_DEVICE_STATE__STATE_FLIPPED = 4;

    // Values for DisplaySwitchLatencyTracked.from_state
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__UNKNOWN = 0;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__AOD = 1;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__LOCKSCREEN = 2;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__NOTIFICATION_SHADE = 3;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__FULL_SCREEN_APP = 4;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__SPLIT_SCREEN_APPS = 5;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__LAUNCHER = 6;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__OVERVIEW = 7;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__FREEFORM_APPS = 8;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__FROM_STATE__SCREEN_OFF = 9;

    // Values for DisplaySwitchLatencyTracked.to_state
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__UNKNOWN = 0;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__AOD = 1;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__LOCKSCREEN = 2;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__NOTIFICATION_SHADE = 3;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__FULL_SCREEN_APP = 4;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__SPLIT_SCREEN_APPS = 5;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__LAUNCHER = 6;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__OVERVIEW = 7;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__FREEFORM_APPS = 8;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_STATE__SCREEN_OFF = 9;

    // Values for DisplaySwitchLatencyTracked.to_foldable_device_state
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_FOLDABLE_DEVICE_STATE__STATE_UNKNOWN = 0;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_FOLDABLE_DEVICE_STATE__STATE_CLOSED = 1;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_FOLDABLE_DEVICE_STATE__STATE_HALF_OPENED = 2;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_FOLDABLE_DEVICE_STATE__STATE_OPENED = 3;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__TO_FOLDABLE_DEVICE_STATE__STATE_FLIPPED = 4;

    // Values for DisplaySwitchLatencyTracked.throttling_level
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__NONE = 0;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__LIGHT = 1;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__MODERATE = 2;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__SEVERE = 3;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__CRITICAL = 4;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__EMERGENCY = 5;
    public static final int DISPLAY_SWITCH_LATENCY_TRACKED__THROTTLING_LEVEL__SHUTDOWN = 6;

    // Annotation constants.
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_IS_UID = StatsLog.ANNOTATION_ID_IS_UID;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_TRUNCATE_TIMESTAMP = StatsLog.ANNOTATION_ID_TRUNCATE_TIMESTAMP;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_PRIMARY_FIELD = StatsLog.ANNOTATION_ID_PRIMARY_FIELD;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_EXCLUSIVE_STATE = StatsLog.ANNOTATION_ID_EXCLUSIVE_STATE;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_PRIMARY_FIELD_FIRST_UID = StatsLog.ANNOTATION_ID_PRIMARY_FIELD_FIRST_UID;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_DEFAULT_STATE = StatsLog.ANNOTATION_ID_DEFAULT_STATE;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_TRIGGER_STATE_RESET = StatsLog.ANNOTATION_ID_TRIGGER_STATE_RESET;
//
//    @android.annotation.SuppressLint("InlinedApi")
//    public static final byte ANNOTATION_ID_STATE_NESTED = StatsLog.ANNOTATION_ID_STATE_NESTED;
//
//
//    // Write methods
//    public static void write(int code, int arg1) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, boolean arg2, boolean arg3, boolean arg4) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        if (NOTIFICATION_LISTENER_SERVICE == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeBoolean(arg2);
//        builder.writeBoolean(arg3);
//        builder.writeBoolean(arg4);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, byte[] arg3) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeByteArray(null == arg3 ? new byte[0] : arg3);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, byte[] arg4, boolean arg5, int arg6, int arg7, int arg8, int arg9, java.lang.String arg10, java.lang.String arg11, int arg12, int arg13, int arg14, int arg15, int arg16, int arg17, int arg18, boolean arg19, int arg20, int arg21, int arg22, java.lang.String arg23, int arg24, int arg25, int arg26, byte[] arg27, int arg28, int arg29, int arg30, int arg31) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeByteArray(null == arg4 ? new byte[0] : arg4);
//        builder.writeBoolean(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        if (LAUNCHER_EVENT == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeString(arg10);
//        builder.writeString(arg11);
//        builder.writeInt(arg12);
//        builder.writeInt(arg13);
//        builder.writeInt(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeBoolean(arg19);
//        builder.writeInt(arg20);
//        builder.writeInt(arg21);
//        builder.writeInt(arg22);
//        builder.writeString(arg23);
//        builder.writeInt(arg24);
//        builder.writeInt(arg25);
//        builder.writeInt(arg26);
//        builder.writeByteArray(null == arg27 ? new byte[0] : arg27);
//        builder.writeInt(arg28);
//        builder.writeInt(arg29);
//        builder.writeInt(arg30);
//        builder.writeInt(arg31);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, boolean arg5) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        if (DEVICE_CONTROL_CHANGED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeBoolean(arg5);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int arg5, boolean arg6, int arg7, int arg8) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeInt(arg5);
//        builder.writeBoolean(arg6);
//        builder.writeInt(arg7);
//        if (LAUNCHER_IMPRESSION_EVENT_V2 == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg8);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9, int arg10, int arg11, byte[] arg12, byte[] arg13) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeInt(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        if (SMARTSPACE_CARD_REPORTED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        builder.writeInt(arg11);
//        builder.writeByteArray(null == arg12 ? new byte[0] : arg12);
//        builder.writeByteArray(null == arg13 ? new byte[0] : arg13);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9, int arg10, int arg11, int arg12, int arg13, int arg14, int arg15, int arg16, int arg17, int arg18, int arg19, int arg20, long arg21, int arg22, int arg23, int arg24, int arg25, int arg26, int arg27, int arg28, boolean arg29, java.lang.String arg30, java.lang.String arg31, int arg32) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeInt(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        builder.writeInt(arg11);
//        builder.writeInt(arg12);
//        builder.writeInt(arg13);
//        builder.writeInt(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeInt(arg19);
//        builder.writeInt(arg20);
//        builder.writeLong(arg21);
//        builder.writeInt(arg22);
//        builder.writeInt(arg23);
//        builder.writeInt(arg24);
//        builder.writeInt(arg25);
//        builder.writeInt(arg26);
//        builder.writeInt(arg27);
//        builder.writeInt(arg28);
//        builder.writeBoolean(arg29);
//        builder.writeString(arg30);
//        builder.writeString(arg31);
//        builder.writeInt(arg32);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9, float arg10, java.lang.String arg11, int arg12) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeInt(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeFloat(arg10);
//        builder.writeString(arg11);
//        builder.writeInt(arg12);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    @android.annotation.SuppressLint("ObsoleteSdkInt")
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int arg5, int[] arg6, int arg7, int arg8, int arg9, int arg10, int arg11, int[] arg12, int arg13, int arg14, int arg15, int arg16, int arg17, int arg18, int arg19, int arg20, int arg21) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg5);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeIntArray(null == arg6 ? new int[0] : arg6);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg11);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeIntArray(null == arg12 ? new int[0] : arg12);
//        if (DISPLAY_SWITCH_LATENCY_TRACKED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg13);
//        builder.writeInt(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeInt(arg19);
//        builder.writeInt(arg20);
//        builder.writeInt(arg21);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, java.lang.String arg5, int arg6, int arg7, int arg8, int arg9, boolean arg10, boolean arg11) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeString(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeBoolean(arg10);
//        builder.writeBoolean(arg11);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, java.lang.String arg5, java.lang.String arg6, int arg7, int arg8, int arg9, int arg10, int arg11, int arg12, int arg13, boolean arg14, int arg15, int arg16, int arg17, int arg18, int arg19, byte[] arg20) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        if (LAUNCHER_SNAPSHOT == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeString(arg5);
//        builder.writeString(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        builder.writeInt(arg11);
//        builder.writeInt(arg12);
//        builder.writeInt(arg13);
//        builder.writeBoolean(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeInt(arg19);
//        builder.writeByteArray(null == arg20 ? new byte[0] : arg20);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    @android.annotation.SuppressLint("ObsoleteSdkInt")
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    public static void write(int code, int arg1, int arg2, int arg3, int arg4, int[] arg5, int[] arg6, boolean[] arg7, int[] arg8) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeIntArray(null == arg5 ? new int[0] : arg5);
//        builder.writeIntArray(null == arg6 ? new int[0] : arg6);
//        builder.writeBooleanArray(null == arg7 ? new boolean[0] : arg7);
//        builder.writeIntArray(null == arg8 ? new int[0] : arg8);
//        if (LAUNCHER_IMPRESSION_EVENT == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, long arg4, int arg5, int arg6, int arg7, int arg8) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeLong(arg4);
//        builder.writeInt(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, int arg3, float arg4, float arg5, float arg6, float arg7, float arg8, long arg9, long arg10, boolean arg11) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeFloat(arg4);
//        builder.writeFloat(arg5);
//        builder.writeFloat(arg6);
//        builder.writeFloat(arg7);
//        builder.writeFloat(arg8);
//        builder.writeLong(arg9);
//        builder.writeLong(arg10);
//        builder.writeBoolean(arg11);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, long arg3) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        if (TASK_MANAGER_EVENT_REPORTED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        if (DEVICE_LOG_ACCESS_EVENT_REPORTED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeInt(arg2);
//        builder.writeLong(arg3);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, java.lang.String arg3, boolean arg4) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeString(arg3);
//        builder.writeBoolean(arg4);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, int arg2, java.lang.String arg3, int arg4) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        if (UI_EVENT_REPORTED == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeString(arg3);
//        builder.writeInt(arg4);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    @android.annotation.SuppressLint("ObsoleteSdkInt")
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    public static void write(int code, int arg1, int arg2, int[] arg3, int[] arg4, int[] arg5, int[] arg6) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeIntArray(null == arg3 ? new int[0] : arg3);
//        builder.writeIntArray(null == arg4 ? new int[0] : arg4);
//        builder.writeIntArray(null == arg5 ? new int[0] : arg5);
//        builder.writeIntArray(null == arg6 ? new int[0] : arg6);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, int arg1, java.lang.String arg2, int arg3, int arg4, boolean arg5) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeString(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeBoolean(arg5);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, float arg1, float arg2, int arg3) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeFloat(arg1);
//        builder.writeFloat(arg2);
//        builder.writeInt(arg3);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static void write(int code, java.lang.String arg1, java.lang.String arg2) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeString(arg1);
//        builder.writeString(arg2);
//
//        builder.usePooledBuffer();
//        StatsLog.write(builder.build());
//    }
//
//    public static StatsEvent buildStatsEvent(int code, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9, int arg10, int arg11, int arg12, int arg13, int arg14, int arg15, int arg16, int arg17, int arg18, int arg19) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        builder.writeInt(arg5);
//        builder.writeInt(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        builder.writeInt(arg11);
//        builder.writeInt(arg12);
//        builder.writeInt(arg13);
//        builder.writeInt(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeInt(arg19);
//
//        return builder.build();
//    }
//
//    public static StatsEvent buildStatsEvent(int code, int arg1, int arg2, int arg3, int arg4, java.lang.String arg5, java.lang.String arg6, int arg7, int arg8, int arg9, int arg10, int arg11, int arg12, int arg13, boolean arg14, int arg15, int arg16, int arg17, int arg18, byte[] arg19, boolean arg20) {
//        final StatsEvent.Builder builder = StatsEvent.newBuilder();
//        builder.setAtomId(code);
//        builder.writeInt(arg1);
//        builder.writeInt(arg2);
//        builder.writeInt(arg3);
//        builder.writeInt(arg4);
//        if (LAUNCHER_LAYOUT_SNAPSHOT == code) {
//            builder.addBooleanAnnotation(ANNOTATION_ID_IS_UID, true);
//        }
//        builder.writeString(arg5);
//        builder.writeString(arg6);
//        builder.writeInt(arg7);
//        builder.writeInt(arg8);
//        builder.writeInt(arg9);
//        builder.writeInt(arg10);
//        builder.writeInt(arg11);
//        builder.writeInt(arg12);
//        builder.writeInt(arg13);
//        builder.writeBoolean(arg14);
//        builder.writeInt(arg15);
//        builder.writeInt(arg16);
//        builder.writeInt(arg17);
//        builder.writeInt(arg18);
//        builder.writeByteArray(null == arg19 ? new byte[0] : arg19);
//        builder.writeBoolean(arg20);
//
//        return builder.build();
//    }
}
