package ru.mfms

import java.io.{File, FileReader}
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.Implicits._
import com.scalawilliam.xs4s.XmlElementExtractor
import com.simplesys.control.ControlStructs._
import com.sksamuel.avro4s.{AvroSchema, RecordFormat}
import com.typesafe.scalalogging.LazyLogging
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import ru.mfms.convertor.scopt.{ConfigApp, Encoding}
import ru.mfms.convertor.xml.FixedTemplaterActor
import ru.mfms.convertor.xml.XML._
import ru.mfms.hdfs.AvroHelper.{getAvroParquetReader, getAvroParquetWriter}
import ru.mfms.hdfs.HDFSFileService
import ru.mfms.hdfs.HDFSFileService._
import ru.mfms.templates.Templates._
import ru.mfms.templates.template
import ru.mfms.zoo.Barrier

package object convertor extends LazyLogging {
    def getFixedTemplaterActor(inputXmlFile: File): FixedTemplaterActor = {
        val xmlInputfactory = XMLInputFactory.newInstance()
        val splitter = XmlElementExtractor.collectElements(_.last == "fixedTemplaterActor")

        val fileReader = new FileReader(inputXmlFile)
        val reader = xmlInputfactory.createXMLEventReader(fileReader)

        (for {
            fixedTemplaterActor ← reader.toIterator.scanCollect(splitter.Scan)
            res = FixedTemplaterActor(
                acnAttr = fixedTemplaterActor \@ "acn"
            )
        } yield res).toList.head
    }

    def recConvertedFile(encoding: Encoding, host: String, port: Int, replication: Int, path: String, inputXmlFile: Option[File], outFile: Option[String] = None, realFileName: Option[String] = None)(implicit fileService: HDFSFileService, barrier: Barrier): String = {
        inputXmlFile.map {
            inputXmlFile ⇒
                val fixedTemplaterActor = getFixedTemplaterActor(inputXmlFile)
                implicit val customer: String = fixedTemplaterActor.acnAttr

                def getOutFile(extension: String): File = {
                    val res = if (outFile.isDefined) new File(inputXmlFile.getParent, s"${outFile.get}.$extension") else new File(ConfigApp.templatesFileName)
                    //logger debug s"==>> OutFile: ${res.getAbsolutePath}"
                    res
                }

                encoding match {
                    case scopt.parquetEncoding ⇒

                        implicit val avroSchema: Schema = AvroSchema[template]
                        System.setProperty("hadoop.home.dir", "/")

                        val fileName = getOutFile("parquet").getName
                        val outFile: String = s"hdfs://$host:$port$path" + fileName
                        val conf = new Configuration()
                        conf.setInt("dfs.replication", replication)

                        val record = RecordFormat[template]

                        val seqExists = getSeqExists(host, port, path + fileName)

                        barrier.open()
                        using2(getAvroParquetWriter[template](outFile, conf))(barrier) {
                            parquetWriter ⇒
                                (getFixedTmplFromXML(inputXmlFile).map(item ⇒ item: template) ++ seqExists).foreach(item ⇒ parquetWriter write (record to item))
                        }

                        s"File: ${realFileName.getOrElse(inputXmlFile.getName)} converted and recorded in DFS (hdfs://${host}:${port}${path}) as $fileName \n"
                }
        }.getOrElse("")
    }

    def getSeqExists(host: String, port: Int, dirPath: String)(implicit fileService: HDFSFileService): Seq[template] = {
        val record = RecordFormat[template]
        val dirExists = fileService.exists(dirPath)
        val fullFileName = s"hdfs://$host:$port$dirPath"
        if (dirExists)
            getSeqExists(fullFileName)
        else
            Seq()
    }

    def getSeqExists(fullFileName: String)(implicit fileService: HDFSFileService): Seq[template] = {
        val record = RecordFormat[template]
        using(getAvroParquetReader(fullFileName)) {
            reader ⇒
                Iterator.continually(reader.read)
                  .takeWhile(_ != null)
                  .map(record.from)
                  .toSeq
        }
    }
}
