package com.ai.opt.uni.session.impl;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ai.opt.uni.session.RequestEventObserver;
import com.ai.opt.uni.session.exception.SessionException;

public class SessionManager {

    private Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final String SESSION_ID_PREFIX = "R_JSID_";
    private static String SESSION_ID_COOKIE = "AIOPT_JSESSIONID";
    private SessionClient cacheClient = new SessionClient();
    private int expirationUpdateInterval = 300;
    private int maxInactiveInterval = 7200;
    private String domain = "";

    static {
        String sessionInCookie = System.getenv("Session.In.Cookie");
        if (sessionInCookie != null && sessionInCookie.length() > 0)
            SESSION_ID_COOKIE = sessionInCookie;
    }

    public CacheHttpSession createSession(
            SessionHttpServletRequestWrapper request,
            HttpServletResponse response,
            RequestEventSubject requestEventSubject, boolean create) {
        String sessionId = getRequestedSessionId(request);

        CacheHttpSession session = null;
        if ((StringUtils.isEmpty(sessionId)) && (!create))
            return null;
        if (StringUtils.isNotEmpty(sessionId)) {
            session = loadSession(sessionId);
        }
        if ((session == null) && (create)) {
            session = createEmptySession(request, response);
        }
        if (session != null)
            attachEvent(session, request, response, requestEventSubject);
        return session;
    }

    private String getRequestedSessionId(HttpServletRequestWrapper request) {
        // String cookid=request.getRequestedSessionId();
        // System.out.println(cookid);
        Cookie[] cookies = request.getCookies();
        if ((cookies == null) || (cookies.length == 0))
            return null;
        for (Cookie cookie : cookies) {
            if (SESSION_ID_COOKIE.equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }

    private void saveSession(CacheHttpSession session) {
        try {
            if (this.log.isDebugEnabled())
                this.log.debug("CacheHttpSession saveSession [ID=" + session.id
                        + ",isNew=" + session.isNew + ",isDiry="
                        + session.isDirty + ",isExpired=" + session.expired
                        + "]");
            if (session.expired)
                this.removeSessionFromCache(generatorSessionKey(session.id));
            else
                // sessionService.saveSession(generatorSessionKey(session.id),
                // session,session.maxInactiveInterval +
                // this.expirationUpdateInterval);
                this.saveSessionToCache(generatorSessionKey(session.id),
                        session, session.maxInactiveInterval);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    private CacheHttpSession createEmptySession(
            SessionHttpServletRequestWrapper request,
            HttpServletResponse response) {
        CacheHttpSession session = new CacheHttpSession();
        session.id = createSessionId();
        session.creationTime = System.currentTimeMillis();
        session.maxInactiveInterval = this.maxInactiveInterval;
        session.isNew = true;
        if (this.log.isDebugEnabled())
            this.log.debug("CacheHttpSession Create [ID=" + session.id + "]");
        saveCookie(session, request, response);
        return session;
    }

    private String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private void attachEvent(final CacheHttpSession session,
                             final HttpServletRequestWrapper request,
                             final HttpServletResponse response,
                             RequestEventSubject requestEventSubject) {
        session.setListener(new SessionListenerAdaptor() {
            public void onInvalidated(CacheHttpSession session) {
                SessionManager.this.saveCookie(session, request, response);
                SessionManager.this.saveSession(session);
            }
        });
        requestEventSubject.attach(new RequestEventObserver() {
            public void completed(HttpServletRequest servletRequest,
                                  HttpServletResponse response) {
                int updateInterval = (int) ((System.currentTimeMillis() - session.lastAccessedTime) / 1000L);
                if (SessionManager.this.log.isDebugEnabled()) {
                    SessionManager.this.log
                            .debug("CacheHttpSession Request completed [ID="
                                    + session.id + ",lastAccessedTime="
                                    + session.lastAccessedTime
                                    + ",updateInterval=" + updateInterval + "]");
                }
                if ((!session.isNew)
                        && (!session.isDirty)
                        && (updateInterval < SessionManager.this.expirationUpdateInterval))
                    return;
                if ((session.isNew) && (session.expired))
                    return;
                session.lastAccessedTime = System.currentTimeMillis();
                SessionManager.this.saveSession(session);
            }
        });
    }

    private void addCookie(CacheHttpSession session,
                           HttpServletRequestWrapper request, HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_ID_COOKIE, null);
        if (!StringUtils.isBlank(domain))
            cookie.setDomain(domain);
        String cookiePath="/";
        try{
	        if(request!=null&&!StringUtils.isBlank(request.getContextPath())){
	        	cookiePath=request.getContextPath();
	        }
        }
        catch(Exception e){
        	log.error("获取cookiepath失败!采用默认的根目录/");
        }
        cookie.setPath(cookiePath);
        
        if (session.expired)
            cookie.setMaxAge(0);
        else if (session.isNew) {
            cookie.setValue(session.getId());
        }
        response.addCookie(cookie);
    }

    private void saveCookie(CacheHttpSession session,
                            HttpServletRequestWrapper request, HttpServletResponse response) {
        if ((!session.isNew) && (!session.expired))
            return;

        Cookie[] cookies = request.getCookies();
        if ((cookies == null) || (cookies.length == 0)) {
            addCookie(session, request, response);
        } else {
            for (Cookie cookie : cookies) {
                if (SESSION_ID_COOKIE.equals(cookie.getName())) {
                    if (!StringUtils.isBlank(domain))
                        cookie.setDomain(domain);
                    cookie.setPath(StringUtils.isBlank(request.getContextPath()) ? "/"
                            : request.getContextPath());
                    cookie.setMaxAge(0);
                }
            }
            addCookie(session, request, response);
        }
        if (this.log.isDebugEnabled())
            this.log.debug("CacheHttpSession saveCookie [ID=" + session.id
                    + "]");
    }

    private CacheHttpSession loadSession(String sessionId) {
        CacheHttpSession session;
        try {
            HttpSession data = this
                    .getSessionFromCache(generatorSessionKey(sessionId));
            if (data == null) {
                this.log.debug("Session " + sessionId + " not found in Redis");
                session = null;
            } else {
                session = (CacheHttpSession) data;
            }
            if (this.log.isDebugEnabled())
                this.log.debug("CacheHttpSession Load [ID=" + sessionId
                        + ",exist=" + (session != null) + "]");
            if (session != null) {
                session.isNew = false;
                session.isDirty = false;
            }
            return session;
        } catch (Exception e) {
            this.log.warn("exception loadSession [Id=" + sessionId + "]", e);
        }
        return null;
    }

    private String generatorSessionKey(String sessionId) {
        return SESSION_ID_PREFIX.concat(sessionId);
        // return "R_JSID_".concat(sessionId);
    }

    public HttpSession getSessionFromCache(String id) {
        Object obj = cacheClient.getSession(id);
        return (HttpSession) obj;

    }

    public void saveSessionToCache(String id, HttpSession session, int liveTime) {
        cacheClient.addItem(id, session, liveTime);
    }

    public void removeSessionFromCache(String id) {
        cacheClient.delItem(id);
    }

}
