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
package edu.uci.ics.hyracks.dataflow.std.connectors;

import java.util.BitSet;

public class HashtableLocalityMap implements ILocalityMap {

    private static final long serialVersionUID = 1L;

    private final BitSet nodeMap;

    public HashtableLocalityMap(BitSet nodeMap) {
        this.nodeMap = nodeMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.uci.ics.hyracks.examples.text.client.aggregation.helpers.ILocalityMap
     * #getConsumers(int, int)
     */
    @Override
    public int[] getConsumers(int senderID, int nConsumerPartitions) {
        int consumersForSender = 0;
        // Get the count of consumers
        for (int i = senderID * nConsumerPartitions; i < (senderID + 1) * nConsumerPartitions; i++) {
            if (nodeMap.get(i))
                consumersForSender++;
        }
        int[] cons = new int[consumersForSender];
        int consIdx = 0;
        for (int i = senderID * nConsumerPartitions; i < (senderID + 1) * nConsumerPartitions; i++) {
            if (nodeMap.get(i)) {
                cons[consIdx] = i - senderID * nConsumerPartitions;
                consIdx++;
            }
        }
        return cons;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.uci.ics.hyracks.examples.text.client.aggregation.helpers.ILocalityMap
     * #getConsumerPartitionCount(int)
     */
    @Override
    public int getConsumerPartitionCount(int nConsumerPartitions) {
        return nConsumerPartitions;
    }

    @Override
    public boolean isConnected(int senderID, int receiverID, int nConsumerPartitions) {
        return nodeMap.get(senderID * nConsumerPartitions + receiverID);
    }

}
