package com.ai.opt.uni.session.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ai.opt.uni.session.impl.RequestEventSubject;
import com.ai.opt.uni.session.impl.SessionHttpServletRequestWrapper;
import com.ai.opt.uni.session.impl.SessionManager;

import java.io.IOException;

public class CacheSessionFilter implements Filter {
    // public static final String[] IGNORE_SUFFIX = { ".png", ".jpg", ".jpeg",
    // ".gif", ".css", ".js", ".html", ".htm" };
    public static String[] IGNORE_SUFFIX = {};
    private SessionManager sessionManager = new SessionManager();

    public void destroy() {

    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (!shouldFilter(request)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        RequestEventSubject eventSubject = new RequestEventSubject();
        SessionHttpServletRequestWrapper requestWrapper = new SessionHttpServletRequestWrapper(
                request, response, this.sessionManager, eventSubject);
        try {
            filterChain.doFilter(requestWrapper, servletResponse);
        } finally {
            eventSubject.completed(request, response);
        }
    }

    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        for (String suffix : IGNORE_SUFFIX) {
            if (uri.endsWith(suffix))
                return false;
        }
        return true;
    }

    public void init(FilterConfig fc) throws ServletException {
        String ignore_suffix = fc.getInitParameter("ignore_suffix");
        if (!"".equals(ignore_suffix))
            IGNORE_SUFFIX = fc.getInitParameter("ignore_suffix").split(",");
    }

}
