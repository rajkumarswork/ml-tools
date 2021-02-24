name := "mltools"
organization := "ml.tools"
version := "0.0.1-SNAPSHOT"

val mlToolsVersion = "0.0.1"

scalaVersion := "2.12.12"

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:higherKinds",
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-Xcheckinit",
  "-Xlint:adapted-args",
  "-Xlint:delayedinit-select",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:missing-interpolator",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
  "-Xfuture", "-Yno-adapted-args"
)
scalacOptions ++= commonScalacOptions

// - Dependencies ---
val featranVersion = "0.8.0-RC1"
val xgBoostVersion = "1.3.1"
val gcStorageVersion = "1.113.9"
val gcBQVersion = "1.126.6"
val tfProtoVersion = "1.15.0"
val scallopVersion = "4.0.2"
val circeVersion = "0.12.3"
val scalatestVersion = "3.2.2"
val scalamockVersion = "5.1.0"

libraryDependencies ++= Seq(
  "com.spotify" %% "featran-core" % featranVersion,
  "ml.dmlc" %% "xgboost4j" % xgBoostVersion,
  "com.google.cloud" % "google-cloud-storage" % gcStorageVersion,
  "com.google.cloud" % "google-cloud-bigquery" % gcBQVersion,
  "org.tensorflow" % "proto" % tfProtoVersion,
  "org.rogach" %% "scallop" % scallopVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "org.scalamock" %% "scalamock" % scalamockVersion % Test
)

// - Assembly plugin ---
assemblyJarName in assembly := "ml-tools.jar"
mainClass in assembly := Some("ml.tools.cli")
assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
