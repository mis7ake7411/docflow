package com.docflow.infra.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 控制靜態資源快取，避免舊版 JS 被瀏覽器長期快取造成空白畫面。
 */
@Component
public class StaticCacheControlFilter extends OncePerRequestFilter {

    private static final String ASSET_PREFIX = "/assets/";
    private static final String CACHE_IMMUTABLE = "public, max-age=31536000, immutable";
    private static final String CACHE_NO = "no-cache, no-store, must-revalidate";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isAsset = path.startsWith(ASSET_PREFIX);
        boolean isIndex = "/".equals(path) || "/index.html".equals(path);

        filterChain.doFilter(request, response);

        if (response.isCommitted()) {
            return;
        }

        if (isAsset) {
            response.setHeader("Cache-Control", CACHE_IMMUTABLE);
        } else if (isIndex) {
            response.setHeader("Cache-Control", CACHE_NO);
        }
    }
}
