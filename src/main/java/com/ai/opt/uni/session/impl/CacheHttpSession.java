package com.ai.opt.uni.session.impl;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ai.opt.uni.session.SessionListener;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public class CacheHttpSession implements HttpSession, Serializable {
	private Logger log = LoggerFactory.getLogger(CacheHttpSession.class);
    private static final long serialVersionUID = 1L;
    protected long creationTime = 0L;
    protected String id;
    protected int maxInactiveInterval;
    protected long lastAccessedTime = 0L;
    protected transient boolean expired = false;
    protected transient boolean isNew = false;
    protected transient boolean isDirty = false;
    private transient SessionListener listener;
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> data = new ConcurrentHashMap();
    
    //GUCL:应用上下文
    private String contextPath=null;

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setListener(SessionListener listener) {
        this.listener = listener;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public String getId() {
        return this.id;
    }

    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getAttribute(String key) {
        return this.data.get(key);
    }

    public Object getValue(String key) {
        return this.data.get(key);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Enumeration getAttributeNames() {
        final Iterator iterator = this.data.keySet().iterator();
        return new Enumeration() {
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            public Object nextElement() {
                return iterator.next();
            }
        };
    }

    public String[] getValueNames() {
        String[] names = new String[this.data.size()];
        return (String[]) this.data.keySet().toArray(names);
    }

    public void setAttribute(String s, Object o) {
        this.data.put(s, o);
        this.isDirty = true;
    }

    public void putValue(String s, Object o) {
        this.data.put(s, o);
        this.isDirty = true;
    }

    public void removeAttribute(String s) {
        this.data.remove(s);
        this.isDirty = true;
    }

    public void removeValue(String s) {
        this.data.remove(s);
        this.isDirty = true;
    }

    public void invalidate() {
        this.expired = true;
        this.isDirty = true;
        if (this.listener != null){
        	log.info("开始  【统一session["+id+"] this.listener】 不为空，开始销毁session");
        	this.listener.onInvalidated(this);        
        	log.info("结束  【统一session["+id+"] this.listener】 不为空，结束销毁session");
        }else{
        	log.info("销毁session失败  【统一session["+id+"] this.listener】 为空，无法销毁session");
        }
    }

    public boolean isNew() {
        return this.isNew;
    }
}
