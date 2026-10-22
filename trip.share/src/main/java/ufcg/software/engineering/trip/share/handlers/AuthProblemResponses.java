package ufcg.software.engineering.trip.share.handlers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuthProblemResponses {
    private final JsonMapper mapper;

    public AuthProblemResponses(JsonMapper mapper) { this.mapper = mapper; }

    public static ProblemDetail problem(HttpStatus status, String errorCode, String detail, String path) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(path));
        problem.setProperty("errorCode", errorCode);
        return problem;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String errorCode, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        mapper.writeValue(response.getOutputStream(), problem(status, errorCode, detail, request.getRequestURI()));
    }
}
