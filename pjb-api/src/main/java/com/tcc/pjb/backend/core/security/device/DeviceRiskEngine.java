package com.tcc.pjb.backend.core.security.device;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;

@SuppressWarnings("ConstantValue")
@Service
public class DeviceRiskEngine {

    public RiskEvaluation evaluateFirstLink(Usuario usuario, String ip, UserSecurityProfile profile, boolean denyPrivateIps) {
        Objects.requireNonNull(usuario, "usuario");

        int risk = 0;
        String network = "UNKNOWN";
        boolean suspect = false;

        if (ip == null || ip.isBlank() || "UNKNOWN".equalsIgnoreCase(ip)) {
            risk += 35;
            suspect = true;
            network = "NO_IP";
        } else if (isPrivateOrReserved(ip)) {
            risk += 60;
            suspect = true;
            network = "PRIVATE_OR_RESERVED";
            if (denyPrivateIps) {
                return RiskEvaluation.deny(95, network, true, "IP privado/reservado não permitido.");
            }
        }

        if (profile != null && profile.getLastLoginIp() != null && profile.getLastLoginAt() != null && ip != null) {
            String prevNet = networkFingerprint(profile.getLastLoginIp());
            String nowNet = networkFingerprint(ip);
            boolean changed = prevNet != null && nowNet != null && !prevNet.equals(nowNet);
            if (changed) {
                Duration d = Duration.between(profile.getLastLoginAt(), LocalDateTime.now());
                if (!d.isNegative() && d.toMinutes() <= 120) {
                    risk += 35;
                    suspect = true;
                    network = (network.equals("UNKNOWN") ? "GEO_VELOCITY" : network + "+GEO_VELOCITY");
                } else {
                    risk += 10;
                }
            }
        }

        String tipo = usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "UNKNOWN";
        if (tipo.contains("ADVOGADO") || tipo.contains("JUIZ") || tipo.contains("MINISTRO") || tipo.contains("DESEMBARGADOR")) {
            risk += 10;
        }

        if (risk >= 90) {
            return RiskEvaluation.deny(risk, network, true, "Risco extremo no vínculo inicial do dispositivo.");
        }
        if (risk >= 70) {
            return RiskEvaluation.challenge(risk, network, suspect, "Risco elevado: exigir desafio adicional.");
        }
        return RiskEvaluation.allow(risk, network, suspect, "OK");
    }

    public boolean isPrivateOrReserved(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) return true;
            byte[] b = a.getAddress();
            if (b.length == 4) {
                int o1 = b[0] & 0xFF;
                int o2 = b[1] & 0xFF;
                if (o1 == 0) return true;
                if (o1 == 100 && (o2 >= 64 && o2 <= 127)) return true;
                if (o1 == 169 && o2 == 254) return true;
                if (o1 == 192 && o2 == 0) return true;
                if (o1 == 198 && (o2 == 18 || o2 == 19)) return true;
                if (o1 == 198 && o2 == 51) return true;
                if (o1 == 203 && o2 == 0) return true;
                if (o1 >= 224) return true;
            }
            return false;
        } catch (UnknownHostException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public static String networkFingerprint(String ip) {
        try {
            if (ip == null || ip.isBlank()) return null;
            InetAddress a = InetAddress.getByName(ip.trim());
            byte[] b = a.getAddress();
            if (b.length == 4) {
                int o1 = b[0] & 0xFF;
                int o2 = b[1] & 0xFF;
                int o3 = b[2] & 0xFF;
                return o1 + "." + o2 + "." + o3 + ".0/24";
            }
            if (b.length == 16) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8; i += 2) {
                    int hi = b[i] & 0xFF;
                    int lo = b[i + 1] & 0xFF;
                    int seg = (hi << 8) | lo;
                    sb.append(Integer.toHexString(seg));
                    if (i < 6) sb.append(":");
                }
                sb.append("::/64");
                return sb.toString().toLowerCase(Locale.ROOT);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
