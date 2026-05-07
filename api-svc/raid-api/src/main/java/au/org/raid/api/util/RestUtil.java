package au.org.raid.api.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

import static au.org.raid.api.util.ObjectUtil.infoLogExecutionTime;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class RestUtil {

    public static <T> HttpEntity<T> createEntityWithBearer(String authnToken) {
        Guard.notNull(authnToken);
        var headers = new HttpHeaders();
        headers.setBearerAuth(authnToken);
        return new HttpEntity<T>(headers);
    }

    public static <T> HttpEntity<T> createEntityWithBearer(
            String authnToken, T body
    ) {
        Guard.notNull(authnToken);
        var headers = new HttpHeaders();
        headers.setBearerAuth(authnToken);
        return new HttpEntity<T>(body, headers);
    }

    /**
     * Does log headers, but redacts the Authorization header.
     */
    public static void debugLogEntity(
            Log log,
            String description,
            HttpEntity<?> entity
    ) {
        if (!log.isDebugEnabled()) {
            return;
        }

        var logHeaders = new HttpHeaders();
        entity.getHeaders().headerSet().stream().
                filter(i -> !StringUtil.equalsIgnoreCase(i.getKey(), AUTHORIZATION)).
                forEach(i -> logHeaders.addAll(i.getKey(), i.getValue()));
        logHeaders.set(AUTHORIZATION, "redacted");

        log.with("body", entity.getBody()).
                with("headers", logHeaders).
                debug(description + " request");
    }

    /**
     * Does log headers, does NOT redact any values.
     */
    public static void debugLogResponse(
            Log log,
            String description,
            ResponseEntity<?> response
    ) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.with("status", response.getStatusCode()).
                with("headers", response.getHeaders()).
                with("body", response.getBody()).
                debug(description + " response");
    }

    /**
     * Debug log data and info log exec time.
     */
    public static <
            TResult,
            TEntity extends HttpEntity<?>,
            TResultResponse extends ResponseEntity<TResult>>
    TResult logExchange(
            Log httpLog,
            String description,
            TEntity entity,
            Function<TEntity, TResultResponse> fun
    ) {
        RestUtil.debugLogEntity(httpLog, description, entity);

        ResponseEntity<TResult> response =
                infoLogExecutionTime(httpLog, description,
                        () -> fun.apply(entity)
                );

        RestUtil.debugLogResponse(httpLog, description, response);

        return response.getBody();
    }

    public static <TRequest, TResult> TResult post(
            RestTemplate rest, String authnToken,
            String url, TRequest request, Class<TResult> resultType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authnToken);
    /* If not set, then when using openapi generated API interface,
    would get errors about "Content-Type 'application/xml;charset=UTF-8' not
    supported.  Not sure why that happend for openapi stuff. */
        headers.setContentType(APPLICATION_JSON);
        var entity = new HttpEntity<>(request, headers);

        var epResponse = rest.exchange(url, POST, entity, resultType);

        return epResponse.getBody();
    }

    public static <TResult> TResult anonGet(
            RestTemplate rest, String url, Class<TResult> resultType
    ) {
        HttpEntity<TResult> entity = new HttpEntity<>(new HttpHeaders());
        var epResponse = rest.exchange(url, GET, entity, resultType);
        return epResponse.getBody();
    }

    public static <TRequest, TResult> TResult anonPost(
            RestTemplate rest,
            String url,
            TRequest request,
            Class<TResult> resultType
    ) {
        HttpEntity<TRequest> entity = new HttpEntity<>(request, new HttpHeaders());
        var epResponse = rest.exchange(url, POST, entity, resultType);
        return epResponse.getBody();
    }


    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static String base64Decode(String encodedString) {
        return new String(Base64.getDecoder().decode(encodedString));
    }

    public static String sanitiseLocationUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }

        if (url.contains("?")) {
            url = url.substring(0, url.lastIndexOf('?'));
        }
        if (url.contains("#")) {
            url = url.substring(0, url.lastIndexOf('#'));
        }
        return url;
    }

    public static String normaliseWithoutTrailingSlash(@Nullable String url) {
        if (!StringUtil.hasValue(url)) {
            return "";
        }

        url = url.trim();
        if (url.charAt(url.length() - 1) == '/') {
            return url.substring(0, url.length() - 2);
        }
        return url;
    }

    public static String normaliseWithTrailingSlash(@Nullable String url) {
        if (!StringUtil.hasValue(url)) {
            return "/";
        }

        url = url.trim();
        if (url.charAt(url.length() - 1) != '/') {
            return url + '/';
        }
        return url;
    }


}
