package ufcg.software.engineering.trip.share.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(oneOf = {LoginRequest.PasswordLogin.class, LoginRequest.FirebaseLogin.class})
public record LoginRequest(
        @NotNull Method method,
        @Email @Size(max = 254) String email,
        @Size(max = 4096) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password,
        @Size(max = 16384) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String idToken) {

    public enum Method { PASSWORD, FIREBASE }

    public LoginRequest {
        if (email != null) email = email.trim();
    }

    @AssertTrue(message = "Informe e-mail e senha ou ID token, conforme o método, sem combinar credenciais.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCredentialsValid() {
        return method == Method.PASSWORD && hasText(email) && hasText(password) && idToken == null
                || method == Method.FIREBASE && hasText(idToken) && email == null && password == null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "LoginRequest[method=" + method + ", credentials=[REDACTED]]";
    }

    @Schema(name = "PasswordLogin", requiredProperties = {"method", "email", "password"})
    public record PasswordLogin(@Schema(allowableValues = "PASSWORD") String method,
            @Schema(format = "email") String email,
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) { }

    @Schema(name = "FirebaseLogin", requiredProperties = {"method", "idToken"})
    public record FirebaseLogin(@Schema(allowableValues = "FIREBASE") String method,
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String idToken) { }
}
