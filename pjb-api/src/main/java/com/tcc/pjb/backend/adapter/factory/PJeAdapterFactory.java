package com.tcc.pjb.backend.adapter.factory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.adapter.strategies.IPJeAdapter;

@Component
public class PJeAdapterFactory {

    private final Map<String, IPJeAdapter> adapterMap;

    public PJeAdapterFactory(ApplicationContext context) {
        this.adapterMap = context.getBeansOfType(IPJeAdapter.class)
                .values()
                .stream()
                .collect(Collectors.toMap(IPJeAdapter::getAdapterKey, adapter -> adapter));
    }

    public IPJeAdapter getAdapter(String adapterKey) {
        return Optional.ofNullable(adapterMap.get(adapterKey))
                .orElseThrow(() -> new PJeAdapterNotFoundException("Adaptador não implementado: " + adapterKey));
    }
}