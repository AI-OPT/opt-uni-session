package com.ai.opt.uni.session;


import com.ai.opt.uni.session.impl.CacheHttpSession;

/**
 * session监听器
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 asiainfo.com <br>
 * 
 * @author
 */
public interface SessionListener {
	/**
	 * session修改
	 * @param paramRedisHttpSession
	 * @author
	 */
    void onAttributeChanged(CacheHttpSession paramRedisHttpSession);

    /**
     * session未验证
     * @param paramRedisHttpSession
     * @author
     */
    void onInvalidated(CacheHttpSession paramRedisHttpSession);
}
