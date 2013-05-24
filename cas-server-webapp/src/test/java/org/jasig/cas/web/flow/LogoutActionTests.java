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

import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.http.Cookie;

import org.jasig.cas.AbstractCentralAuthenticationServiceTest;
import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.logout.LogoutRequestStatus;
import org.jasig.cas.services.DefaultServicesManagerImpl;
import org.jasig.cas.services.InMemoryServiceRegistryDaoImpl;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Scott Battaglia
 * @since 3.0
 */
public class LogoutActionTests extends AbstractCentralAuthenticationServiceTest {

    private static final String COOKIE_TGC_ID = "CASTGC";

    private LogoutAction logoutAction;

    private CookieRetrievingCookieGenerator warnCookieGenerator;

    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    private InMemoryServiceRegistryDaoImpl serviceRegistryDao;

    private DefaultServicesManagerImpl serviceManager;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private RequestContext requestContext;

    @Before
    public void onSetUp() throws Exception {
        this.request = new MockHttpServletRequest();
        this.response = new MockHttpServletResponse();
        this.requestContext = mock(RequestContext.class);
        final ServletExternalContext servletExternalContext = mock(ServletExternalContext.class);
        when(this.requestContext.getExternalContext()).thenReturn(servletExternalContext);
        when(servletExternalContext.getNativeRequest()).thenReturn(request);
        when(servletExternalContext.getNativeResponse()).thenReturn(response);
        final LocalAttributeMap flowScope = new LocalAttributeMap();
        when(this.requestContext.getFlowScope()).thenReturn(flowScope);

        this.warnCookieGenerator = new CookieRetrievingCookieGenerator();
        this.serviceRegistryDao = new InMemoryServiceRegistryDaoImpl();
        this.serviceManager = new DefaultServicesManagerImpl(serviceRegistryDao);
        this.serviceManager.reload();

        this.warnCookieGenerator.setCookieName("test");

        this.ticketGrantingTicketCookieGenerator = new CookieRetrievingCookieGenerator();
        this.ticketGrantingTicketCookieGenerator.setCookieName(COOKIE_TGC_ID);

        this.logoutAction = new LogoutAction();
        this.logoutAction.setCentralAuthenticationService(getCentralAuthenticationService());
        this.logoutAction.setWarnCookieGenerator(this.warnCookieGenerator);
        this.logoutAction.setTicketGrantingTicketCookieGenerator(this.ticketGrantingTicketCookieGenerator);
        this.logoutAction.setServicesManager(this.serviceManager);
    }

    @Test
    public void testLogoutNoCookie() throws Exception {
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
    }

    @Test
    public void testLogoutForServiceWithFollowRedirectsAndMatchingService() throws Exception {
        this.request.addParameter("service", "TestService");
        final RegisteredServiceImpl impl = new RegisteredServiceImpl();
        impl.setServiceId("TestService");
        impl.setName("TestService");
        impl.setEnabled(true);
        this.serviceManager.save(impl);
        this.logoutAction.setFollowServiceRedirects(true);
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
        assertEquals("TestService", this.requestContext.getFlowScope().get("logoutRedirectUrl"));
    }

    @Test
    public void logoutForServiceWithNoFollowRedirects() throws Exception {
        this.request.addParameter("service", "TestService");
        this.logoutAction.setFollowServiceRedirects(false);
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
        assertNull(this.requestContext.getFlowScope().get("logoutRedirectUrl"));
    }

    @Test
    public void logoutForServiceWithFollowRedirectsNoAllowedService() throws Exception {
        this.request.addParameter("service", "TestService");
        final RegisteredServiceImpl impl = new RegisteredServiceImpl();
        impl.setServiceId("http://FooBar");
        impl.setName("FooBar");
        this.serviceManager.save(impl);
        this.logoutAction.setFollowServiceRedirects(true);
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
        assertNull(this.requestContext.getFlowScope().get("logoutRedirectUrl"));
    }

    @Test
    public void testLogoutCookie() throws Exception {
        Cookie cookie = new Cookie(COOKIE_TGC_ID, "test");
        this.request.setCookies(new Cookie[] {cookie});
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
    }

    @Test
    public void testLogoutRequestBack() throws Exception {
        final Cookie cookie = new Cookie(COOKIE_TGC_ID, "test");
        this.request.setCookies(new Cookie[] {cookie});
        CentralAuthenticationService centralAuthenticationService = mock(CentralAuthenticationService.class);
        LogoutRequest logoutRequest = new LogoutRequest("", null);
        logoutRequest.setStatus(LogoutRequestStatus.SUCCESS);
        when(centralAuthenticationService.destroyTicketGrantingTicket("test")).thenReturn(Arrays.asList(logoutRequest));
        this.logoutAction.setCentralAuthenticationService(centralAuthenticationService);
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FINISH_EVENT, event.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLogoutRequestFront() throws Exception {
        final Cookie cookie = new Cookie(COOKIE_TGC_ID, "test");
        this.request.setCookies(new Cookie[] {cookie});
        final CentralAuthenticationService centralAuthenticationService = mock(CentralAuthenticationService.class);
        final LogoutRequest logoutRequest = new LogoutRequest("", null);
        when(centralAuthenticationService.destroyTicketGrantingTicket("test")).thenReturn(Arrays.asList(logoutRequest));
        this.logoutAction.setCentralAuthenticationService(centralAuthenticationService);
        final Event event = this.logoutAction.doExecute(this.requestContext);
        assertEquals(LogoutAction.FRONT_EVENT, event.getId());
        Iterator<LogoutRequest> logoutRequests =
                (Iterator<LogoutRequest>) this.requestContext.getFlowScope().get(LogoutAction.LOGOUT_REQUESTS);
        assertTrue(logoutRequests.hasNext());
        assertEquals(logoutRequest, logoutRequests.next());
    }
}
