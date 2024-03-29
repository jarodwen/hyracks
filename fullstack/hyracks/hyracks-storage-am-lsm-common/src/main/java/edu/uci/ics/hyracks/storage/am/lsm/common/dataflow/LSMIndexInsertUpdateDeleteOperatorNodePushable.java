/*
 * Copyright 2009-2012 by The Regents of the University of California
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
package edu.uci.ics.hyracks.storage.am.lsm.common.dataflow;

import java.nio.ByteBuffer;

import edu.uci.ics.hyracks.api.context.IHyracksTaskContext;
import edu.uci.ics.hyracks.api.dataflow.value.IRecordDescriptorProvider;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.dataflow.common.comm.io.FrameTupleAppender;
import edu.uci.ics.hyracks.dataflow.common.comm.util.FrameUtils;
import edu.uci.ics.hyracks.storage.am.common.dataflow.IIndexOperatorDescriptor;
import edu.uci.ics.hyracks.storage.am.common.dataflow.IndexInsertUpdateDeleteOperatorNodePushable;
import edu.uci.ics.hyracks.storage.am.common.ophelpers.IndexOperation;
import edu.uci.ics.hyracks.storage.am.lsm.common.api.ILSMIndexAccessor;

public class LSMIndexInsertUpdateDeleteOperatorNodePushable extends IndexInsertUpdateDeleteOperatorNodePushable {

    protected FrameTupleAppender appender;

    public LSMIndexInsertUpdateDeleteOperatorNodePushable(IIndexOperatorDescriptor opDesc, IHyracksTaskContext ctx,
            int partition, int[] fieldPermutation, IRecordDescriptorProvider recordDescProvider, IndexOperation op) {
        super(opDesc, ctx, partition, fieldPermutation, recordDescProvider, op);
    }

    @Override
    public void nextFrame(ByteBuffer buffer) throws HyracksDataException {
        accessor.reset(buffer);
        ILSMIndexAccessor lsmAccessor = (ILSMIndexAccessor) indexAccessor;
        int lastFlushedTupleIndex = 0;
        int tupleCount = accessor.getTupleCount();
        for (int i = 0; i < tupleCount; i++) {
            try {
                if (tupleFilter != null) {
                    frameTuple.reset(accessor, i);
                    if (!tupleFilter.accept(frameTuple)) {
                        lsmAccessor.noOp();
                        continue;
                    }
                }
                tuple.reset(accessor, i);

                switch (op) {
                    case INSERT: {
                        if (!lsmAccessor.tryInsert(tuple)) {
                            flushPartialFrame(lastFlushedTupleIndex, i);
                            lastFlushedTupleIndex = (i == 0) ? 0 : i - 1;
                            lsmAccessor.insert(tuple);
                        }
                        break;
                    }
                    case DELETE: {
                        if (!lsmAccessor.tryDelete(tuple)) {
                            flushPartialFrame(lastFlushedTupleIndex, i);
                            lastFlushedTupleIndex = (i == 0) ? 0 : i - 1;
                            lsmAccessor.delete(tuple);
                        }
                        break;
                    }
                    case UPSERT: {
                        if (!lsmAccessor.tryUpsert(tuple)) {
                            flushPartialFrame(lastFlushedTupleIndex, i);
                            lastFlushedTupleIndex = (i == 0) ? 0 : i - 1;
                            lsmAccessor.upsert(tuple);
                        }
                        break;
                    }
                    case UPDATE: {
                        if (!lsmAccessor.tryUpdate(tuple)) {
                            flushPartialFrame(lastFlushedTupleIndex, i);
                            lastFlushedTupleIndex = (i == 0) ? 0 : i - 1;
                            lsmAccessor.update(tuple);
                        }
                        break;
                    }
                    default: {
                        throw new HyracksDataException("Unsupported operation " + op
                                + " in tree index InsertUpdateDelete operator");
                    }
                }
            } catch (HyracksDataException e) {
                throw e;
            } catch (Exception e) {
                throw new HyracksDataException(e);
            }
        }
        if (lastFlushedTupleIndex == 0) {
            // No partial flushing was necessary. Forward entire frame.
            System.arraycopy(buffer.array(), 0, writeBuffer.array(), 0, buffer.capacity());
            FrameUtils.flushFrame(writeBuffer, writer);
        } else {
            // Flush remaining partial frame.
            flushPartialFrame(lastFlushedTupleIndex, tupleCount);
        }
    }

    private void flushPartialFrame(int startTupleIndex, int endTupleIndex) throws HyracksDataException {
        if (appender == null) {
            appender = new FrameTupleAppender(ctx.getFrameSize());
        }
        appender.reset(writeBuffer, true);
        for (int i = startTupleIndex; i < endTupleIndex; i++) {
            if (!appender.append(accessor, i)) {
                throw new IllegalStateException("Failed to append tuple into frame.");
            }
        }
        FrameUtils.flushFrame(writeBuffer, writer);
    }
}
