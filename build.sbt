import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.packager.docker.Cmd
import ru.simplesys.sbprocessing.sbtbuild.{CommonDeps, CommonSettings}

lazy val parquetIVC = (project in file("."))
  .enablePlugins(GitVersioning)
  .aggregate(
      common,
      parquetCommon,
      parquetAvro,
      parquetHadoop,
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

lazy val parquetCommon = Project(id = "parquet-common", base = file("parquet-common"))
  .dependsOn(common)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.`org.slf4j`,
          CommonDeps.parquetFormat,
          CommonDeps.scalaTest
      )
  )

lazy val parquetAvro = Project(id = "parquet-avro", base = file("parquet-avro"))
  .dependsOn(parquetHadoop)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.fastUtil,
          CommonDeps.parquetColumn,
          CommonDeps.hadoopClient,
          CommonDeps.hadoopCommon,
          CommonDeps.scalaTest
      )
  )

lazy val parquetHadoop = Project(id = "parquet-hadooop", base = file("parquet-hadooop"))
  .dependsOn(common, parquetCommon)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.parquetColumn,
          CommonDeps.hadoopClient,
          CommonDeps.parquetJackson,
          CommonDeps.jackson,
          CommonDeps.jacksonCore,
          CommonDeps.`snappy-java`,
          CommonDeps.`commons-pool`,
          //CommonDeps.`brotli-codec`,
          CommonDeps.scalaTest
      ),
      //resolvers += Resolver.url("jitpack.io", url("https://jitpack.io"))
  )

lazy val test = Project(id = "test", base = file("test"))
  .dependsOn(common, parquetAvro)
  .settings(
      libraryDependencies ++= Seq(
          CommonDeps.avro,
          CommonDeps.hadoopClient
      )
  )
