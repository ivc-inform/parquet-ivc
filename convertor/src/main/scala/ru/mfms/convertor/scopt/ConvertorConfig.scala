package ru.mfms.convertor.scopt

import java.io.File

import configs.syntax._
import ru.mfms.config.{CoverterAppSettings, TsConfig}
import scopt.OptionParser

abstract sealed trait ModeRun

case object none extends ModeRun
case object test extends ModeRun
case object convert extends ModeRun
case object convertBatch extends ModeRun

object ModeRun {
    private val values = SealedEnumRuntime.values[ModeRun]
    private val mappedKeys: Map[String, ModeRun] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[ModeRun, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): ModeRun = mappedKeys(objName)
    def getName(obj: ModeRun): String = mappedObject(obj)
}

abstract sealed trait Encoding
case object parquetEncoding extends Encoding

object Encoding {
    private val values = SealedEnumRuntime.values[Encoding]
    private val mappedKeys: Map[String, Encoding] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[Encoding, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): Encoding = mappedKeys(objName)
    def getName(obj: Encoding): String = mappedObject(obj)
}

object ConverterConfig {
    def replaceBaseDir(file: File, baseDir: Option[File]): File =
        if (baseDir.isDefined && !file.isAbsolute)
            new File(baseDir.get, file.getPath)
        else
            file

    def replaceExtension(file: File, newExtension: String): File = {

        val fileName = {
            val a = file.getAbsolutePath
            val dotIndx = a.lastIndexOf('.')
            if (dotIndx == -1)
                a
            else
                a.substring(0, dotIndx)
        }

        new File(fileName + "." + newExtension)
    }
}

case class ConverterConfig(
                            mode: ModeRun = none,
                            baseDir: Option[File] = None,
                            xmlFile: Option[File] = None,
                            outFile: Option[File] = None,
                            encoding: Encoding = parquetEncoding,
                            host: String = ConfigApp.appSettings.hdfs.host,
                            port: Int = ConfigApp.appSettings.hdfs.port,
                            hdfs_path: String = ConfigApp.appSettings.hdfs.path,
                            replication: Int = ConfigApp.appSettings.hdfs.replication
                          ) {

    import ConverterConfig._

    def getXmlFile: Option[File] = xmlFile.map(replaceBaseDir(_, baseDir))

    def getOutFile = outFile

    override def toString = s"mode: ${ModeRun.getName(mode)}, baseDir: ${baseDir.getOrElse("None")}, xmlFile: ${xmlFile.getOrElse("None")}, outFile: ${outFile.getOrElse("None")}, encoding: ${Encoding.getName(encoding)}, host: $host, port: $port, hdfs_path: $hdfs_path, replication: $replication"
}

object ConfigApp extends TsConfig{
    val appSettings = config.get[CoverterAppSettings]("app").value

    implicit class StrOpts(str: String) {
        def isEmpty = str == null || str.isEmpty
    }

    implicit val optionFileRead: scopt.Read[Option[File]] = scopt.Read.reads {
        v ⇒
            if (v.isEmpty)
                None
            else
                Some(new File(v))
    }

    implicit val optionBooleanRead: scopt.Read[Option[Boolean]] = scopt.Read.reads {
        v ⇒
            if (v.isEmpty)
                None
            else
                Some(v.toBoolean)
    }

    implicit val encodingRead: scopt.Read[Encoding] = scopt.Read.reads {
        v ⇒
            if (v.isEmpty)
                parquetEncoding
            else
                Encoding.getObject(v)
    }

    val templatesFileName = "templates.xml.parquet"

    val parser = new OptionParser[ConverterConfig]("ConverterApp") {
        head("converter", "1.x")


        cmd(convertBatch.getClass.getSimpleName.replace("$", "")).action((_, c) => c.copy(mode = convertBatch))
          .text("Конвертация xml - файлов шаблонизатора, с последующей записью в DFS")
          .children(

              opt[Option[File]]('b', "baseDir").optional.valueName("<directory-path>").
                action((baseDir, config) =>
                    config.copy(baseDir = baseDir)).text("Base directory, optional, default(.)"),

              opt[Option[File]]("outFile").optional.valueName("<file-path>").
                action((outFile, config) => config.copy(outFile = outFile)).
                text(s"outFile, optional, default($templatesFileName)"),

              opt[String]('h', "host").optional.valueName("<url>").
                action((host, config) =>
                    config.copy(host = host)).text(s"Host of client DFS:  default(${ConfigApp.appSettings.hdfs.host})"),

              opt[Int]('p', "port").optional.valueName("<int value>").
                action((port, config) =>
                    config.copy(port = port)).text(s"Port of client DFS:  default(${ConfigApp.appSettings.hdfs.port})"),

              opt[Int]("replication").optional.valueName("<int value>").
                action((replication, config) =>
                    config.copy(replication = replication)).text(s"Replication rate DFS:  default(${ConfigApp.appSettings.hdfs.replication})"),

              opt[String]("hdfs_path").optional.valueName("<path>").
                action((hdfs_path, config) =>
                    config.copy(hdfs_path = hdfs_path)).text(s"Path on DFS:  default(${ConfigApp.appSettings.hdfs.path})")
          )

        cmd(convert.getClass.getSimpleName.replace("$", "")).action((_, c) => c.copy(mode = convert))
          .text("Конвертация xml - файла шаблонизатора, с последующей записью в DFS")
          .children(
              opt[Option[File]]("xmlFile").required.valueName("<file-path>").
                action((xmlFile, config) => config.copy(xmlFile = xmlFile)).
                text(s"xmlFile, optional, default($templatesFileName)"),

              opt[Option[File]]("outFile").optional.valueName("<file-path>").
                action((outFile, config) => config.copy(outFile = outFile)).
                text(s"outFile, optional, default($templatesFileName)"),

              opt[String]('h', "host").optional.valueName("<url>").
                action((host, config) =>
                    config.copy(host = host)).text("Host of client DFS:  default(dev.db-support.ru)"),

              opt[Int]('p', "port").optional.valueName("<int value>").
                action((port, config) =>
                    config.copy(port = port)).text("Port of client DFS:  default(8020)"),

              opt[Int]("replication").optional.valueName("<int value>").
                action((replication, config) =>
                    config.copy(replication = replication)).text("Replication rate DFS:  default(3)"),

              opt[String]("hdfs_path").optional.valueName("<path>").
                action((hdfs_path, config) =>
                    config.copy(hdfs_path = hdfs_path)).text(s"Path on DFS:  default(${ConfigApp.appSettings.hdfs.path})")
          )

        cmd(test.getClass.getSimpleName.replace("$", "")).action((_, c) => c.copy(mode = test))
          .text("Конвертация xml - файла шаблонизатора, с последующей записью в DFS")
          .children(
              opt[Option[File]]("xmlFile").required.valueName("<file-path>").
                action((xmlFile, config) => config.copy(xmlFile = xmlFile)).
                text(s"xmlFile, optional, default($templatesFileName)"),
          )
    }
}
