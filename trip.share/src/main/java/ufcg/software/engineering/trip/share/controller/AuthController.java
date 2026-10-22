package ufcg.software.engineering.trip.share.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ufcg.software.engineering.trip.share.dto.LoginRequest;
import ufcg.software.engineering.trip.share.dto.LoginResponse;
import ufcg.software.engineering.trip.share.security.SessionPrincipal;
import ufcg.software.engineering.trip.share.service.AuthService;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Firebase autentica a identidade; o backend mantém sessões independentes de 8 horas por padrão.")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    @Operation(summary = "Autenticar com e-mail/senha ou ID token Firebase",
            description = "Exige usuário ativo com firebase_uid previamente salvo no cadastro. "
                    + "Retorna token opaco para Authorization: Bearer. Sem renovação automática; ao expirar, executar login novamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão criada"),
            @ApiResponse(responseCode = "400", description = "Corpo inválido ou credenciais combinadas", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (inclui cadastro ausente ou suspenso)", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Provedor indisponível", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(auth.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar somente a sessão atual", security = @SecurityRequirement(name = "sessionBearer"),
            description = "Invalida imediatamente o token informado. Não revoga outras sessões nem refresh tokens Firebase.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão revogada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sessão ausente, expirada ou revogada", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SessionPrincipal principal) {
        auth.logout(principal);
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
