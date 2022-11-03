/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.filefilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.apache.commons.io.file.CounterAssertions;
import org.apache.commons.io.file.Counters;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link NameFileFilter}.
 */
public class NameFileFilterTest {

    /**
     * Javadoc example.
     *
     * System.out calls are commented out here but not in the Javadoc.
     */
    @Test
    public void testJavadocExampleUsingIo() {
        final File dir = FileUtils.current();
        final String[] files = dir.list(new NameFileFilter("NOTICE.txt"));
        // End of Javadoc example
        assertEquals(1, files.length, "Testing file size");
    }

    /**
     * Javadoc example.
     *
     * System.out calls are commented out here but not in the Javadoc.
     */
    @Test
    public void testJavadocExampleUsingNio() throws IOException {
        final Path dir = Paths.get("");
        // We are interested in files older than one day
        final AccumulatorPathVisitor visitor = AccumulatorPathVisitor.withLongCounters(new NameFileFilter("NOTICE.txt"),
            TrueFileFilter.INSTANCE);
        //
        // Walk one dir
        Files.walkFileTree(dir, Collections.emptySet(), 1, visitor);
        // System.out.println(visitor.getPathCounters());
        // System.out.println(visitor.getFileList());
        // System.out.println(visitor.getDirList());
        assertEquals(1, visitor.getPathCounters().getFileCounter().get(), "Testing the total value of files");
        assertEquals(1, visitor.getPathCounters().getDirectoryCounter().get(), "Testing the total value of directories");
        assertTrue(visitor.getPathCounters().getByteCounter().get() > 0, "Verify if exists any datum");
        assertFalse(visitor.getDirList().isEmpty(), "Verify if any directory was visited");
        assertFalse(visitor.getFileList().isEmpty(), "Verify if any file was visited");
        assertEquals(1, visitor.getFileList().size(), "Amount of visited files");
        //
        visitor.getPathCounters().reset();
        //
        // Walk dir tree
        Files.walkFileTree(dir, visitor);
        // System.out.println(visitor.getPathCounters());
        // System.out.println(visitor.getDirList());
        // System.out.println(visitor.getFileList());
        //
        // End of Javadoc example
        assertTrue(visitor.getPathCounters().getFileCounter().get() > 0, "Check if any file exists");
        assertTrue(visitor.getPathCounters().getDirectoryCounter().get() > 0, "Check if any directory exists");
        assertTrue(visitor.getPathCounters().getByteCounter().get() > 0, "Check if any data exists");
        // We counted and accumulated
        assertFalse(visitor.getDirList().isEmpty(), "Check if exists any visited directory");
        assertFalse(visitor.getFileList().isEmpty(), "Check if exists any visited file");
        //
        assertNotEquals(Counters.noopPathCounters(), visitor.getPathCounters(), "Compare noop paths and visited paths");
        visitor.getPathCounters().reset();
        CounterAssertions.assertZeroCounters(visitor.getPathCounters());
    }

    @Test
    public void testNoCounting() throws IOException {
        final Path dir = Paths.get("");
        final AccumulatorPathVisitor visitor = new AccumulatorPathVisitor(Counters.noopPathCounters(),
            new NameFileFilter("NOTICE.txt"), TrueFileFilter.INSTANCE);
        Files.walkFileTree(dir, Collections.emptySet(), 1, visitor);
        //
        CounterAssertions.assertZeroCounters(visitor.getPathCounters());
        // We did not count, but we still accumulated
        assertFalse(visitor.getDirList().isEmpty(), "Check if exists any visited directory");
        assertFalse(visitor.getFileList().isEmpty(), "Check if exists any visited file");
    }
}
