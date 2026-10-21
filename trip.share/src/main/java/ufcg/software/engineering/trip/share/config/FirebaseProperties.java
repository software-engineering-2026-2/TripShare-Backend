package ufcg.software.engineering.trip.share.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Caminho do arquivo de credencial do Firebase, vindo de FIREBASE_KEY_PATH.
 * Sem valor padrão: a credencial deve ser injetada externamente e o startup falha com erro claro sem ela.
 */
@ConfigurationProperties(prefix = "app.firebase")
@Validated
public record FirebaseProperties(@NotBlank String keyPath) {
}
