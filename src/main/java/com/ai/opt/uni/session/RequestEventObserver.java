package com.ai.opt.uni.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * RequestEventObserver
 * Date: 2017年2月9日 <br>
 * Copyright (c) 2017 asiainfo.com <br>
 * 
 * @author
 */
public interface RequestEventObserver {
	/**
	 * completed
	 * @param paramHttpServletRequest
	 * @param paramHttpServletResponse
	 * @author
	 */
    void completed(HttpServletRequest paramHttpServletRequest,
                   HttpServletResponse paramHttpServletResponse);
}
