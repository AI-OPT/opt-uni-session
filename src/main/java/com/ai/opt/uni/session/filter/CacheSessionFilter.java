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
 * 统一session过滤器
 * web.xml中配置示例：
 * <filter>
        <filter-name>sessionFilter</filter-name>
        <filter-class>com.ai.opt.uni.session.filter.CacheSessionFilter</filter-class>
        <init-param>
            <param-name>ignore_suffix</param-name>
            <param-value>.png,.jpg,.jpeg,.gif,.css,.js,.html,.htm</param-value>
        </init-param>
        <init-param>
            <param-name>cookie_name</param-name>
            <param-value>YC_PORTAL_JESSIONID</param-value>
        </init-param>
    </filter>
 *
 * Date: 2016年12月7日 <br>
 * Copyright (c) 2016 asiainfo.com <br>
 * @author gucl
 */
public class CacheSessionFilter implements Filter {
    /**
     * 忽略的后缀名称
     * IGNORE_SUFFIX = { ".png", ".jpg", ".jpeg",".gif", ".css", ".js", ".html", ".htm" };
     * 
     */
	public static String[] IGNORE_SUFFIX = {};
    /**
     * 统一session的cookie的名称 如：YC_PORTAL_JESSIONID
     * 
     */
    public static String COOKIE_NAME = "AIOPT_JSESSIONID";
    private SessionManager sessionManager = null;

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
            if (!StringUtils.isBlank(suffix)&&uri.endsWith(suffix.trim()))
                return false;
        }
        return true;
    }

    public void init(FilterConfig fc) throws ServletException {
        String ignore_suffix = fc.getInitParameter("ignore_suffix");
        if (StringUtils.isNotBlank(ignore_suffix)){
            IGNORE_SUFFIX = fc.getInitParameter("ignore_suffix").split(",");
        }
        
        String cookie_name = fc.getInitParameter("cookie_name");
        if (StringUtils.isNotBlank(cookie_name)){
            COOKIE_NAME = fc.getInitParameter("cookie_name").trim();
        }
        sessionManager = new SessionManager(COOKIE_NAME);
    }

}
