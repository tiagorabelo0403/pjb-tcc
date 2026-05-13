package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbDatasourceTransportPostureGuardTest {

    @Test
    void datasource_profiles_keep_internal_transport_and_hardened_posture() throws Exception {
        String base = Files.readString(Path.of("src", "main", "resources", "application.yml"));
        String docker = Files.readString(Path.of("src", "main", "resources", "application-docker.yml"));
        String prod = Files.readString(Path.of("src", "main", "resources", "application-prod.yml"));

        assertTrue(base.contains("ApplicationName: ${PJB_DB_APPLICATION_NAME:pjb-backend-core}"),
                "Datasource base deve identificar a aplicação no banco para observabilidade e trilha operacional.");
        assertTrue(base.contains("sslmode: ${PJB_DB_SSL_MODE:prefer}"),
                "Datasource base deve declarar postura explícita de SSL em vez de depender de default implícito.");
        assertTrue(base.contains("targetServerType: ${PJB_DB_TARGET_SERVER_TYPE:any}"),
                "Datasource base deve declarar targetServerType para evitar failover opaco.");
        assertTrue(base.contains("ApplicationName: ${PJB_DB_READ_APPLICATION_NAME:pjb-read-replica}"),
                "Replica de leitura deve ter ApplicationName distinto para auditoria e troubleshooting.");
        assertTrue(base.contains("targetServerType: ${PJB_DB_READ_TARGET_SERVER_TYPE:preferSecondary}"),
                "Replica de leitura deve preferir secundária quando a malha estiver ativa.");
        assertTrue(base.contains("loadBalanceHosts: ${PJB_DB_READ_LOAD_BALANCE_HOSTS:true}"),
                "Replica de leitura deve permitir balanceamento explícito entre hosts internos.");

        assertTrue(docker.contains("url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/pjb}"),
                "Profile docker deve trafegar por hostname interno do compose, nunca por endpoint público implícito.");
        assertTrue(docker.contains("sslmode: ${PJB_DB_SSL_MODE:disable}"),
                "Profile docker deve declarar postura SSL explícita para laboratório local e evitar fallback silencioso.");
        assertTrue(docker.contains("ApplicationName: ${PJB_DB_APPLICATION_NAME:pjb-api-docker}"),
                "Profile docker deve identificar o pool na observabilidade do banco.");

        assertTrue(prod.contains("sslmode: ${PJB_DB_SSL_MODE:verify-full}"),
                "Profile prod deve exigir verify-full por padrão para blindar o transporte ao banco.");
        assertTrue(prod.contains("sslrootcert: ${PJB_DB_SSL_ROOT_CERT:}"),
                "Profile prod deve permitir root cert explícito para validação da cadeia TLS do banco.");
        assertTrue(prod.contains("targetServerType: ${PJB_DB_TARGET_SERVER_TYPE:primary}"),
                "Profile prod deve preferir primário na malha write para evitar escrita em target inadequado.");
        assertTrue(prod.contains("ApplicationName: ${PJB_DB_APPLICATION_NAME:pjb-api-prod}"),
                "Profile prod deve identificar a aplicação no banco para observabilidade e auditoria.");
    }
}
