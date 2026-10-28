package br.com.petshop.web.http;

import java.util.List;

/**
 * Exceção que carrega o status HTTP adequado para um erro de negócio.
 */
@SuppressWarnings("serial")
public final class ApiException extends RuntimeException {

    private final int status;
    private final List<String> details;
    private final String allowHeader;

    private ApiException(int status, String message, List<String> details, String allowHeader) {
        super(message);
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
        this.allowHeader = allowHeader;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message, null, null);
    }

    public static ApiException badRequest(String message, List<String> details) {
        return new ApiException(400, message, details, null);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message, null, null);
    }

    public static ApiException methodNotAllowed(String message, String allowHeader) {
        return new ApiException(405, message, null, allowHeader);
    }

    public static ApiException payloadTooLarge(String message) {
        return new ApiException(413, message, null, null);
    }

    public static ApiException unsupportedMediaType(String message) {
        return new ApiException(415, message, null, null);
    }

    public int status() {
        return status;
    }

    public List<String> details() {
        return details;
    }

    public String allowHeader() {
        return allowHeader;
    }
}
