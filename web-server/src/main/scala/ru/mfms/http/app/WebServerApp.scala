package ru.mfms.http.app

import java.io.File
import java.security.PrivilegedExceptionAction

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route.asyncHandler
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import com.simplesys.circe.Circe._
import com.simplesys.control.ControlStructs._
import com.sksamuel.avro4s.{AvroSchema, RecordFormat, SchemaFor}
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._
import de.heikoseeberger.akkahttpcirce._
import io.circe.syntax._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{LocatedFileStatus, RemoteIterator}
import org.apache.hadoop.security.UserGroupInformation
import ru.mfms.config.{HttpAppSettings, TsConfig}
import ru.mfms.convertor.scopt.Encoding
import ru.mfms.convertor.{recConvertedFile, _}
import ru.mfms.hdfs.AvroHelper.getAvroParquetWriter
import ru.mfms.hdfs.HDFSFileService
import ru.mfms.hdfs.HDFSFileService._
import ru.mfms.http.structs.{DeleteFile, DownloadData}
import ru.mfms.templates.templateSerDeser._
import ru.mfms.templates.{atp, created, template}
import ru.mfms.zoo.Barrier

import scala.compat.Platform.EOL
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

//http://0.0.0.0:8085/mfmd/dfs/get_schema
//curl -F "parquetEncoding=@convertor/src/main/templates/fixed_raiff_short.save.xml"  http://localhost:8085/mfmd/dfs/upload
//curl -F "parquetEncoding=@convertor/src/main/templates/fixed_raiff_short1.save.xml"  http://localhost:8085/mfmd/dfs/upload
//curl -F "parquetEncoding=@convertor/src/main/templates/fixed_raiff.save.xml"  http://localhost:8085/mfmd/dfs/upload
//curl -F "parquetEncoding=@convertor/src/main/templates/fixed_sb.save.xml"  http://localhost:8085/mfmd/dfs/upload
//curl -F "parquetEncoding=@convertor/src/main/templates/fixed_raiff_short.save.xml" -F "parquetEncoding=@convertor/src/main/templates/fixed_raiff.save.xml"  http://localhost:8085/mfmd/dfs/upload

//curl -H "Content-Type: application/json" -X POST -d '[{"filePath":"/templates/fixed_raiff_short.save.xml.parquet"}, {"filePath":"/templates/fixed_raiff.save.xml.parquet"}]' http://localhost:8085/mfmd/dfs/delete
//curl -H "Content-Type: application/json" -X POST -d '[{"filePath":"/templates/fixed_raiff_short.save.xml.parquet"}]' http://localhost:8085/mfmd/dfs/delete
//curl -H "Content-Type: application/json" -X POST -d '[{"filePath":"/templates/*"}]' http://0.0.0.0:8085/mfmd/dfs/delete

//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb","raiff"],"format":"json","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb"],"format":"json","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb"],"format":"json-b","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":[],"format":"json","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb"],"format":"binary","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb"],"format":"json","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["raiff"],"format":"binary","details":true}' http://localhost:8085/mfmd/dfs/download
//curl -H "Content-Type: application/json" -X POST -d '{"clients":["sb","raiff"],"format":"binary","details":true}' http://localhost:8085/mfmd/dfs/download
//curl http://localhost:8085/health/ruok

object WebServerApp extends App with LazyLogging with TsConfig {
    val appSettings = config.get[HttpAppSettings]("app").value
    implicit val barrier = new Barrier(appSettings.zoo)
    //appSettings.toString.log

    implicit val system = ActorSystem(appSettings.name)
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val line = "===================================================================================================="

    implicit def convertToScalaIterator[T](underlying: RemoteIterator[T]): Iterator[T] = {
        case class wrapper(underlying: RemoteIterator[T]) extends Iterator[T] {
            override def hasNext = underlying.hasNext

            override def next = underlying.next
        }
        wrapper(underlying)
    }

    import Directives._
    //import CirceEnum._ //Необходим для правильного отображения Enum типа case object fro sealed trait; в общем случае это имеет вид {"name":{}}
    import CirceEnum._ //Необходим для правильного отображения Enum типа case object fro sealed trait; в общем случае это имеет вид {"name":{}}
    import FailFastCirceSupport._
    import io.circe.generic.auto._
    //import io.circe.java8.time._ //Должен быть !!!!
    import io.circe.java8.time._ //Должен быть !!!!

    System.setProperty("hadoop.home.dir", "/")
    val record = RecordFormat[template]

