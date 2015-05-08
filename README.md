This is a simple wrapper implementation of [Hivemall](https://github.com/myui/hivemall/) for Spark.
This can make highly scalable machine learning algorithms available in DataFrame, HiveContext, and ML Pipeline.

Installation
--------------------

```
git clone https://github.com/maropu/hivemall-spark.git

cd hivemall-spark

./bin/sbt assembly

<your spark>/bin/spark-shell --jars <hivemall-spark>/target/scala-2.10/hivemall-spark-assembly-0.0.1.jar
```

To avoid this annoying option in spark-shell, you can set the hivemall jar at `spark.jars`
in <your spark>/conf/spark-default.conf.

Hivemall in DataFrame
--------------------
[DataFrame](https://spark.apache.org/docs/latest/sql-programming-guide.html#dataframes) is a distributed collection
of data with names, types, and qualifiers.
To apply Hivemall fuctions in DataFrame, you type codes below;

```
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.hive.HivemallOps._
import sqlContext.implicits._

val trainTable = sc.parallelize(
  HivemallLabeledPoint(0.0f, "1:0.8" :: "2:0.2" :: Nil) ::
  HivemallLabeledPoint(1.0f, "2:0.7" :: Nil) ::
  HivemallLabeledPoint(0.0f, "1:0.9" :: Nil) :: Nil)

sqlContext.createDataFrame(trainTable)
  .train_logregr(add_bias($"feature"), $"label")
  .groupBy("_c0") // _c0:feature _c1:weight
  .agg("_c1" -> "avg")
```

Hivemall in HiveContext
--------------------
For those try Hivemall in [HiveContext](https://spark.apache.org/docs/latest/sql-programming-guide.html#hive-tables),
run a script to register the functions of Hivemall in spark-shell and
say a SQL statements as follows;

```
:load <hivemall-spark>/scripts/define-all.sh

sqlContext.sql("
  SELECT model.feature, CAST(AVG(model.weight) AS FLOAT) AS weight
    FROM (
      SELECT train_logregr(add_bias(features), CAST(label AS FLOAT)) AS(feature, weight)
        FROM trainTable
    ) model
    GROUP BY model.feature")
```

Hivemall in Spark ML Pipeline
--------------------
[Spark ML Pipeline](https://spark.apache.org/docs/latest/ml-guide.html) is a set of APIs for machine learning algorithms
to make it easier to combine multiple algorithms into a single pipeline, or workflow.

```
import org.apache.spark.ml.MLPipeline
import org.apache.spark.ml.regression.HivemallLogress
import org.apache.spark.ml.feature.HivemallAmplifier
import org.apache.spark.ml.feature.HivemallFtVectorizer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.HivemallLabeledPoint
import org.apache.spark.sql.Row

import sqlContext.implicits._

// Training data
val trainData = sc.parallelize(
  HivemallLabeledPoint(1.f, "0:0.0" :: "1:1.1" :: "2:0.1" ::Nil) ::
  HivemallLabeledPoint(0.f, "0:2.0" :: "1:0.1" :: "2:1.0" ::Nil) ::
  HivemallLabeledPoint(1.f, "0:2.0" :: "1:1.3" :: "2:1.0" ::Nil) ::
  HivemallLabeledPoint(0.f, "0:0.0" :: "1:0.2" :: "2:0.5" ::Nil) ::
  Nil)

// Configure a ML pipeline, which consists of three stages:
// HivemallAmplifier, HivemallFtVectorizer, and HivemallLogress
val hivemallPipeline = new MLPipeline().setStages(
  Array(
    // Amplify the training data
    new HivemallAmplifier().setScaleFactor(10),
    // Transform Hivemall features into Spark-specific vectors
    new HivemallFtVectorizer().setInputCol("features").setOutputCol("ftvec"),
    // Create a HivemallLogress instance
    new HivemallLogress().setFeaturesCol("ftvec").setBiasParam(true))
  )

// Learn a Hivemall logistic regression model
val model = hivemallPipeline.fit(trainData.toDF)

// Test data
val testData = sc.parallelize(
  HivemallLabeledPoint(1.f, "0:1.9" :: "1:1.3" :: "2:0.9" ::Nil) ::
  HivemallLabeledPoint(0.f, "0:0.0" :: "1:0.3" :: "2:0.4" ::Nil) ::
  HivemallLabeledPoint(0.f, "0:0.1" :: "1:0.2" :: "2:0.5" ::Nil) ::
  HivemallLabeledPoint(1.f, "0:1.8" :: "1:1.4" :: "2:0.9" ::Nil) ::
  Nil)

// Make predictions on the test data using the learned model
model.transform(testData.toDF)
  .select("ftvec", "label", "prediction")
  .collect()
  .foreach { case Row(features: Vector, label: Double, prediction: Double) =>
    println(s"($features, $label) -> prediction=$prediction")
  }
```

Current Status
--------------------
The current implementation of Spark cannot correctly handle UDF/UDAF/UDTF in Hive.
Related issues for hivemall-spark are as follows;

* [SPARK-6747] (https://issues.apache.org/jira/browse/SPARK-6747) Support List<> as a return type in Hive UDF ([#5395](https://github.com/apache/spark/pull/5395))

* [SPARK-6921] (https://issues.apache.org/jira/browse/SPARK-6921) Support Map<K,V> as a return type in Hive UDF ([#XXXX](https://github.com/apache/spark/pull/XXXX))

* [SPARK-6734](https://issues.apache.org/jira/browse/SPARK-6734) Add UDTF.close support in Generate ([#5383](https://github.com/apache/spark/pull/5383))

* [SPARK-4233](https://issues.apache.org/jira/browse/SPARK-4233) WIP:Simplify the UDAF API (Interface) ([#3247](https://github.com/apache/spark/pull/3247))

System Requirements
--------------------

* Spark 1.4 (Expected)

* Hive 0.13

TODO
--------------------

* Support the other functions of Hivemall in DataFrame

* Support Hive 0.12

* Support Spark ML Pipeline

* Register this package as a [spark package](http://spark-packages.org/)

        For easy installations: <your spark>/bin/spark-shell --packages hivemall-spark:0.0.1

* Replace LEFT OUTER JOIN in test/prediction phases with map-side CROSS JOIN

        SELECT sigmoid(dot_product(m.weight_vector, t.features)) as prob
          FROM test_data t
          CROSS JOIN lr_model m

