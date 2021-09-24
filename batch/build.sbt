//scalaVersion := "Scala version, for example, 2.11.8"

//name := "word-count"
//organization := "dataproc.codelab"
//version := "1.0"

//libraryDependencies ++= Seq(
//  "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
//  "org.apache.spark" %% "spark-core" % "Spark version, for example, 2.3.1" % "provided"
//)



resolvers ++= Seq(
  // allows us to include spark packages
   "spark-packages" at "https://repos.spark-packages.org/",
 // "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/",
  "conjars" at "https://conjars.org/repo"
)

val localTarget: Boolean = false
// set to true when testing locally (or to build a fat jar)
// false for deployment to Cloudera with a thin jar
// reload all sbt projects to clear ivy cache

val localDeps = Seq(
  "org.apache.spark" %% "spark-core" % "3.1.2",
  "org.apache.spark" %% "spark-sql" % "3.1.2",
  "org.apache.spark" %% "spark-hive" % "3.1.2"
)

val clouderaDeps = Seq(
  "org.apache.spark" %% "spark-core" % "3.1.2" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.1.2" % "provided",
  "org.apache.spark" %% "spark-hive" % "3.1.2" % "provided",
  "commons-httpclient" % "commons-httpclient" % "3.1"
)

val otherDeps = Seq(
  "com.databricks" % "spark-csv_2.11" % "1.5.0",
  "com.typesafe" % "config" % "1.3.3",
  "org.elasticsearch" %% "elasticsearch-spark-20" % "7.12.0"  excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.rogach" %% "scallop" % "3.1.5",
  "org.scalaj" %% "scalaj-http" % "2.4.1",
  "com.crealytics" %% "spark-excel" % "0.12.0"
)

if (localTarget) libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
) ++ localDeps ++ otherDeps
else libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
) ++ clouderaDeps ++ otherDeps

dependencyOverrides += "commons-codec" % "commons-codec" % "1.11"

scalacOptions ++= List("-unchecked", "-Xlint")