    def route: Route = {
        val ugi = UserGroupInformation.createRemoteUser(appSettings.hdfs.username)
        ugi.doAs {
            Try {
                new PrivilegedExceptionAction[Route] {
                    override def run(): Route = {
                        implicit val fileService = new HDFSFileService(appSettings.hdfs.host, appSettings.hdfs.port, appSettings.hdfs.blockSize, appSettings.hdfs.replication)
                        val fileName = s"hdfs://${appSettings.hdfs.host}:${appSettings.hdfs.port}${appSettings.hdfs.path}${appSettings.hdfs.templatesFileName}"
                        Route.seal(
                            path(separateOnSlashes(appSettings.http.pathUpload)) {
                                withoutSizeLimit {
                                    (post & entity(as[Multipart.FormData])) { formData ⇒
                                        complete {
                                            processFile(formData).map { info ⇒
                                                HttpResponse(StatusCodes.OK, entity = s"\n$line\n" + info + line + "\n\n")
                                            }.recover {
                                                case ex: Throwable ⇒
                                                    HttpResponse(StatusCodes.InternalServerError, entity = ex.getStackTrace().mkString("", EOL, EOL))
                                            }
                                        }
                                    }
                                }
                            } ~ path(separateOnSlashes(appSettings.http.pathAppend)) {
                                (post & entity(as[Seq[template]])) { templates ⇒

                                    val seqExists = getSeqExists(appSettings.hdfs.host, appSettings.hdfs.port, appSettings.hdfs.path + appSettings.hdfs.templatesFileName)

                                    val conf = new Configuration()
                                    conf.setInt("dfs.replication", appSettings.hdfs.replication)
                                    barrier.open()
                                    using2(getAvroParquetWriter[template](fileName, conf = conf))(barrier) {
                                        parquetWriter ⇒
                                            (templates ++ seqExists).foreach(item ⇒ parquetWriter write (record to item))
                                    }

                                    complete("Опрерация выполнена.")
                                }
                            } ~ path(separateOnSlashes(appSettings.http.pathDelete)) {
                                (post & entity(as[Seq[DeleteFile]])) { files ⇒
                                    val res = files.map {
                                        file ⇒
                                            def removeFile(filePath: String): String = {
                                                if (fileService.removeFile(filePath))
                                                    s"File: $filePath deleted."
                                                else
                                                    s"File: $filePath not deleted."
                                            }

                                            if (file.filePath.last == '*') {
                                                val path = file.filePath.dropRight(1)
                                                val iterator: Iterator[LocatedFileStatus] = fileService.listFiles(path)
                                                iterator.map(fileStatus ⇒ removeFile(path + fileStatus.getPath.getName)).mkString("\n")
                                            } else {
                                                removeFile(file.filePath)
                                            }
                                    }.mkString("\n" + line + "\n", "\n", "\n" + line + "\n\n")
                                    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, res))
                                }
                            } ~ path(separateOnSlashes(appSettings.http.pathDownload)) {
                                (post & entity(as[DownloadData])) { download ⇒

                                    def getResultData: Seq[template] = {
                                        if (download.clients.nonEmpty)
                                            getSeqExists(appSettings.hdfs.host, appSettings.hdfs.port, appSettings.hdfs.path + appSettings.hdfs.templatesFileName)
                                              .filter(item ⇒ download.clients.contains(item.customer))
                                              .map(item ⇒ item.copy(examples = if (download.details) item.examples else Seq(), history = if (download.details) item.history else item.history.filter(_.`type` == created)))
                                        else
                                            getSeqExists(appSettings.hdfs.host, appSettings.hdfs.port, appSettings.hdfs.path + appSettings.hdfs.templatesFileName)
                                              .map(item ⇒ item.copy(examples = if (download.details) item.examples else Seq(), history = if (download.details) item.history else item.history.filter(_.`type` == created)))
                                    }

                                    barrier.open()
                                    try {
                                        download.format match {
                                            case "json" ⇒
                                                complete(getResultData)

                                            case "json-b" ⇒
                                                complete(HttpEntity(ContentTypes.`application/json`, getResultData.asJson.spaces41))

                                            case "binary" ⇒
                                                complete(HttpEntity(ContentTypes.`application/octet-stream`, getResultData))
                                        }    
                                    } finally{
                                        barrier.close()
                                    }
                                }
                            } ~
                              path(separateOnSlashes(appSettings.http.pathGetSchema)) {
                                  get {
                                      implicit val schemaFor = SchemaFor[Seq[atp]]
                                      complete(HttpResponse(StatusCodes.OK, entity = AvroSchema[template].toString(true)))
                                  }
                              } ~
                              path("health" / "ruok") {
                                  get {
                                      complete("imok")
                                  }
                              }
                        )
                    }
                }
            } match {
                case Success(res) ⇒ res
                case Failure(e) ⇒ throw e
            }
        }
    }

    private def processFile(formData: Multipart.FormData)(implicit fileService: HDFSFileService): Future[String] = {
        formData.parts.filter(part ⇒ part.filename.isDefined).mapAsync(1) { part ⇒
            val tmpFile = File.createTempFile("akka-http-upload-ts", ".xml")
            part.entity.dataBytes.runWith(FileIO.toPath(tmpFile.toPath)).map(_ ⇒ (FileInfo(part.name, part.filename.get, part.entity.contentType), tmpFile))

        }.runFold("") {
            case (str, (fileInfo, file)) ⇒
                Encoding.getObject(fileInfo.fieldName) match {
                    case encoding ⇒

                        //logger info s"============================>>> tmpFile : ${file.getAbsolutePath}, fileInfo: $fileInfo "
                        val res = recConvertedFile(encoding = encoding, host = appSettings.hdfs.host, port = appSettings.hdfs.port, replication = appSettings.hdfs.replication, path = appSettings.hdfs.path, inputXmlFile = Some(file), realFileName = Some(fileInfo.fileName))
                        file.delete()
                        str + res
                }
        }
    }

    val server = Http(system).bind(appSettings.http.host, appSettings.http.port)

    val bindingFuture =
        server
          .to(Sink.foreach(_.handleWithAsyncHandler(asyncHandler(route), appSettings.http.server.parallelism)))
          .run()

    sys.addShutdownHook {
        () ⇒
            logger info s"system terminating"
            barrier.shudown()
            system.terminate()
            logger info s"system terminated"
    }

}
