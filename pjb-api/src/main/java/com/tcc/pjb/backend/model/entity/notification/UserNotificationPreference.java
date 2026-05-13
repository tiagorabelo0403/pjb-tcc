package com.tcc.pjb.backend.model.entity.notification;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_usuario_notification_preference",
        indexes = {
                @Index(name = "idx_notif_pref_usuario", columnList = "usuario_id", unique = true),
                @Index(name = "idx_notif_pref_status", columnList = "ativo")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "allow_email", nullable = false)
    private boolean allowEmail;

    @Column(name = "allow_push", nullable = false)
    private boolean allowPush;

    @Column(name = "allow_whatsapp", nullable = false)
    private boolean allowWhatsapp;

    @Column(name = "allow_ar_digital", nullable = false)
    private boolean allowArDigital;

    @Column(name = "allow_webhook", nullable = false)
    private boolean allowWebhook;

    @Column(name = "allow_digest", nullable = false)
    private boolean allowDigest;

    @Column(name = "only_high_priority", nullable = false)
    private boolean onlyHighPriority;

    @Column(name = "anti_spam_window_minutes")
    private Integer antiSpamWindowMinutes;

    @Column(name = "push_endpoint", length = 300)
    private String pushEndpoint;

    @Column(name = "whatsapp_number", length = 40)
    private String whatsappNumber;

    @Column(name = "ar_digital_address", length = 180)
    private String arDigitalAddress;

    @Column(name = "webhook_url", length = 300)
    private String webhookUrl;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void syncDefaults() {
        if (antiSpamWindowMinutes == null || antiSpamWindowMinutes <= 0) {
            antiSpamWindowMinutes = 30;
        }
        if (!allowEmail && !allowPush && !allowWhatsapp && !allowArDigital && !allowWebhook) {
            allowEmail = true;
            allowPush = true;
        }
        ativo = true;
    }
}
