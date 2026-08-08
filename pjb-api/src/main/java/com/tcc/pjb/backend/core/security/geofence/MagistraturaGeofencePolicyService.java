package com.tcc.pjb.backend.core.security.geofence;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaGeofencePolicyService {

    public enum Decisao { PERMITIDO, BLOQUEADO_PAIS, BLOQUEADO_UF, BLOQUEADO_VPN, INDISPONIVEL }

    public record Avaliacao(Decisao decisao, String motivo) {}

    private final GeoIpLookupPort geoIpLookupPort;
    private final JudgeTravelExceptionRepository travelExceptionRepository;
    private final GeofenceProperties props;
    private final Environment environment;

    public MagistraturaGeofencePolicyService(GeoIpLookupPort geoIpLookupPort,
                                              JudgeTravelExceptionRepository travelExceptionRepository,
                                              GeofenceProperties props,
                                              Environment environment) {
        this.geoIpLookupPort = Objects.requireNonNull(geoIpLookupPort);
        this.travelExceptionRepository = Objects.requireNonNull(travelExceptionRepository);
        this.props = Objects.requireNonNull(props);
        this.environment = Objects.requireNonNull(environment);
    }

    public Avaliacao avaliar(Usuario usuario, String ip) {
        GeoLookupResult resultado = geoIpLookupPort.lookup(ip);
        if (!resultado.disponivel()) {
            if (indisponibilidadeBloqueiaEmProd()) {
                return new Avaliacao(Decisao.INDISPONIVEL, "Consulta de geolocalização indisponível");
            }
            return new Avaliacao(Decisao.PERMITIDO, "Geo-bloqueio indisponível - liberado (fora de prod ou enforcement desativado)");
        }
        if (resultado.anonymousProxyOuHosting()) {
            return new Avaliacao(Decisao.BLOQUEADO_VPN, "Acesso via VPN/datacenter detectado - desative a VPN para continuar");
        }
        if (temExcecaoAtiva(usuario.getId(), resultado)) {
            return new Avaliacao(Decisao.PERMITIDO, "Exceção de viagem ativa");
        }
        if (!"BR".equalsIgnoreCase(resultado.countryIso())) {
            return new Avaliacao(Decisao.BLOQUEADO_PAIS, "Acesso fora do Brasil não autorizado");
        }
        if (usuario.getUf() != null && !usuario.getUf().equalsIgnoreCase(resultado.subdivisionIso())) {
            return new Avaliacao(Decisao.BLOQUEADO_UF, "Acesso fora da UF de lotação (" + usuario.getUf() + ")");
        }
        return new Avaliacao(Decisao.PERMITIDO, null);
    }

    private boolean indisponibilidadeBloqueiaEmProd() {
        return props.enforceRealEmProd() && environment.acceptsProfiles(Profiles.of("prod"));
    }

    private boolean temExcecaoAtiva(Long usuarioId, GeoLookupResult resultado) {
        return travelExceptionRepository.existeExcecaoAtivaParaDestino(
                usuarioId, resultado.countryIso(), resultado.subdivisionIso(), LocalDate.now());
    }
}
