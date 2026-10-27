package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.dto.ErrorResponse;

public final class SecurityErrors {
    private SecurityErrors() {
    }

    public static void write(JsonMapper mapper, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == 401) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(status, message));
    }
}
