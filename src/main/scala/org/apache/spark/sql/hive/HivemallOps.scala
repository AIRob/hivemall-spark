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

package org.apache.spark.sql.hive

import hivemall.regression.LogressUDTF
import hivemall.ftvec.AddBiasUDFWrapper

import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.plans.logical.{Generate, LogicalPlan}
import org.apache.spark.sql.{Column, DataFrame}

/**
 * A wrapper of hivemall for DataFrame.
 *
 * @groupname regression
 * @groupname ftvec
 * @groupname ftvec.amplify
 * @groupname ftvec.hashing
 * @groupname ftvec.scaling
 * @groupname tools.mapred
 * @groupname dataset
 */
class HivemallOps(df: DataFrame) {

  /**
   * An implicit conversion to avoid doing annoying transformation.
   */
  @inline private implicit def toDataFrame(logicalPlan: LogicalPlan) =
    DataFrame(df.sqlContext, logicalPlan)

  /**
   * @see hivemall.regression.AdaDeltaUDTF
   * @group regression
   */
  def train_adadelta(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.regression.AdaDeltaUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.regression.AdaGradUDTF
   * @group regression
   */
  def train_adagrad(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.regression.AdaGradUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.regression.AROWRegressionUDTF
   * @group regression
   */
  def train_arow_regr(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.regression.AROWRegressionUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.regression.LogressUDTF
   * @group regression
   */
  def train_logregr(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.regression.LogressUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.ftvec.amplify.AmplifierUDTF
   * @group ftvec.amplify
   */
  def amplify(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.ftvec.amplify.AmplifierUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.ftvec.amplify.RandomAmplifierUDTF
   * @group ftvec.amplify
   */
  def rand_amplify(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.ftvec.amplify.RandomAmplifierUDTF"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }

  /**
   * @see hivemall.dataset.LogisticRegressionDataGeneratorUDTF
   * @group dataset
   */
  def lr_datagen(exprs: Column*): DataFrame = {
    Generate(new HiveGenericUdtf(
        new HiveFunctionWrapper("hivemall.dataset.LogisticRegressionDataGeneratorUDTFWrapper"),
        Nil, exprs.map(_.expr)),
      join = false, outer = false, None, df.logicalPlan)
  }
}

object HivemallOps {

  /**
   * Implicitly inject the [[HivemallOps]] into [[DataFrame]].
   */
  implicit def dataFrameToHivemallOps(df: DataFrame): HivemallOps =
    new HivemallOps(df)

  /**
   * An implicit conversion to avoid doing annoying transformation.
   */
  @inline private implicit def toColumn(expr: Expression) = Column(expr)

  /**
   * @see hivemall.ftvec.AddBiasUDF
   * @group ftvec
   */
  def add_bias(exprs: Column*): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.AddBiasUDFWrapper"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.ExtractFeatureUdf
   * @group ftvec
   */
  def extract_feature(exprs: Column*): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.ExtractFeatureUDFWrapper"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.ExtractWeightUdf
   * @group ftvec
   */
  def extract_weight(exprs: Column*): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.ExtractWeightUDFWrapper"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.AddFeatureIndexUDFWrapper
   * @group ftvec
   */
  def add_feature_index(exprs: Column*): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.AddFeatureIndexUDFWrapper"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.SortByFeatureUDF
   * @group ftvec
   */
  def sort_by_feature(exprs: Column*): Column = {
    new HiveSimpleUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.SortByFeatureUDF"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.hashing.Sha1UDF
   * @group ftvec.hashing
   */
  def sha1(exprs: Column*): Column = {
    new HiveSimpleUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.hashing.Sha1UDF"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.scaling.RescaleUDF
   * @group ftvec.scaling
   */
  def rescale(exprs: Column*): Column = {
    new HiveSimpleUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.scaling.RescaleUDF"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.scaling.ZScoreUDF
   * @group ftvec.scaling
   */
  def zscore(exprs: Column*): Column = {
    new HiveSimpleUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.scaling.ZScoreUDF"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.ftvec.scaling.L2NormalizationUDF
   * @group ftvec.scaling
   */
  def normalize(exprs: Column*): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.ftvec.scaling.L2NormalizationUDFWrapper"), exprs.map(_.expr))
  }

  /**
   * @see hivemall.tools.mapred.RowIdUDF
   * @group tools.mapred
   */
  def rowid(): Column = {
    new HiveGenericUdf(new HiveFunctionWrapper(
      "hivemall.tools.mapred.RowIdUDFWrapper"), Nil)
  }
}
