package ru.mfms.hdfs

import java.io.{File, InputStream}
import java.util.Properties

import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import org.apache.hadoop.security.UserGroupInformation
import org.apache.log4j.PropertyConfigurator
import ru.mfms.hdfs.utils.SourceInputStream._

import scala.io.Source

object HDFSFileService {
    implicit def str2Path(str: String): Path = new Path(str)

    def findFile(name: String, file: File): Option[File] =
        Option(file.listFiles) match {
            case None ⇒ None
            case Some(list) ⇒
                list.flatMap {
                    file ⇒
                        if (file.isDirectory)
                            findFile(name, file)
                        else if (file.getName == name)
                            Some(file)
                        else
                            None
                }.headOption
        }

    val props = new Properties()

    props.put("log4j.rootLogger", "WARN, A")
    props.put("log4j.appender.A", "com.mihnita.colorlog.log4j.JAnsiColorConsoleAppender")
    props.put("log4j.appender.A.layout", "org.apache.log4j.EnhancedPatternLayout")
    props.put("log4j.appender.A.layout.ConversionPattern", "Jansi> %-5p: %c{2} [%t] - %m%n")

    PropertyConfigurator configure props
}

class HDFSFileService(host: String, port: Int, blockSize: Int, replication: Int) extends LazyLogging {

    import HDFSFileService._

    private val conf = new Configuration()
    System.setProperty("hadoop.home.dir", "/")

    conf.set("fs.defaultFS", s"hdfs://$host:$port")
    conf.set("dfs.replication", replication.toString)
    conf.set("dfs.blocksize", blockSize.toString)
    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
    conf.set("dfs.support.append", true.toString)

    private val fileSystem: FileSystem = FileSystem.get(conf)

    def moveFromLocalFile(src: Path, dst: Path): Unit = fileSystem.moveFromLocalFile(src, dst)

    def removeFile(filename: String): Boolean = {
        val res = fileSystem.delete(filename, true)
        if (res)
            logger info s"file : $filename removed."
        else
            logger info s"file : $filename not removed."
        res
    }

    def listFiles(path: String) = fileSystem.listFiles(path, false)

    def append(path: String) = fileSystem.append(path)

    def exists(filename: String): Boolean = fileSystem.exists(filename)

    def getFile(filename: String): InputStream = fileSystem.open(filename)

    def createFolder(folderPath: String): Unit = if (!fileSystem.exists(folderPath)) fileSystem.mkdirs(folderPath)

    def close() = fileSystem.close()
}
