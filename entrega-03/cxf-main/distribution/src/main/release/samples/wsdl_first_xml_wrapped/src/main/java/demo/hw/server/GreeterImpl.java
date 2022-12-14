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

package demo.hw.server;

import org.apache.hello_world_xml_http.wrapped.Greeter;
import org.apache.hello_world_xml_http.wrapped.PingMeFault;
import org.apache.hello_world_xml_http.wrapped.types.FaultDetail;

@jakarta.jws.WebService(serviceName = "XMLService",
                      portName = "XMLPort",
                      endpointInterface = "org.apache.hello_world_xml_http.wrapped.Greeter",
                      targetNamespace = "http://apache.org/hello_world_xml_http/wrapped")

@jakarta.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")

public class GreeterImpl implements Greeter {

    public String greetMe(String me) {
        System.out.println("received calling greetMe!");
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
        System.out.println("Executing operation greetMeOneWay\n");
        System.out.println("Hello there " + me);
    }

    public String sayHi() {
        System.out.println("received calling sayHi!");
        return "Bonjour";
    }

    public void pingMe() throws PingMeFault {
        System.out.println("received calling PingMeFault!");
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }
}
