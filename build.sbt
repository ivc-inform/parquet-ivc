import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.packager.docker.Cmd
//import com.typesafe.sbt.packager.docker.Cmd
import ru.simplesys.sbprocessing.sbtbuild.{CommonDeps, CommonSettings}

lazy val templateSorage = (project in file("."))
  .enablePlugins(GitVersioning)
  .aggregate(
      common,
      zookeeper,
      hadoop,
      parquetAvro,
      convertor,
      webServer,
      test
  )
  .settings(
      inThisBuild(Seq(
          git.baseVersion := CommonSettings.settingValues.baseVersion,
          scalaVersion := CommonSettings.settingValues.scalaVersion,
          name := CommonSettings.settingValues.name,
          publishTo := {
              val corporateRepo = "http://toucan.simplesys.lan/"
              if (isSnapshot.value)
                  Some("snapshots" at corporateRepo + "artifactory/libs-snapshot-local")
              else
                  Some("releases" at corporateRepo + "artifactory/libs-release-local")
          },
          credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

      ) ++ CommonSettings.defaultSettings),
      publishArtifact := false,
  )

lazy val common = Project(id = "common", base = file("common"))
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.`color-loggers`,
          CommonDeps.logging,
          CommonDeps.ssysCommon,
          CommonDeps.configWrapper,
          CommonDeps.scalaTest
      )
  )

lazy val zookeeper = Project(id = "zookeeper", base = file("zookeeper"))
  .dependsOn(common)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.zookeeper,
          CommonDeps.logging,
          CommonDeps.scalaTest
      )
  )

lazy val hadoop = Project(id = "hadoop", base = file("hadoop"))
  .dependsOn(parquetAvro)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.scalaXML,
          //CommonDeps.parquetAvro,
      )
  )

lazy val parquetAvro = Project(id = "parquet-avro", base = file("parquet-avro"))
  .dependsOn(zookeeper)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.akkaHttpAvro,
          CommonDeps.fastUtil,
          CommonDeps.parquetColumn,
          CommonDeps.parquetHadoop,
          CommonDeps.hadoopClient,
          CommonDeps.hadoopCommon,
          CommonDeps.scalaTest
      )
  )

lazy val convertor = Project(id = "convertor", base = file("convertor"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(hadoop)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.akkaStream,
          CommonDeps.scalaReflect.value,
          CommonDeps.scopt,
          CommonDeps.xs4s,
          CommonDeps.progressBar,
          CommonDeps.commonTypes,
          CommonDeps.scalaTest,
          CommonDeps.akkaHttpAvro
      ),
      //develop:on
      //      javaOptions ++= Seq(
      //          "-javaagent:../jrebel/jrebel.jar",
      //          "-noverify",
      //          "-XX:+UseConcMarkSweepGC",
      //          "-XX:+CMSClassUnloadingEnabled"
      //      ),
      //
      //      JRebelPlugin.jrebelSettings,
      //      jrebel.enabled := true
      //develop:off
  )

lazy val webServer = Project(id = "web-server", base = file("web-server"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  //.enablePlugins(JavaServerAppPackaging)
  .dependsOn(convertor)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.akkaActor,
          CommonDeps.akkaStream,
          CommonDeps.akkaHttp,
          CommonDeps.akkaHttpCirce,
          CommonDeps.akkaHttpAvro,
          CommonDeps.`color-loggers`,
          CommonDeps.scalaTest
      ),
      //develop:on
      //      javaOptions ++= Seq(
      //          "-javaagent:../jrebel/jrebel.jar",
      //          "-noverify",
      //          "-XX:+UseConcMarkSweepGC",
      //          "-XX:+CMSClassUnloadingEnabled"
      //      ),
      //
      //      JRebelPlugin.jrebelSettings,
      //      jrebel.enabled := true,
      //
      //      packageName in Docker := CommonSettings.settingValues.name,
      //      dockerRepository in Docker := Some("hub.docker.com"),
      //      dockerRepository := Some("ivcinform"),
      //      dockerUpdateLatest := false,
      //      dockerExposedPorts in Docker := Seq(8085),
      //      dockerBaseImage := "ivcinform/java-sdk:1.8.0.151-b12",
      //      dockerDocfileCommands := Seq(
      //          copy(s"universal/stage/", s"/opt/docker"),
      //          entrypoint("/opt/docker/bin/web-server")
      //      ),
      //develop:off

      // -------------- Prod ----------------------------
      mainClass in Compile := Some("ru.mfms.http.app.WebServerApp"),
      mappings in Universal ++= Seq(
          ((resourceDirectory in Compile).value / "logback.xml") -> "conf/logback.xml",
          ((resourceDirectory in Compile).value / "containerpilot.json") -> "etc/containerpilot.json"
      ),
      scriptClasspath := Seq("../conf/") ++ scriptClasspath.value,
      javaOptions ++= Seq("-Xms256m", "-Xmx1024m", "-Dmd.env.hostname=$HOST", "-Dmd.env.containerid=$MESOS_TASK_ID"),

      //?
      packageName in Docker := s"${CommonSettings.dockerGroupName}/${name.value.toLowerCase}",
      maintainer in Docker := "Andrey Yudin <ayudin@mfms.ru>",
      dockerBaseImage := "docker.mfms.ru/mfms/oracle-java:8-6",
      daemonUser in Docker := "dockrun",
      daemonGroup in Docker := "dockgrouprun",
      dockerEntrypoint := Seq("/opt/containerpilot/containerpilot", "/opt/config-preparer/var-expander.sh") ++ dockerEntrypoint.value,
      dockerCommands ++= Seq(
          Cmd("ENV", "BUILTIN_IMAGE_GROUP", CommonSettings.dockerGroupName),
          Cmd("ENV", "BUILTIN_IMAGE_NAME", name.value.toLowerCase),
          Cmd("ENV", "BUILTIN_VERSION", version.value),
          Cmd("ENV", "DEFAULT_CONF_DIR", "conf"),
          Cmd("ENV", "CONTAINERPILOT", """file:///opt/docker/etc/containerpilot.json"""),
          Cmd("ENV", "APP_FULLNAME", """/md/serveces/tss""")
      ),
      dockerRepository := Some("docker.mfms.ru"),
      dockerUpdateLatest := true,
      // -------------- End Prod ------------------------

      dockerAlias := DockerAlias(dockerRepository.value, None, (packageName in Docker).value, Some(version.value))
  )

lazy val test = Project(id = "test", base = file("test"))
  .dependsOn(common, hadoop, zookeeper, parquetAvro)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.akkaHttpCirce,
          CommonDeps.akkaHttpAvro,
          CommonDeps.hadoopClient
      )
  )
