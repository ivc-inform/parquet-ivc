package ru.simplesys.sbprocessing.sbtbuild

import sbt.Keys.scalaVersion
import sbt.{addSbtPlugin, _}

object PluginDeps {
    object versions {
        val sbtNativePackagerVersion = "1.2.5-SNAPSHOT"
        val sbtResolverVersion = "0.9.1"
        val jrabelPluginVersion = "0.11.1"
    }

    val sbtNativePackager = addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % versions.sbtNativePackagerVersion)
    val sbtRevolver = addSbtPlugin("io.spray" % "sbt-revolver" % versions.sbtResolverVersion)
    val jrebelPlugin = addSbtPlugin("com.simplesys" % "jrebel-plugin" % versions.jrabelPluginVersion)
    val coursier = addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC12")
}

object CommonDeps {
    val scalaTestVersion = "3.0.4"
    val scalaTagsVersion = "0.6.7"
    val akkaVersion = "2.5.8"
    val akkaHttpVersion = "10.0.11"
    val commonTypesVersion = "1.4.5-SNAPSHOT"
    //val commonTypesVersion = "1.4.5.0"
    
    val circeVersion = "0.8.0"
    val scoptVersion = "3.7.0"
    val avro4sVersion = "1.8.2-SNAPSHOT"
    val xs4sVersion = "0.2.1-SNAPSHOT"
    val parquet17Version = "1.7.0"
    val parquetVersion = "1.9.0"
    val hadoopVersion = "3.0.0"
    val configWrapperVersion = "0.4.4"
    val loggingVersion = "3.7.2"
    val logbackVersion = "1.2.3"
    val avroVersion = "1.8.2"
    val ssysCoreVersion = "1.5-SNAPSHOT"
    val akkaHttpJsonVersion = "1.18.2-SNAPSHOT"

    val scalaReflect = Def.setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    val ssysCommon = "com.simplesys.core" %% "common" % ssysCoreVersion

    val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
    val scalaTags = "com.lihaoyi" %% "scalatags" % scalaTagsVersion
    val configWrapper = "com.github.kxbmap" %% "configs" % configWrapperVersion

    val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % akkaHttpJsonVersion
    val akkaHttpAvro = "de.heikoseeberger" %% "akka-http-avro4s" % akkaHttpJsonVersion

    val commonTypes = "ru.mfms.mfmd.integration" %% "common-types" % commonTypesVersion

    val scopt = "com.github.scopt" %% "scopt" % scoptVersion
    val xs4s = "com.scalawilliam" %% "xs4s" % xs4sVersion

    val avro = "org.apache.avro" % "avro" % avroVersion
    //val parquetAvro = "org.apache.parquet" % "parquet-avro" % parquetVersion
    val parquetColumn = "org.apache.parquet" % "parquet-column" % parquetVersion
    val parquetHadoop = "org.apache.parquet" % "parquet-hadoop" % parquetVersion
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion
    val hadoopCommon = "org.apache.hadoop" % "hadoop-common" % hadoopVersion
    val fastUtil = "it.unimi.dsi" % "fastutil" % "8.1.1"


    val logging = "com.typesafe.scala-logging" %% "scala-logging" % loggingVersion
    
    val zookeeper = "com.loopfor.zookeeper" %% "zookeeper-client" % "1.5-SNAPSHOT"

    val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

    val progressBar = "com.simplesys" %% "pb" % "0.3-SNAPSHOT"

    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

    val `color-loggers` = "com.mihnita" % "color-loggers" % "1.0.5"

}
