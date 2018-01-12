package ru.mfms.convertor.app

import java.io.File
import java.security.PrivilegedExceptionAction

import com.sksamuel.avro4s.RecordFormat
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.parquet.hadoop.ParquetWriter
import pb.ProgressBar
import ru.mfms.config.{CoverterAppSettings, TsConfig}
import ru.mfms.convertor._
import ru.mfms.convertor.scopt._
import ru.mfms.convertor.xml.XML._
import ru.mfms.hdfs.AvroHelper.getAvroParquetWriter
import ru.mfms.hdfs.HDFSFileService
import ru.mfms.hdfs.HDFSFileService._
import ru.mfms.templates.{Templates, template}
import ru.mfms.zoo.Barrier
import scala.language.reflectiveCalls
import com.simplesys.control.ControlStructs._

import scala.util.{Failure, Success, Try}

//Запускать из проекта convertor

// run convert --encoding:parquetEncoding -b:convertor/src/main/resources --xmlFile:fixed_raiff_short.save.xml --outFile:fixed_raiff_short.save.parquet
// run convert --encoding:parquetEncoding -b:convertor/src/main/resources --xmlFile:fixed_raiff_short.save.xml
// run convert --encoding:parquetEncoding -b:convertor/src/main/resources --xmlFile:convertor/src/main/templatesfixed_sb.save.xml

// run convertBatch --baseDir:convertor/src/main/loaded_templates --replication:1
// run convertBatch --baseDir:convertor/src/main/templates --replication:1

object ConverterApp extends LazyLogging with TsConfig {
    def main(args: Array[String]): Unit = {
        val appSettings = config.get[CoverterAppSettings]("app").value
        implicit val barrier = new Barrier(appSettings.zoo)
        val ugi = UserGroupInformation.createRemoteUser(appSettings.hdfs.username)

        ugi.doAs {
            Try(
                new PrivilegedExceptionAction[Unit] {
                    override def run(): Unit = {
                        def getListOfFiles(dir: File, extensions: List[String]): List[File] = {
                            if (dir.exists && dir.isDirectory) {
                                dir.listFiles.filter(_.isFile).toList.filter(_.isFile).filter { file ⇒
                                    //logger debug file.getAbsolutePath
                                    extensions.exists(file.getName.endsWith(_))
                                }
                            } else {
                                List[File]()
                            }
                        }

                        ConfigApp.parser.parse(args, ConverterConfig()) match {
                            case Some(config) if config.mode == none =>
                                ConfigApp.parser.showUsage()

                            case Some(config) if config.mode == convertBatch =>
                                System.setProperty("hadoop.home.dir", "/")

                                var _config = config

                                if (config.baseDir.isEmpty)
                                    _config = config.copy(baseDir = Some(new File(".")))

                                if (config.outFile.isEmpty)
                                    _config = config.copy(outFile = Some(new File(ConfigApp.templatesFileName)))

                                logger debug _config.toString

                                val baseDir = config.baseDir.get

                                val files = getListOfFiles(baseDir, List("xml")).sortWith(_.getName < _.getName)
                                logger debug files.map(_.getName).mkString("\n\n ===================== Files for convertation =======================\n", "\n", "\n ===================== End Files for convertation ===================\n")

                                val outFile = _config.outFile.get
                                val outFilePath = s"hdfs://${_config.host}:${_config.port}${_config.hdfs_path}${outFile.getName}"

                                val record = RecordFormat[template]

                                val conf = new Configuration()
                                conf.setInt("dfs.replication", config.replication)
                                
                                using2(getAvroParquetWriter[template](outFilePath, conf))(barrier) {
                                    parquetWriter ⇒
                                        barrier.open()
                                        logger.debug("\n")
                                        files.foreach {
                                            file ⇒
                                                val fn = file.getName
                                                val pb = new ProgressBar(getCountFixedTempl(file), Some(fn))
                                                pb.showSpeed = false
                                                import Templates._

                                                val fixedTemplaterActor = getFixedTemplaterActor(file)
                                                implicit val customer: String = fixedTemplaterActor.acnAttr

                                                getFixedTmplFromXML(file).foreach {
                                                    item ⇒                                                       
                                                        parquetWriter write (record to (item: template))
                                                        pb += 1
                                                }
                                                logger.debug(s"Done: ($fn)\n")
                                        }
                                }
                                logger.debug(s"File: ${outFile.getName} recorded in DFS (hdfs://${_config.host}:${_config.port}${_config.hdfs_path})\n")

                            case Some(config) if config.mode == convert =>
                                // Не ипользуется, хотя возможено, если нужно импортировать какой-то один файл !!!!
                                implicit val fileService: HDFSFileService = new HDFSFileService(appSettings.hdfs.host, appSettings.hdfs.port, 128 * 1024 * 1024, appSettings.hdfs.replication)

                                recConvertedFile(encoding = config.encoding, host = config.host, port = config.port, replication = config.replication, path = config.hdfs_path, inputXmlFile = config.getXmlFile, config.getOutFile.map(_.getName))

                            case Some(config) if config.mode == test =>
                                println("Hello Dolly !!!")

                            case None =>
                                ConfigApp.parser.showUsage()
                                logger.debug("For example: convertBatch --baseDir:../templates --replication:1")
                                logger.debug("OR")
                                logger.debug("For example: convert --xmlFile:convertor/src/main/templatesfixed_sb.save.xml --replication:1")
                        }
                    }
                }) match {
                case Success(res) ⇒ res
                case Failure(e) ⇒ throw e
            }
        }

        sys.addShutdownHook {
            () ⇒
                logger info s"system terminating"
                //barrier.shudown()
                logger info s"system terminated"
        }
    }

}
