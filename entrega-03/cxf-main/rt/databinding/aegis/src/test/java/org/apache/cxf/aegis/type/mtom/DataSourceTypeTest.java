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
package org.apache.cxf.aegis.type.mtom;

import java.io.IOException;
import java.io.InputStream;

import jakarta.activation.DataSource;

import org.easymock.MockType;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Unit test for DataSourceType class, which also tests any static helper functions invoked
 * by its implementation.
 *
 */
public class DataSourceTypeTest {

    @Test
    public void inputStreamShouldBeClosedOnHappyPath() throws Exception {
        DataSource ds = mock(MockType.STRICT, DataSource.class);
        InputStream is = mock(MockType.STRICT, InputStream.class);
        expect(ds.getInputStream()).andReturn(is);
        replay(ds);
        expect(is.available()).andReturn(1);
        expect(is.read(anyObject(byte[].class))).andReturn(-1);
        is.close();
        replay(is);

        DataSourceType dst = new DataSourceType(false, null);
        dst.getBytes(ds);

        verify(ds);
        verify(is);
    }

    @Test(expected = RuntimeException.class)
    public void inputStreamShouldBeClosedOnReadingException() throws Exception {
        DataSource ds = mock(MockType.STRICT, DataSource.class);
        InputStream is = mock(MockType.STRICT, InputStream.class);
        expect(ds.getInputStream()).andReturn(is);
        replay(ds);
        expect(is.available()).andThrow(new IOException());
        is.close();
        replay(is);

        DataSourceType dst = new DataSourceType(false, null);
        try {
            dst.getBytes(ds);
        } finally {
            verify(ds);
            verify(is);
        }

    }
}
