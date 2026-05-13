package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public record SecretariatQueueAgendaFilter(
      String tribunal,
      String foro,
      String vara,
      String orgao,
      String secretaria,
      String rito,
      String cellCode,
      String responsavel,
      String categoria
  ) {
    public static SecretariatQueueAgendaFilter empty() {
      return new SecretariatQueueAgendaFilter(null, null, null, null, null, null, null, null, null);
    }

    public SecretariatQueueAgendaFilter normalized() {
      return new SecretariatQueueAgendaFilter(
          firstNonBlank(tribunal),
          firstNonBlank(foro),
          firstNonBlank(vara),
          firstNonBlank(orgao),
          firstNonBlank(secretaria),
          firstNonBlank(rito),
          firstNonBlank(cellCode),
          firstNonBlank(responsavel),
          firstNonBlank(categoria)
      );
    }

    public Map<String, Object> toMap() {
      LinkedHashMap<String, Object> out = new LinkedHashMap<>();
      putIfPresent(out, "tribunal", tribunal);
      putIfPresent(out, "foro", foro);
      putIfPresent(out, "vara", vara);
      putIfPresent(out, "orgao", orgao);
      putIfPresent(out, "secretaria", secretaria);
      putIfPresent(out, "rito", rito);
      putIfPresent(out, "cellCode", cellCode);
      putIfPresent(out, "responsavel", responsavel);
      putIfPresent(out, "categoria", categoria);
      return Collections.unmodifiableMap(out);
    }
  

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && !key.isBlank() && value != null) {
            target.put(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
