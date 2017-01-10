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
    public static final String SESSION_ID_PREFIX = "R_JSID_";
    private String sessionCookieName = "AIOPT_JSESSIONID";
    private SessionClient cacheClient = new SessionClient();
    private int expirationUpdateInterval = 300;
    private int maxInactiveInterval = 7200;
    private String domain = "";
    
    public SessionManager() {
		super();
		String sessionInCookie = System.getenv("Session.In.Cookie");
        if (sessionInCookie != null && sessionInCookie.length() > 0){
        	sessionCookieName = sessionInCookie;
        }
	}
    
	public SessionManager(String sessionCookieName) {
		super();
		this.sessionCookieName = sessionCookieName;
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
        if (session != null){
            attachEvent(session, request, response, requestEventSubject);
        }
        return session;
    }

    private String getRequestedSessionId(HttpServletRequestWrapper request) {
        // String cookid=request.getRequestedSessionId();
        // System.out.println(cookid);
        Cookie[] cookies = request.getCookies();
        if ((cookies == null) || (cookies.length == 0))
            return null;
        for (Cookie cookie : cookies) {
            if (sessionCookieName.equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }

    private void saveSession(CacheHttpSession session) {
        try {
            ////if (this.log.isDebugEnabled())
                this.log.info("【统一session["+session.id+"]】 CacheHttpSession saveSession [ID=" + session.id
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
        ////if (this.log.isDebugEnabled())
            this.log.info("【统一session["+session.id+"]】 CacheHttpSession Create [ID=" + session.id + "]");
    	//GUCL：将应用程序上下文存放到session的contextPath里面
        session.setContextPath(request.getContextPath());
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
               //// if (SessionManager.this.log.isDebugEnabled()) {
                  SessionManager.this.log
                            .info("【统一session["+session.id+"]】 CacheHttpSession Request completed [ID="
                                    + session.id + ",lastAccessedTime="
                                    + session.lastAccessedTime
                                    + ",updateInterval=" + updateInterval + "]");
               //// }
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
    	log.info("====【统一session["+session.id+"]】 开始添加cookie ");
        Cookie cookie = new Cookie(sessionCookieName, null);
        if (!StringUtils.isBlank(domain))
            cookie.setDomain(domain);
        String cookiePath=session.getContextPath();
        //cookiePath为空时，必须设置为"/"，否则IE浏览器无法解析""的cookiePath，取不到session
        if(StringUtils.isBlank(cookiePath)){
        	cookiePath="/";
        }
        cookie.setPath(cookiePath);
        log.info("【统一session["+session.id+"]】 sessionCookieName="+sessionCookieName);
    	log.info("【统一session["+session.id+"]】 domain="+domain);
    	log.info("【统一session["+session.id+"]】 cookiePath="+cookiePath);
        
        if (session.expired){
        	cookie.setMaxAge(0);
        	log.info("【统一session["+session.id+"]】 cookie["+sessionCookieName+"]已过期，销毁cookie");
        }
        else if (session.isNew) {
            cookie.setValue(session.getId());
            log.info("【统一session["+session.id+"]】 cookie["+sessionCookieName+"]为新建，创建cookie");
        }
        response.addCookie(cookie);
        log.info("====【统一session["+session.id+"]】 结束添加cookie");
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
                if (sessionCookieName.equals(cookie.getName())) {
                    if (!StringUtils.isBlank(domain))
                        cookie.setDomain(domain);
                    cookie.setPath(StringUtils.isBlank(session.getContextPath()) ? "/"
                            : session.getContextPath());
                    cookie.setMaxAge(0);
                }
            }
            addCookie(session, request, response);
        }
        if (this.log.isInfoEnabled())
            this.log.info("【统一session["+session.id+"]】 CacheHttpSession saveCookie [ID=" + session.id
                    + "]");
    }

    private CacheHttpSession loadSession(String sessionId) {
        CacheHttpSession session;
        try {
            HttpSession data = this
                    .getSessionFromCache(generatorSessionKey(sessionId));
            if (data == null) {
                this.log.info("【统一session["+sessionId+"]】 Session " + sessionId + " not found in Redis");
                session = null;
            } else {
                session = (CacheHttpSession) data;
            }
            if (this.log.isInfoEnabled())
                this.log.info("【统一session["+sessionId+"]】 CacheHttpSession Load [ID=" + sessionId
                        + ",exist=" + (session != null) + "]");
            if (session != null) {
                session.isNew = false;
                session.isDirty = false;
            }
            return session;
        } catch (Exception e) {
            this.log.error("【统一session["+sessionId+"]】 exception loadSession [Id=" + sessionId + "]", e);
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
