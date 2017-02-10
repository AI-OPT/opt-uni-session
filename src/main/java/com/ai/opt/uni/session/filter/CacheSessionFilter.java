package com.ai.opt.uni.session.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.ai.opt.uni.session.impl.RequestEventSubject;
import com.ai.opt.uni.session.impl.SessionHttpServletRequestWrapper;
import com.ai.opt.uni.session.impl.SessionManager;

import java.io.IOException;

/**
 * 缓存过滤器
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 asiainfo.com <br>
 * 
 * @author
 */
public class CacheSessionFilter implements Filter {
    // public static final String[] IGNORE_SUFFIX = { ".png", ".jpg", ".jpeg",
    // ".gif", ".css", ".js", ".html", ".htm" };
    public static String[] IGNORE_SUFFIX = {};
    private SessionManager sessionManager = new SessionManager();

    public void destroy() {

    }

    /**
     * doFilter接口
     */
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

    /**
     * 判断uri是否拦截
     * @param request
     * @return
     * @author
     */
    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        for (String suffix : IGNORE_SUFFIX) {
            if (!StringUtils.isBlank(suffix)&&uri.endsWith(suffix.trim()))
                return false;
        }
        return true;
    }

    /**
     * 初始化
     */
    public void init(FilterConfig fc) throws ServletException {
        String ignore_suffix = fc.getInitParameter("ignore_suffix");
        if (!"".equals(ignore_suffix))
            IGNORE_SUFFIX = fc.getInitParameter("ignore_suffix").split(",");
    }

}
