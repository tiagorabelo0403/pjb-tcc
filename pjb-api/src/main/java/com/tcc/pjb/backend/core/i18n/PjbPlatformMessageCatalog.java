package com.tcc.pjb.backend.core.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.springframework.stereotype.Component;

@Component
public class PjbPlatformMessageCatalog {

    private static final String BUNDLE = "i18n.pjb-platform-messages";

    public String text(String key, Object... args) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag("pt-BR"));
            return MessageFormat.format(bundle.getString(key), args == null ? new Object[0] : args);
        } catch (MissingResourceException ex) {
            return key;
        }
    }
}
