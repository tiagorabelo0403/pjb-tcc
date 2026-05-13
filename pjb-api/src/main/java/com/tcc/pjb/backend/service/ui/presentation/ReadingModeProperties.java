package com.tcc.pjb.backend.service.ui.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import lombok.Getter;
import lombok.Setter;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "pjb.ui.readingmode")
public class ReadingModeProperties {

  private boolean enabledByDefault = true;

  @NotNull
  private UiReadingIntensity defaultIntensity = UiReadingIntensity.SOFT;

  private boolean applyToAllPages = true;

  @Valid
  private Intensity soft = Intensity.softDefaults();

  @Valid
  private Intensity medium = Intensity.mediumDefaults();

  @Valid
  private Intensity strong = Intensity.strongDefaults();

  public Intensity resolve(UiReadingIntensity intensity) {
    UiReadingIntensity i = intensity == null ? defaultIntensity : intensity;
    return switch (i) {
      case MEDIUM -> medium;
      case STRONG -> strong;
      case SOFT -> soft;
    };
  }

  @Getter
  @Setter
  public static class Intensity {

    @Min(50)
    @Max(120)
    private int maxWidthCh;

    @DecimalMax("3.0")
    @DecimalMin("1.0")
    private double lineHeight;

    @DecimalMax("5.0")
    @DecimalMin("0.0")
    private double paragraphGapRem;

    @Min(80)
    @Max(200)
    private int fontScalePercent;

    @DecimalMax("0.05")
    @DecimalMin("0.0")
    private double letterSpacingEm;

    public static Intensity softDefaults() {
      Intensity i = new Intensity();
      i.maxWidthCh = 74;
      i.lineHeight = 1.65;
      i.paragraphGapRem = 0.75;
      i.fontScalePercent = 100;
      i.letterSpacingEm = 0.0;
      return i;
    }

    public static Intensity mediumDefaults() {
      Intensity i = new Intensity();
      i.maxWidthCh = 70;
      i.lineHeight = 1.72;
      i.paragraphGapRem = 0.90;
      i.fontScalePercent = 105;
      i.letterSpacingEm = 0.002;
      return i;
    }

    public static Intensity strongDefaults() {
      Intensity i = new Intensity();
      i.maxWidthCh = 66;
      i.lineHeight = 1.80;
      i.paragraphGapRem = 1.05;
      i.fontScalePercent = 110;
      i.letterSpacingEm = 0.004;
      return i;
    }
  }
}
