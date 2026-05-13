package com.tcc.pjb.backend.service.ui.accessibility.governance;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import lombok.Getter;
import lombok.Setter;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "pjb.ui.accessibility.abac")
public class AccessibilityAbacProperties {

  @NotNull
  private boolean enabled = true;

  private String policyFile;

  @NotNull
  private String classpathResource = "classpath:ui/accessibility-abac.json";
}
