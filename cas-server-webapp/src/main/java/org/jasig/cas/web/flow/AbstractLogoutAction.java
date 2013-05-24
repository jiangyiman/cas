/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.web.flow;

import javax.servlet.http.HttpServletResponse;

import org.springframework.webflow.action.AbstractAction;

/**
 * Abstract logout action, which prevents caching on logout.
 *
 * @author Jerome Leleu
 * @since 4.0.0
 */
public abstract class AbstractLogoutAction extends AbstractAction {

    /** A constant for the logout requests in web flow. */
    public static final String LOGOUT_REQUESTS = "logoutRequests";

    /** The finish event in webflow. */
    public static final String FINISH_EVENT = "finish";

    /** The front event in webflow. */
    public static final String FRONT_EVENT = "front";

    /** The redirect to app event in webflow. */
    public static final String REDIRECT_APP_EVENT = "redirectApp";

    /**
     * Prevent caching by adding the appropriate headers.
     * Copied from the <code>preventCaching</code> method in the {@link WebContentGenerator} class.
     *
     * @param response the HTTP response.
     */
    protected final void preventCaching(final HttpServletResponse response) {
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 1L);
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
    }
}
