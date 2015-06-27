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

package org.apache.spark.ml.feature

import org.scalatest.FunSuite

class HivemallLabeledPointSuite extends FunSuite {

  test("toString") {
    val lp = HmLabeledPoint(1.0, Seq("1:0.5", "3:0.3", "8:0.1"))
    assert(lp.toString === "1.0,[1:0.5,3:0.3,8:0.1]")
  }

  test("parse") {
    val lp = HmLabeledPoint.parse("1.0,[1:0.5,3:0.3,8:0.1]")
    assert(lp.label === 1.0)
    assert(lp.features === Seq("1:0.5", "3:0.3", "8:0.1"))
  }
}
