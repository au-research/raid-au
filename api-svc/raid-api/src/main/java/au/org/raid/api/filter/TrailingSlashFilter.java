package au.org.raid.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class TrailingSlashFilter extends OncePerRequestFilter {

    private static final Set<String> TRAILING_SLASH_PATHS = Set.of("/raid", "/service-point");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (TRAILING_SLASH_PATHS.contains(path)) {
            String newPath = path + "/";
            String query = request.getQueryString();
            response.sendRedirect(newPath + (query != null ? "?" + query : ""));
            return;
        }

        filterChain.doFilter(request, response);
    }
}

