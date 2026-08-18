package com.tcc.pjb.backend.core.security.geofence;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MaxMindGeoLite2LookupAdapter implements GeoIpLookupPort {

    private static final Logger log = LoggerFactory.getLogger(MaxMindGeoLite2LookupAdapter.class);

    private final GeofenceProperties props;
    private DatabaseReader reader;

    public MaxMindGeoLite2LookupAdapter(GeofenceProperties props) {
        this.props = Objects.requireNonNull(props);
    }

    @PostConstruct
    void carregar() {
        String databasePath = props.databasePath();
        if (databasePath == null || databasePath.isBlank()) {
            log.warn("pjb.security.geofence.database-path não configurado - geo-bloqueio de magistratura ficará indisponível");
            return;
        }
        try {
            reader = new DatabaseReader.Builder(new File(databasePath)).build();
        } catch (IOException e) {
            log.error("Falha ao carregar base GeoLite2 em {}: {}", databasePath, e.getMessage());
        }
    }

    @Override
    public GeoLookupResult lookup(String ip) {
        if (reader == null || ip == null || ip.isBlank()) {
            return GeoLookupResult.indisponivel();
        }
        try {
            CityResponse resposta = reader.city(InetAddress.getByName(ip));
            String pais = resposta.getCountry().getIsoCode();
            String subdivisao = resposta.getMostSpecificSubdivision().getIsoCode();
            List<String> asnConhecidos = props.asnVpnConhecidos();
            Long asn = resposta.getTraits().getAutonomousSystemNumber();
            boolean suspeito = asn != null && asnConhecidos.contains(String.valueOf(asn));
            return new GeoLookupResult(pais, subdivisao, suspeito, true);
        } catch (IOException | GeoIp2Exception | RuntimeException e) {
            log.warn("Falha na consulta GeoIP para {}: {}", ip, e.getMessage());
            return GeoLookupResult.indisponivel();
        }
    }
}
