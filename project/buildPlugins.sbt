//import com.typesafe.sbt.GitVersioning
//import com.typesafe.sbt.SbtGit.git
import ru.simplesys.sbprocessing.sbtbuild.{CommonSettings, PluginDeps}
import sbt._

//lazy val sbtNativePackager = uri("../../sbt-plugins/sbt-native-packager")

lazy val root = Project(id = "buildPlugins", base = file(".")).dependsOn(/*RootProject(sbtNativePackager)*/)
  .enablePlugins(GitVersioning)
  .settings(inThisBuild(CommonSettings.defaultSettings ++ Seq(
        //git.baseVersion := CommonSettings.settingValues.baseVersion
  )))
  .settings(
      classpathTypes += "maven-plugin",
      PluginDeps.sbtNativePackager,
      PluginDeps.sbtRevolver,
      PluginDeps.jrebelPlugin,
      //PluginDeps.coursier
  )
