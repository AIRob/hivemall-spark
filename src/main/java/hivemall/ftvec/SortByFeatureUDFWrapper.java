/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hivemall.ftvec;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper of [[hivemall.ftvec.SortByFeatureUDF]].
 *
 * NOTE: This is needed to avoid the issue of Spark reflection.
 * That is, spark cannot handle Map<> as a return type in Hive UDF.
 * Therefore, the type must be passed via ObjectInspector.
 */
public class SortByFeatureUDFWrapper extends GenericUDF {
    private SortByFeatureUDF udf = new SortByFeatureUDF();

    private Map<IntWritable, FloatWritable> retValue = new HashMap<IntWritable, FloatWritable>();
    private MapObjectInspector argumentOI = null;

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if(arguments.length != 1) {
            throw new UDFArgumentLengthException(
                    "sort_by_feature() has an only single argument.");
        }

        switch(arguments[0].getCategory()) {
            case MAP:
                ObjectInspector keyOI = ((MapObjectInspector) arguments[0]).getMapKeyObjectInspector();
                ObjectInspector valueOI = ((MapObjectInspector) arguments[0]).getMapValueObjectInspector();
                if(keyOI.getCategory().equals(Category.PRIMITIVE)
                        && valueOI.getCategory().equals(Category.PRIMITIVE)) {
                    final PrimitiveCategory keyCategory = ((PrimitiveObjectInspector) keyOI).getPrimitiveCategory();
                    final PrimitiveCategory valueCategory = ((PrimitiveObjectInspector) valueOI).getPrimitiveCategory();
                    if (keyCategory == PrimitiveCategory.INT
                            && valueCategory == PrimitiveCategory.FLOAT) {
                        break;
                    }
                }
            default:
                throw new UDFArgumentTypeException(0,
                    "sort_by_feature() must have Map[int, float] as an argument, but "
                        + arguments[0].getTypeName() + " was found.");
        }

        argumentOI = (MapObjectInspector) arguments[0];

        return ObjectInspectorFactory.getStandardMapObjectInspector(
                argumentOI.getMapKeyObjectInspector(),
                argumentOI.getMapValueObjectInspector());
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        assert(arguments.length == 1);
        final Object arrayObject = arguments[0].get();
        final MapObjectInspector arrayOI = argumentOI;
        @SuppressWarnings("unchecked")
        final Map<IntWritable, FloatWritable> input = (Map<IntWritable, FloatWritable>) arrayOI.getMap(arrayObject);
        retValue = udf.evaluate(input);
        return retValue;
    }

    @Override
    public String getDisplayString(String[] children) {
        return "sort_by_feature(" + Arrays.toString(children) + ")";
    }
}
