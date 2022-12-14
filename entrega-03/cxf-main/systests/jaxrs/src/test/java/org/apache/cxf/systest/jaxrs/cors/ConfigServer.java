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

package org.apache.cxf.systest.jaxrs.cors;

import java.util.Arrays;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter;

/**
 *
 */
public class ConfigServer {
    private CrossOriginResourceSharingFilter inputFilter;

    @POST
    @Consumes("application/json")
    @Path("/setOriginList")
    @Produces("text/plain")
    public String setOriginList(String[] origins) {
        if (origins != null) {
            inputFilter.setAllowOrigins(Arrays.asList(origins));
        }
        return "ok";
    }

    @POST
    @Path("/setAllowCredentials/{yn}")
    @Produces("text/plain")
    public String setAllowCredentials(@PathParam("yn") boolean yn) {
        inputFilter.setAllowCredentials(yn);
        return "ok";
    }


    public CrossOriginResourceSharingFilter getInputFilter() {
        return inputFilter;
    }

    public void setInputFilter(CrossOriginResourceSharingFilter inputFilter) {
        this.inputFilter = inputFilter;
    }

}
