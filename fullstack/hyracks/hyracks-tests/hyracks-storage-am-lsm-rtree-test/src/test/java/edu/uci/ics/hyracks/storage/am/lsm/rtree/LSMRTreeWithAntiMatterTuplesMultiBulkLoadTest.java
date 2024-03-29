/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.hyracks.storage.am.lsm.rtree;

import java.util.Random;

import org.junit.After;
import org.junit.Before;

import edu.uci.ics.hyracks.api.dataflow.value.ISerializerDeserializer;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.api.exceptions.HyracksException;
import edu.uci.ics.hyracks.storage.am.common.api.IPrimitiveValueProviderFactory;
import edu.uci.ics.hyracks.storage.am.config.AccessMethodTestsConfig;
import edu.uci.ics.hyracks.storage.am.lsm.rtree.util.LSMRTreeTestHarness;
import edu.uci.ics.hyracks.storage.am.lsm.rtree.util.LSMRTreeWithAntiMatterTuplesTestContext;
import edu.uci.ics.hyracks.storage.am.rtree.AbstractRTreeBulkLoadTest;
import edu.uci.ics.hyracks.storage.am.rtree.AbstractRTreeTestContext;
import edu.uci.ics.hyracks.storage.am.rtree.frames.RTreePolicyType;

@SuppressWarnings("rawtypes")
public class LSMRTreeWithAntiMatterTuplesMultiBulkLoadTest extends AbstractRTreeBulkLoadTest {

    public LSMRTreeWithAntiMatterTuplesMultiBulkLoadTest() {
        super(AccessMethodTestsConfig.LSM_RTREE_BULKLOAD_ROUNDS, AccessMethodTestsConfig.LSM_RTREE_TEST_RSTAR_POLICY);
    }

    private final LSMRTreeTestHarness harness = new LSMRTreeTestHarness();

    @Before
    public void setUp() throws HyracksException {
        harness.setUp();
    }

    @After
    public void tearDown() throws HyracksDataException {
        harness.tearDown();
    }

    @Override
    protected AbstractRTreeTestContext createTestContext(ISerializerDeserializer[] fieldSerdes,
            IPrimitiveValueProviderFactory[] valueProviderFactories, int numKeys, RTreePolicyType rtreePolicyType)
            throws Exception {
        return LSMRTreeWithAntiMatterTuplesTestContext.create(harness.getMemBufferCache(),
                harness.getMemFreePageManager(), harness.getIOManager(), harness.getFileReference(),
                harness.getDiskBufferCache(), harness.getDiskFileMapProvider(), fieldSerdes, valueProviderFactories,
                numKeys, rtreePolicyType, harness.getMergePolicy(), harness.getOperationTrackerFactory(),
                harness.getIOScheduler(), harness.getIOOperationCallbackProvider());

    }

    @Override
    protected Random getRandom() {
        return harness.getRandom();
    }
}
