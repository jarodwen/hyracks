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

package edu.uci.ics.hyracks.storage.am.rtree.frames;

import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexFrameFactory;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexTupleWriterFactory;
import edu.uci.ics.hyracks.storage.am.rtree.api.IRTreeLeafFrame;

public class RTreeNSMLeafFrameFactory implements ITreeIndexFrameFactory {

    private static final long serialVersionUID = 1L;
    private final ITreeIndexTupleWriterFactory tupleWriterFactory;
    private final int keyFieldCount;

    public RTreeNSMLeafFrameFactory(ITreeIndexTupleWriterFactory tupleWriterFactory, int keyFieldCount) {
        this.tupleWriterFactory = tupleWriterFactory;
        if (keyFieldCount % 2 != 0) {
            throw new IllegalArgumentException("The key has different number of dimensions.");
        }
        this.keyFieldCount = keyFieldCount;
    }

    @Override
    public IRTreeLeafFrame createFrame() {
        return new RTreeNSMLeafFrame(tupleWriterFactory.createTupleWriter(), keyFieldCount);
    }

	@Override
	public ITreeIndexTupleWriterFactory getTupleWriterFactory() {
		return tupleWriterFactory;
	}
}