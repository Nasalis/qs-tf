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
package org.apache.cxf.jaxrs.client.spec;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.ProxyOutputStream;

public class ClientRequestFilterInterceptor extends AbstractOutDatabindingInterceptor {

    public ClientRequestFilterInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message outMessage) throws Fault {
        ClientProviderFactory pf = ClientProviderFactory.getInstance(outMessage);
        if (pf == null) {
            return;
        }

        // create an empty proxy output stream that the filter can interact with
        // and save a reference for later
        ProxyOutputStream pos = new ProxyOutputStream();
        outMessage.setContent(OutputStream.class, pos);
        outMessage.setContent(ProxyOutputStream.class, pos);

        List<ProviderInfo<ClientRequestFilter>> filters = pf.getClientRequestFilters();
        if (!filters.isEmpty()) {

            final Exchange exchange = outMessage.getExchange();
            final ClientRequestContext context = new ClientRequestContextImpl(outMessage, false);
            for (ProviderInfo<ClientRequestFilter> filter : filters) {
                InjectionUtils.injectContexts(filter.getProvider(), filter, outMessage);
                try {
                    filter.getProvider().filter(context);

                    @SuppressWarnings("unchecked")
                    Map<String, List<Object>> headers = CastUtils.cast((Map<String, List<Object>>) 
                                                                       outMessage.get(Message.PROTOCOL_HEADERS));
                    HttpUtils.convertHeaderValuesToString(headers, false);

                    Response response = outMessage.getExchange().get(Response.class);
                    if (response != null) {
                        outMessage.getInterceptorChain().abort();

                        Message inMessage = new MessageImpl();
                        inMessage.setExchange(exchange);
                        inMessage.put(Message.RESPONSE_CODE, response.getStatus());
                        inMessage.put(Message.PROTOCOL_HEADERS, response.getMetadata());
                        exchange.setInMessage(inMessage);

                        MessageObserver observer = exchange.get(MessageObserver.class);
                        observer.onMessage(inMessage);
                        return;
                    }
                } catch (IOException ex) {
                    throw new ProcessingException(ex);
                }
            }
        }
    }

}
