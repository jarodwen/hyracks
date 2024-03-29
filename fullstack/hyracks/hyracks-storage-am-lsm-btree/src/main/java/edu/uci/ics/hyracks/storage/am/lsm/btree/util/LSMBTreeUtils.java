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

package edu.uci.ics.hyracks.storage.am.lsm.btree.util;

import edu.uci.ics.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import edu.uci.ics.hyracks.api.dataflow.value.ITypeTraits;
import edu.uci.ics.hyracks.api.io.FileReference;
import edu.uci.ics.hyracks.api.io.IIOManager;
import edu.uci.ics.hyracks.storage.am.bloomfilter.impls.BloomFilterFactory;
import edu.uci.ics.hyracks.storage.am.btree.frames.BTreeNSMInteriorFrameFactory;
import edu.uci.ics.hyracks.storage.am.btree.frames.BTreeNSMLeafFrameFactory;
import edu.uci.ics.hyracks.storage.am.btree.impls.BTree;
import edu.uci.ics.hyracks.storage.am.common.api.IFreePageManagerFactory;
import edu.uci.ics.hyracks.storage.am.common.api.IInMemoryFreePageManager;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexFrameFactory;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexMetaDataFrameFactory;
import edu.uci.ics.hyracks.storage.am.common.frames.LIFOMetaDataFrameFactory;
import edu.uci.ics.hyracks.storage.am.common.freepage.LinkedListFreePageManagerFactory;
import edu.uci.ics.hyracks.storage.am.lsm.btree.impls.LSMBTree;
import edu.uci.ics.hyracks.storage.am.lsm.btree.impls.LSMBTreeFileManager;
import edu.uci.ics.hyracks.storage.am.lsm.btree.tuples.LSMBTreeCopyTupleWriterFactory;
import edu.uci.ics.hyracks.storage.am.lsm.btree.tuples.LSMBTreeTupleWriterFactory;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.IInMemoryBufferCache;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMIOOperationCallbackProvider;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMIOOperationScheduler;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMIndexFileManager;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMMergePolicy;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMOperationTrackerFactory;
import edu.uci.ics.hyracks.storage.am.lsm.common.impls.BTreeFactory;
import edu.uci.ics.hyracks.storage.am.lsm.common.impls.TreeIndexFactory;
import edu.uci.ics.hyracks.storage.common.buffercache.IBufferCache;
import edu.uci.ics.hyracks.storage.common.file.IFileMapProvider;

public class LSMBTreeUtils {

    public static LSMBTree createLSMTree(IInMemoryBufferCache memBufferCache,
            IInMemoryFreePageManager memFreePageManager, IIOManager ioManager, FileReference file,
            IBufferCache diskBufferCache, IFileMapProvider diskFileMapProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] cmpFactories, int[] bloomFilterKeyFields, ILSMMergePolicy mergePolicy,
            ILSMOperationTrackerFactory opTrackerFactory, ILSMIOOperationScheduler ioScheduler,
            ILSMIOOperationCallbackProvider ioOpCallbackProvider) {
        return createLSMTree(memBufferCache, memFreePageManager, ioManager, file, diskBufferCache, diskFileMapProvider,
                typeTraits, cmpFactories, bloomFilterKeyFields, mergePolicy, opTrackerFactory, ioScheduler,
                ioOpCallbackProvider, 0);
    }

    public static LSMBTree createLSMTree(IInMemoryBufferCache memBufferCache,
            IInMemoryFreePageManager memFreePageManager, IIOManager ioManager, FileReference file,
            IBufferCache diskBufferCache, IFileMapProvider diskFileMapProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] cmpFactories, int[] bloomFilterKeyFields, ILSMMergePolicy mergePolicy,
            ILSMOperationTrackerFactory opTrackerFactory, ILSMIOOperationScheduler ioScheduler,
            ILSMIOOperationCallbackProvider ioOpCallbackProvider, int startIODeviceIndex) {
        LSMBTreeTupleWriterFactory insertTupleWriterFactory = new LSMBTreeTupleWriterFactory(typeTraits,
                cmpFactories.length, false);
        LSMBTreeTupleWriterFactory deleteTupleWriterFactory = new LSMBTreeTupleWriterFactory(typeTraits,
                cmpFactories.length, true);
        LSMBTreeCopyTupleWriterFactory copyTupleWriterFactory = new LSMBTreeCopyTupleWriterFactory(typeTraits,
                cmpFactories.length);
        ITreeIndexFrameFactory insertLeafFrameFactory = new BTreeNSMLeafFrameFactory(insertTupleWriterFactory);
        ITreeIndexFrameFactory copyTupleLeafFrameFactory = new BTreeNSMLeafFrameFactory(copyTupleWriterFactory);
        ITreeIndexFrameFactory deleteLeafFrameFactory = new BTreeNSMLeafFrameFactory(deleteTupleWriterFactory);
        ITreeIndexFrameFactory interiorFrameFactory = new BTreeNSMInteriorFrameFactory(insertTupleWriterFactory);
        ITreeIndexMetaDataFrameFactory metaFrameFactory = new LIFOMetaDataFrameFactory();
        IFreePageManagerFactory freePageManagerFactory = new LinkedListFreePageManagerFactory(diskBufferCache,
                metaFrameFactory);

        TreeIndexFactory<BTree> diskBTreeFactory = new BTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, interiorFrameFactory, copyTupleLeafFrameFactory, cmpFactories,
                typeTraits.length);
        TreeIndexFactory<BTree> bulkLoadBTreeFactory = new BTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, interiorFrameFactory, insertLeafFrameFactory, cmpFactories, typeTraits.length);

        BloomFilterFactory bloomFilterFactory = new BloomFilterFactory(diskBufferCache, diskFileMapProvider,
                bloomFilterKeyFields);

        ILSMIndexFileManager fileNameManager = new LSMBTreeFileManager(ioManager, diskFileMapProvider, file,
                diskBTreeFactory, startIODeviceIndex);

        LSMBTree lsmTree = new LSMBTree(memBufferCache, memFreePageManager, interiorFrameFactory,
                insertLeafFrameFactory, deleteLeafFrameFactory, fileNameManager, diskBTreeFactory,
                bulkLoadBTreeFactory, bloomFilterFactory, diskFileMapProvider, typeTraits.length, cmpFactories,
                mergePolicy, opTrackerFactory, ioScheduler, ioOpCallbackProvider);
        return lsmTree;
    }
}
