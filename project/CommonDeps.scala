package ru.simplesys.sbprocessing.sbtbuild

import sbt.Keys.scalaVersion
import sbt.{addSbtPlugin, _}

object PluginDeps {
    object versions {
        val sbtNativePackagerVersion = "1.2.5-SNAPSHOT"
        val sbtResolverVersion = "0.9.1"
    }

    val sbtNativePackager = addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % versions.sbtNativePackagerVersion)
    val sbtRevolver = addSbtPlugin("io.spray" % "sbt-revolver" % versions.sbtResolverVersion)
}

object CommonDeps {
    val scalaTestVersion = "3.0.4"

    val parquetVersion = "1.9.0"
    val hadoopVersion = "3.0.0"
    val configWrapperVersion = "0.4.4"
    val loggingVersion = "3.7.2"
    val logbackVersion = "1.2.3"
    val avroVersion = "1.8.2"
    val jacksonVersion = "1.9.13"
    //val ssysCoreVersion = "1.5-SNAPSHOT"

    val scalaReflect = Def.setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    //val ssysCommon = "com.simplesys.core" %% "common" % ssysCoreVersion

    val configWrapper = "com.github.kxbmap" %% "configs" % configWrapperVersion

    val avro = "org.apache.avro" % "avro" % avroVersion
    val parquetAvro = "org.apache.parquet" % "parquet-avro" % parquetVersion
    val parquetColumn = "org.apache.parquet" % "parquet-column" % parquetVersion
    val parquetJackson = "org.apache.parquet" % "parquet-jackson" % parquetVersion
    val jackson = "org.codehaus.jackson" % "jackson-mapper-asl" % jacksonVersion
    val jacksonCore = "org.codehaus.jackson" % "jackson-core-asl" % jacksonVersion
    val parquetFormat = "org.apache.parquet" % "parquet-format" % "2.4.0"
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion exclude("org.slf4j", "slf4j-log4j12")
    val hadoopCommon = "org.apache.hadoop" % "hadoop-common" % hadoopVersion
    val fastUtil = "it.unimi.dsi" % "fastutil" % "8.1.1"
    val `snappy-java` = "org.xerial.snappy" % "snappy-java" % "1.1.2.6" % Compile
    val `commons-pool` = "commons-pool" % "commons-pool" % "1.6"
    val `brotli-codec` = "com.github.rdblue" % "brotli-codec" % "0.1.1" % Optional
    val `org.slf4j` = "org.slf4j" % "slf4j-api" % "1.7.22"

    val logging = "com.typesafe.scala-logging" %% "scala-logging" % loggingVersion

    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

    val `color-loggers` = "com.mihnita" % "color-loggers" % "1.0.5"
}
