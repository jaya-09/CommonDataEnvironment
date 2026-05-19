package com.cde.qlm.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component @Slf4j
public class QlmCorrelationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hReq = (HttpServletRequest) req;
        String cid = hReq.getHeader("X-Correlation-Id");
        if (cid == null || cid.isBlank()) cid = UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        ((HttpServletResponse) res).setHeader("X-Correlation-Id", cid);
        try { chain.doFilter(req, res); } finally { MDC.remove("correlationId"); }
    }
}
