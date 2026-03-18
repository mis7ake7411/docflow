package com.docflow.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@WebFilter("/*")
public class MDCTraceFilter implements Filter {
  private static final String TRACE_ID = "traceId";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    String traceId = UUID.randomUUID().toString();
    MDC.put(TRACE_ID, traceId);

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID); // 避免 ThreadLocal 資料外洩
    }
  }
}
