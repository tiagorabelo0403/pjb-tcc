package com.tcc.pjb.backend.core.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class PjbStaticMessageCatalog {

    private static final String BUNDLE = "i18n.pjb-platform-messages";
    private static final Locale LOCALE = Locale.forLanguageTag("pt-BR");

    private PjbStaticMessageCatalog() {
    }

    public static String text(String key, Object... args) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, LOCALE);
            return MessageFormat.format(bundle.getString(key), args == null ? new Object[0] : args);
        } catch (MissingResourceException ex) {
            return key;
        }
    }
}
