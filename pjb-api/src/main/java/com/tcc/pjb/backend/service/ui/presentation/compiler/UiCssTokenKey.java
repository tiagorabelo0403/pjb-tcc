package com.tcc.pjb.backend.service.ui.presentation.compiler;

public enum UiCssTokenKey {
  BG("--pjb-bg"),
  SURFACE("--pjb-surface"),
  TEXT("--pjb-text"),
  MUTED("--pjb-muted"),
  LINK("--pjb-link"),
  BORDER("--pjb-border"),
  FOCUS("--pjb-focus"),
  BORDER_WIDTH("--pjb-border-width"),
  FOCUS_WIDTH("--pjb-focus-width"),
  RADIUS("--pjb-radius"),
  SHADOW_OPACITY("--pjb-shadow-opacity"),
  MOTION_FACTOR("--pjb-motion-factor"),
  FONT_SCALE("--pjb-font-scale"),
  LINE_HEIGHT("--pjb-line-height"),
  PARAGRAPH_GAP("--pjb-paragraph-gap"),
  LETTER_SPACING("--pjb-letter-spacing"),
  CONTENT_MAX_WIDTH("--pjb-content-max-width"),
  UNDERLINE_THICKNESS("--pjb-underline-thickness"),

  
  CHAT_BG("--pjb-chat-bg"),
  CHAT_IN_BUBBLE_BG("--pjb-chat-in-bubble-bg"),
  CHAT_OUT_BUBBLE_BG("--pjb-chat-out-bubble-bg"),
  CHAT_IN_TEXT("--pjb-chat-in-text"),
  CHAT_OUT_TEXT("--pjb-chat-out-text"),
  CHAT_ACCENT("--pjb-chat-accent"),
  CHAT_INPUT_BG("--pjb-chat-input-bg"),
  CHAT_DIVIDER("--pjb-chat-divider"),
  CHAT_BUBBLE_RADIUS("--pjb-chat-bubble-radius"),

  CHAT_ATTACH_ENABLED("--pjb-chat-attach-enabled"),
  CHAT_ATTACH_MAX_BYTES("--pjb-chat-attach-max-bytes"),
  CHAT_ATTACH_MAX_PER_MESSAGE("--pjb-chat-attach-max-per-message"),

  
  NOTIFY_BADGE_BG("--pjb-notify-badge-bg"),
  NOTIFY_BADGE_TEXT("--pjb-notify-badge-text"),
  NOTIFY_PULSE_MS("--pjb-notify-pulse-ms"),
  NOTIFY_SHAKE_MS("--pjb-notify-shake-ms"),
  NOTIFY_SHAKE_DEG("--pjb-notify-shake-deg"),
  NOTIFY_PULSE_SCALE("--pjb-notify-pulse-scale"),

  
  WATERMARK_TEXT("--pjb-watermark-text"),
  WATERMARK_OPACITY("--pjb-watermark-opacity"),
  WATERMARK_ROTATE("--pjb-watermark-rotate");

  private final String css;

  UiCssTokenKey(String css) {
    this.css = css;
  }

  public String css() {
    return css;
  }

  public String key() {
    return css;
  }
}
