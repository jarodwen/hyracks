/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hivesterix.serde.lazy.objectinspector.primitive;

import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.io.IntWritable;

import edu.uci.ics.hivesterix.serde.lazy.LazyInteger;

/**
 * A WritableIntObjectInspector inspects a IntWritable Object.
 */
public class LazyIntObjectInspector extends AbstractPrimitiveLazyObjectInspector<IntWritable> implements
        IntObjectInspector {

    LazyIntObjectInspector() {
        super(PrimitiveObjectInspectorUtils.intTypeEntry);
    }

    @Override
    public int get(Object o) {
        return getPrimitiveWritableObject(o).get();
    }

    @Override
    public Object copyObject(Object o) {
        return o == null ? null : new LazyInteger((LazyInteger) o);
    }

    @Override
    public Object getPrimitiveJavaObject(Object o) {
        return o == null ? null : Integer.valueOf(get(o));
    }
}
