package ru.simplesys.sbprocessing.sbtbuild

object CommonSettings {
    object settingValues {
        val name = "template-storage-service"
        val scalaVersion = "2.12.4"
        val organization = "ru.mfms.template-storage"
        val baseVersion = "0.12.4"

        val scalacOptions = Seq(
            "-feature",
            "-language:higherKinds",
            "-language:implicitConversions",
            "-language:existentials",
            "-language:postfixOps",
            "-deprecation",
            "-unchecked")
    }

    val defaultSettings = {
        import sbt.Keys._
        Seq(
            scalacOptions := settingValues.scalacOptions,
            organization := settingValues.organization
        )
    }

    val dockerGroupName = "mfmd"
}
