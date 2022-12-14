/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;

public class CXFNonSpringServlet extends AbstractHTTPServlet {
    public static final String TRANSPORT_ID = "transportId";

    private static final long serialVersionUID = -2437897227486327166L;
    private static final String IGNORE_SERVLET_CONTEXT_RESOLVER = "ignore.servlet.context.resolver";
    private static final String DEFAULT_TRANSPORT_ID = "http://cxf.apache.org/transports/http/configuration";

    protected Bus bus;
    private DestinationRegistry destinationRegistry;
    private boolean globalRegistry;
    private ServletController controller;
    private ClassLoader loader;
    private boolean loadBus = true;

    public CXFNonSpringServlet() {
    }

    public CXFNonSpringServlet(DestinationRegistry destinationRegistry) {
        this(destinationRegistry, true);
    }

    public CXFNonSpringServlet(DestinationRegistry destinationRegistry,
                               boolean loadBus) {
        this.destinationRegistry = destinationRegistry;
        this.globalRegistry = destinationRegistry != null;
        this.loadBus = loadBus;
    }

    public CXFNonSpringServlet(DestinationRegistry destinationRegistry, Bus bus) {
        this.destinationRegistry = destinationRegistry;
        this.globalRegistry = destinationRegistry != null;
        this.bus = bus;
        this.loadBus = false;
    }

    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        if (this.bus == null && loadBus) {
            loadBus(sc);
        }
        if (this.bus != null) {
            loader = initClassLoader();
            registerServletContextResolver(sc);
            if (destinationRegistry == null) {
                this.destinationRegistry = getDestinationRegistryFromBusOrDefault(sc.getInitParameter(TRANSPORT_ID));
            }
        }

        this.controller = createServletController(sc);
        finalizeServletInit(sc);
    }

    @Override
    protected void finalizeServletInit(ServletConfig servletConfig) throws ServletException {
        super.finalizeServletInit(servletConfig);

        if (this.destinationRegistry instanceof ServletConfigAware) {
            ((ServletConfigAware)this.destinationRegistry).onServletConfigAvailable(servletConfig);
        }
    }

    protected void registerServletContextResolver(ServletConfig sc) {
        if (Boolean.valueOf(sc.getInitParameter(IGNORE_SERVLET_CONTEXT_RESOLVER))) {
            return;
        }

        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(sc.getServletContext()));
    }

    protected ClassLoader initClassLoader() {
        return bus.getExtension(ClassLoader.class);
    }

    protected DestinationRegistry getDestinationRegistryFromBusOrDefault(final String transportId) {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            String peferredTransportId = transportId;

            // Check if the preferred transport is set on a bus level (f.e., from any
            // extension or customization).
            if (StringUtils.isEmpty(peferredTransportId) && getBus() != null) {
                peferredTransportId = (String)getBus().getProperty(AbstractTransportFactory.PREFERRED_TRANSPORT_ID);
            }

            if (StringUtils.isEmpty(peferredTransportId)) {
                final Set<String> candidates = dfm.getRegisteredDestinationFactoryNames();

                // If the default transport is present, fall back to it and don't even
                // consider other candidates
                if (!candidates.contains(DEFAULT_TRANSPORT_ID)) {
                    peferredTransportId = candidates
                        .stream()
                        .filter(name -> name.endsWith("/configuration"))
                        .findAny()
                        .orElse(DEFAULT_TRANSPORT_ID);
                }
            }

            DestinationFactory df = StringUtils.isEmpty(peferredTransportId)
                ? dfm.getDestinationFactory(DEFAULT_TRANSPORT_ID)
                    : dfm.getDestinationFactory(peferredTransportId);
            if (df instanceof HTTPTransportFactory) {
                HTTPTransportFactory transportFactory = (HTTPTransportFactory)df;
                return transportFactory.getRegistry();
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }
        return null;
    }

    protected void loadBus(ServletConfig sc) {
        this.bus = BusFactory.newInstance().createBus();
    }

    protected ServletController createServletController(ServletConfig servletConfig) {
        HttpServlet serviceListGeneratorServlet =
            new ServiceListGeneratorServlet(destinationRegistry, bus);
        return new ServletController(destinationRegistry,
                                     servletConfig,
                                     serviceListGeneratorServlet);
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            try {
                if (loader != null) {
                    origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
                if (bus != null) {
                    origBus = BusFactory.getAndSetThreadDefaultBus(bus);
                }
                HttpServletRequest httpRequest = (HttpServletRequest)request;
                if (controller.filter(new HttpServletRequestFilter(httpRequest,
                                                                   super.getServletName()),
                                      (HttpServletResponse)response)) {
                    return;
                }
            } finally {
                if (origBus != bus) {
                    BusFactory.setThreadDefaultBus(origBus);
                }
                if (origLoader != null) {
                    origLoader.reset();
                }
            }
        }
        chain.doFilter(request, response);
    }
    @Override
    protected void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            if (bus != null) {
                origBus = BusFactory.getAndSetThreadDefaultBus(bus);
            }
            controller.invoke(request, response);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    public void destroy() {
        if (!globalRegistry) {
            for (String path : destinationRegistry.getDestinationsPaths()) {
                // clean up the destination in case the destination itself can
                // no longer access the registry later
                AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(path);
                synchronized (dest) {
                    destinationRegistry.removeDestination(path);
                    dest.releaseRegistry();
                }
            }
            destinationRegistry = null;
        }
        destroyBus();
        super.destroy();
    }

    public void destroyBus() {
        if (bus != null) {
            bus.shutdown(true);
            bus = null;
        }
    }

    private static class HttpServletRequestFilter extends HttpServletRequestWrapper {
        private String filterName;
        private String servletPath;
        private String pathInfo;
        HttpServletRequestFilter(HttpServletRequest request, String filterName) {
            super(request);
            this.filterName = filterName;
        }

        @Override
        public String getServletPath() {
            if (servletPath == null) {
                try {
                    Method m = ServletContext.class.getMethod("getFilterRegistration", new Class[] {String.class});
                    Object registration = m.invoke(super.getServletContext(), new Object[]{filterName});
                    if (registration != null) {
                        m = registration.getClass().getMethod("getUrlPatternMappings", new Class[] {});
                        Collection<String> mappings =
                            CastUtils.cast((Collection<?>)m.invoke(registration, new Object[]{}));
                        if (!mappings.isEmpty()) {
                            String mapping = mappings.iterator().next();
                            if (mapping.endsWith("/*")) {
                                servletPath = mapping.substring(0, mapping.length() - 2);
                            }
                        }
                    }
                } catch (Throwable ex) {
                    // ignore
                }
                if (servletPath == null) {
                    servletPath = "";
                }
            }

            return servletPath;
        }

        @Override
        public String getPathInfo() {
            if (pathInfo == null) {
                pathInfo = super.getPathInfo();
                if (pathInfo == null) {
                    pathInfo = getRequestURI();
                }
                String prefix = super.getContextPath() + this.getServletPath();
                if (pathInfo.startsWith(prefix)) {
                    pathInfo = pathInfo.substring(prefix.length());
                }
            }
            return pathInfo;
        }
    }
}
