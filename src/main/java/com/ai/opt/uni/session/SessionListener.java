package com.ai.opt.uni.session;


import com.ai.opt.uni.session.impl.CacheHttpSession;

public interface SessionListener {
    void onAttributeChanged(CacheHttpSession paramRedisHttpSession);

    void onInvalidated(CacheHttpSession paramRedisHttpSession);
}
