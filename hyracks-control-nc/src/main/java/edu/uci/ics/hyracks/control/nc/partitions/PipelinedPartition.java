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
package edu.uci.ics.hyracks.control.nc.partitions;

import java.nio.ByteBuffer;

import edu.uci.ics.hyracks.api.comm.IFrameWriter;
import edu.uci.ics.hyracks.api.dataflow.TaskAttemptId;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.api.partitions.IPartition;
import edu.uci.ics.hyracks.api.partitions.PartitionId;
import edu.uci.ics.hyracks.control.common.job.PartitionState;

public class PipelinedPartition implements IFrameWriter, IPartition {
    private final PartitionManager manager;

    private final PartitionId pid;

    private final TaskAttemptId taId;

    private IFrameWriter delegate;

    private boolean failed;

    public PipelinedPartition(PartitionManager manager, PartitionId pid, TaskAttemptId taId) {
        this.manager = manager;
        this.pid = pid;
        this.taId = taId;
    }

    @Override
    public boolean isReusable() {
        return false;
    }

    @Override
    public void deallocate() {
        // do nothing
    }

    @Override
    public synchronized void writeTo(IFrameWriter writer) {
        delegate = writer;
        notifyAll();
    }

    @Override
    public synchronized void open() throws HyracksDataException {
        manager.registerPartition(pid, taId, this, PartitionState.STARTED);
        failed = false;
        while (delegate == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new HyracksDataException(e);
            }
        }
        delegate.open();
    }

    @Override
    public void nextFrame(ByteBuffer buffer) throws HyracksDataException {
        delegate.nextFrame(buffer);
    }

    @Override
    public void fail() throws HyracksDataException {
        failed = true;
        delegate.fail();
    }

    @Override
    public void close() throws HyracksDataException {
        if (!failed) {
            manager.updatePartitionState(pid, taId, this, PartitionState.COMMITTED);
        }
        delegate.close();
    }
}