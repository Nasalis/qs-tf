/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.apache.bookkeeper.bookie.stats;

import static org.apache.bookkeeper.bookie.BookKeeperServerStats.BOOKIE_SCOPE;
import static org.apache.bookkeeper.bookie.BookKeeperServerStats.CATEGORY_SERVER;
import static org.apache.bookkeeper.bookie.BookKeeperServerStats.INDEX_INMEM_ILLEGAL_STATE_DELETE;
import static org.apache.bookkeeper.bookie.BookKeeperServerStats.INDEX_INMEM_ILLEGAL_STATE_RESET;

import lombok.Getter;
import org.apache.bookkeeper.stats.Counter;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.stats.annotations.StatsDoc;

/**
 * A umbrella class for {@link org.apache.bookkeeper.bookie.IndexInMemPageMgr} stats.
 */
@StatsDoc(
    name = BOOKIE_SCOPE,
    category = CATEGORY_SERVER,
    help = "Index InMemPage Manager related stats"
)
@Getter
public class IndexInMemPageMgrStats {

    // Stats
    @StatsDoc(
        name = INDEX_INMEM_ILLEGAL_STATE_RESET,
        help = "The number of index pages detected as in illegal state when resetting"
    )
    private final Counter illegalStateResetCounter;
    @StatsDoc(
        name = INDEX_INMEM_ILLEGAL_STATE_DELETE,
        help = "The number of index pages detected as in illegal state when deleting"
    )
    private final Counter illegalStateDeleteCounter;

    public IndexInMemPageMgrStats(StatsLogger statsLogger) {
        illegalStateResetCounter = statsLogger.getCounter(INDEX_INMEM_ILLEGAL_STATE_RESET);
        illegalStateDeleteCounter = statsLogger.getCounter(INDEX_INMEM_ILLEGAL_STATE_DELETE);
    }

}
