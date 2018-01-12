import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.packager.docker.Cmd
import ru.simplesys.sbprocessing.sbtbuild.{CommonDeps, CommonSettings}

lazy val parquetIVC = (project in file("."))
  .enablePlugins(GitVersioning)
  .aggregate(
      common,
      parquetAvro,
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
          CommonDeps.configWrapper,
          CommonDeps.scalaTest
      )
  )

lazy val parquetAvro = Project(id = "parquet-avro", base = file("parquet-avro"))
  .dependsOn(common)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.fastUtil,
          CommonDeps.parquetColumn,
          CommonDeps.parquetHadoop,
          CommonDeps.hadoopClient,
          CommonDeps.hadoopCommon,
          CommonDeps.scalaTest
      )
  )



lazy val test = Project(id = "test", base = file("test"))
  .dependsOn(common, parquetAvro)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.hadoopClient
      )
  )
