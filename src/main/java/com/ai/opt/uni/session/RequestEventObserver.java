package com.ai.opt.uni.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestEventObserver {
    void completed(HttpServletRequest paramHttpServletRequest,
                   HttpServletResponse paramHttpServletResponse);
}
