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
package edu.uci.ics.hivesterix.serde.lazy;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hive.serde2.ByteStream.Output;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.typeinfo.ListTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.MapTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.WritableUtils;

import edu.uci.ics.hivesterix.serde.lazy.objectinspector.LazyObjectInspectorFactory;
import edu.uci.ics.hivesterix.serde.lazy.objectinspector.primitive.PrimitiveObjectInspectorFactory;

/**
 * LazyUtils.
 */
public final class LazyUtils {

    /**
     * Convert the byte array to an int starting from the given offset. Refer to
     * code by aeden on DZone Snippets:
     * 
     * @param b
     *            the byte array
     * @param offset
     *            the array offset
     * @return the integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     * Convert the byte array to a long starting from the given offset.
     * 
     * @param b
     *            the byte array
     * @param offset
     *            the array offset
     * @return the long
     */
    public static long byteArrayToLong(byte[] b, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            int shift = (8 - 1 - i) * 8;
            value += ((long) (b[i + offset] & 0x00000000000000FF)) << shift;
        }
        return value;
    }

    /**
     * Convert the byte array to a short starting from the given offset.
     * 
     * @param b
     *            the byte array
     * @param offset
     *            the array offset
     * @return the short
     */
    public static short byteArrayToShort(byte[] b, int offset) {
        short value = 0;
        value += (b[offset] & 0x000000FF) << 8;
        value += (b[offset + 1] & 0x000000FF);
        return value;
    }

    /**
     * Record is the unit that data is serialized in. A record includes two
     * parts. The first part stores the size of the element and the second part
     * stores the real element. size element record ->
     * |----|-------------------------|
     * A RecordInfo stores two information of a record, the size of the "size"
     * part which is the element offset and the size of the element part which
     * is element size.
     */
    public static class RecordInfo {
        public RecordInfo() {
            elementOffset = 0;
            elementSize = 0;
        }

        public byte elementOffset;
        public int elementSize;

        @Override
        public String toString() {
            return "(" + elementOffset + ", " + elementSize + ")";
        }
    }

    static VInt vInt = new LazyUtils.VInt();

    /**
     * Check a particular field and set its size and offset in bytes based on
     * the field type and the bytes arrays.
     * For void, boolean, byte, short, int, long, float and double, there is no
     * offset and the size is fixed. For string, map, list, struct, the first
     * four bytes are used to store the size. So the offset is 4 and the size is
     * computed by concating the first four bytes together. The first four bytes
     * are defined with respect to the offset in the bytes arrays.
     * 
     * @param objectInspector
     *            object inspector of the field
     * @param bytes
     *            bytes arrays store the table row
     * @param offset
     *            offset of this field
     * @param recordInfo
     *            modify this byteinfo object and return it
     */
    public static void checkObjectByteInfo(ObjectInspector objectInspector, byte[] bytes, int offset,
            RecordInfo recordInfo) {
        Category category = objectInspector.getCategory();
        switch (category) {
            case PRIMITIVE:
                PrimitiveCategory primitiveCategory = ((PrimitiveObjectInspector) objectInspector)
                        .getPrimitiveCategory();
                switch (primitiveCategory) {
                    case VOID:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = 0;
                        break;
                    case BOOLEAN:
                    case BYTE:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = 1;
                        break;
                    case SHORT:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = 2;
                        break;
                    case FLOAT:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = 4;
                        break;
                    case DOUBLE:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = 8;
                        break;
                    case INT:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = WritableUtils.decodeVIntSize(bytes[offset]);
                        break;
                    case LONG:
                        recordInfo.elementOffset = 0;
                        recordInfo.elementSize = WritableUtils.decodeVIntSize(bytes[offset]);
                        break;
                    case STRING:
                        // using vint instead of 4 bytes
                        LazyUtils.readVInt(bytes, offset, vInt);
                        recordInfo.elementOffset = vInt.length;
                        recordInfo.elementSize = vInt.value;
                        break;
                    default: {
                        throw new RuntimeException("Unrecognized primitive type: " + primitiveCategory);
                    }
                }
                break;
            case LIST:
            case MAP:
            case STRUCT:
                recordInfo.elementOffset = 4;
                recordInfo.elementSize = LazyUtils.byteArrayToInt(bytes, offset);
                break;
            default: {
                throw new RuntimeException("Unrecognized non-primitive type: " + category);
            }
        }
    }

    /**
     * A zero-compressed encoded long.
     */
    public static class VLong {
        public VLong() {
            value = 0;
            length = 0;
        }

        public long value;
        public byte length;
    };

    /**
     * Reads a zero-compressed encoded long from a byte array and returns it.
     * 
     * @param bytes
     *            the byte array
     * @param offset
     *            offset of the array to read from
     * @param vlong
     *            storing the deserialized long and its size in byte
     */
    public static void readVLong(byte[] bytes, int offset, VLong vlong) {
        byte firstByte = bytes[offset];
        vlong.length = (byte) WritableUtils.decodeVIntSize(firstByte);
        if (vlong.length == 1) {
            vlong.value = firstByte;
            return;
        }
        long i = 0;
        for (int idx = 0; idx < vlong.length - 1; idx++) {
            byte b = bytes[offset + 1 + idx];
            i = i << 8;
            i = i | (b & 0xFF);
        }
        vlong.value = (WritableUtils.isNegativeVInt(firstByte) ? (i ^ -1L) : i);
    }

    /**
     * A zero-compressed encoded integer.
     */
    public static class VInt implements Serializable {
        private static final long serialVersionUID = 1L;

        public VInt() {
            value = 0;
            length = 0;
        }

        public int value;
        public byte length;
    };

    /**
     * Reads a zero-compressed encoded int from a byte array and returns it.
     * 
     * @param bytes
     *            the byte array
     * @param offset
     *            offset of the array to read from
     * @param vInt
     *            storing the deserialized int and its size in byte
     */
    public static void readVInt(byte[] bytes, int offset, VInt vInt) {
        byte firstByte = bytes[offset];
        vInt.length = (byte) WritableUtils.decodeVIntSize(firstByte);
        if (vInt.length == 1) {
            vInt.value = firstByte;
            return;
        }
        int i = 0;
        for (int idx = 0; idx < vInt.length - 1; idx++) {
            byte b = bytes[offset + 1 + idx];
            i = i << 8;
            i = i | (b & 0xFF);
        }
        vInt.value = (WritableUtils.isNegativeVInt(firstByte) ? (i ^ -1) : i);
    }

    /**
     * Writes a zero-compressed encoded int to a byte array.
     * 
     * @param byteStream
     *            the byte array/stream
     * @param i
     *            the int
     */
    public static void writeVInt(Output byteStream, int i) {
        writeVLong(byteStream, i);
    }

    /**
     * Write a zero-compressed encoded long to a byte array.
     * 
     * @param byteStream
     *            the byte array/stream
     * @param l
     *            the long
     */
    public static void writeVLong(Output byteStream, long l) {
        if (l >= -112 && l <= 127) {
            byteStream.write((byte) l);
            return;
        }

        int len = -112;
        if (l < 0) {
            l ^= -1L; // take one's complement'
            len = -120;
        }

        long tmp = l;
        while (tmp != 0) {
            tmp = tmp >> 8;
            len--;
        }

        byteStream.write((byte) len);

        len = (len < -120) ? -(len + 120) : -(len + 112);

        for (int idx = len; idx != 0; idx--) {
            int shiftbits = (idx - 1) * 8;
            long mask = 0xFFL << shiftbits;
            byteStream.write((byte) ((l & mask) >> shiftbits));
        }
    }

    static Map<TypeInfo, ObjectInspector> cachedLazyObjectInspector = new ConcurrentHashMap<TypeInfo, ObjectInspector>();

    /**
     * Returns the lazy binary object inspector that can be used to inspect an
     * lazy binary object of that typeInfo
     * For primitive types, we use the standard writable object inspector.
     */
    public static ObjectInspector getLazyObjectInspectorFromTypeInfo(TypeInfo typeInfo, boolean topLevel) {
        if (typeInfo == null)
            throw new IllegalStateException("illegal type null ");
        ObjectInspector result = cachedLazyObjectInspector.get(typeInfo);
        if (result == null) {
            switch (typeInfo.getCategory()) {
                case PRIMITIVE: {
                    result = PrimitiveObjectInspectorFactory
                            .getPrimitiveLazyObjectInspector(((PrimitiveTypeInfo) typeInfo).getPrimitiveCategory());
                    break;
                }
                case LIST: {
                    ObjectInspector elementObjectInspector = getLazyObjectInspectorFromTypeInfo(
                            ((ListTypeInfo) typeInfo).getListElementTypeInfo(), false);
                    result = LazyObjectInspectorFactory.getLazyListObjectInspector(elementObjectInspector);
                    break;
                }
                case MAP: {
                    MapTypeInfo mapTypeInfo = (MapTypeInfo) typeInfo;
                    ObjectInspector keyObjectInspector = getLazyObjectInspectorFromTypeInfo(
                            mapTypeInfo.getMapKeyTypeInfo(), false);
                    ObjectInspector valueObjectInspector = getLazyObjectInspectorFromTypeInfo(
                            mapTypeInfo.getMapValueTypeInfo(), false);
                    result = LazyObjectInspectorFactory.getLazyMapObjectInspector(keyObjectInspector,
                            valueObjectInspector);
                    break;
                }
                case STRUCT: {
                    StructTypeInfo structTypeInfo = (StructTypeInfo) typeInfo;
                    List<String> fieldNames = structTypeInfo.getAllStructFieldNames();
                    List<TypeInfo> fieldTypeInfos = structTypeInfo.getAllStructFieldTypeInfos();
                    List<ObjectInspector> fieldObjectInspectors = new ArrayList<ObjectInspector>(fieldTypeInfos.size());

                    for (int i = 0; i < fieldTypeInfos.size(); i++) {
                        fieldObjectInspectors.add(getLazyObjectInspectorFromTypeInfo(fieldTypeInfos.get(i), false));
                    }

                    // if it is top level then create columnar
                    if (topLevel)
                        result = LazyObjectInspectorFactory.getLazyColumnarObjectInspector(fieldNames,
                                fieldObjectInspectors);
                    // if it is not top level then create struct
                    else
                        result = LazyObjectInspectorFactory.getLazyStructObjectInspector(fieldNames,
                                fieldObjectInspectors);

                    break;
                }
                default: {
                    result = null;
                }
            }
            cachedLazyObjectInspector.put(typeInfo, result);
        }
        return result;
    }

    /**
     * get top-level lazy object inspector
     * 
     * @param fieldNames
     * @param fieldTypeInfos
     * @return
     */
    public static ObjectInspector getLazyObjectInspector(List<String> fieldNames, List<TypeInfo> fieldTypeInfos) {
        List<ObjectInspector> fieldObjectInspectors = new ArrayList<ObjectInspector>(fieldTypeInfos.size());
        for (int i = 0; i < fieldTypeInfos.size(); i++) {
            fieldObjectInspectors.add(getLazyObjectInspectorFromTypeInfo(fieldTypeInfos.get(i), false));
        }

        return LazyObjectInspectorFactory.getLazyColumnarObjectInspector(fieldNames, fieldObjectInspectors);
    }

    private LazyUtils() {
        // prevent instantiation
    }

    /**
     * Returns -1 if the first byte sequence is lexicographically less than the
     * second; returns +1 if the second byte sequence is lexicographically less
     * than the first; otherwise return 0.
     */
    public static int compare(byte[] b1, int start1, int length1, byte[] b2, int start2, int length2) {

        int min = Math.min(length1, length2);

        for (int i = 0; i < min; i++) {
            if (b1[start1 + i] == b2[start2 + i]) {
                continue;
            }
            if (b1[start1 + i] < b2[start2 + i]) {
                return -1;
            } else {
                return 1;
            }
        }

        if (length1 < length2) {
            return -1;
        }
        if (length1 > length2) {
            return 1;
        }
        return 0;
    }

    public static int hashBytes(byte[] data, int start, int len) {
        int hash = 1;
        for (int i = start; i < len; i++) {
            hash = (31 * hash) + data[i];
        }
        return hash;
    }

    /**
     * Writes a zero-compressed encoded int to a byte array.
     * 
     * @param byteStream
     *            the byte array/stream
     * @param i
     *            the int
     */
    public static void writeVInt(DataOutput byteStream, int i) throws IOException {
        writeVLong(byteStream, i);
    }

    /**
     * Write a zero-compressed encoded long to a byte array.
     * 
     * @param byteStream
     *            the byte array/stream
     * @param l
     *            the long
     */
    public static void writeVLong(DataOutput byteStream, long l) throws IOException {
        if (l >= -112 && l <= 127) {
            byteStream.write((byte) l);
            return;
        }

        int len = -112;
        if (l < 0) {
            l ^= -1L; // take one's complement'
            len = -120;
        }

        long tmp = l;
        while (tmp != 0) {
            tmp = tmp >> 8;
            len--;
        }

        byteStream.write((byte) len);

        len = (len < -120) ? -(len + 120) : -(len + 112);

        for (int idx = len; idx != 0; idx--) {
            int shiftbits = (idx - 1) * 8;
            long mask = 0xFFL << shiftbits;
            byteStream.write((byte) ((l & mask) >> shiftbits));
        }
    }
}
