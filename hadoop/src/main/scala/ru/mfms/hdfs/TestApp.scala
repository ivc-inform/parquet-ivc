package ru.mfms.hdfs

import java.io._
import java.security.PrivilegedExceptionAction
import java.util.Timer

import HDFSFileService._
import org.apache.hadoop.security.UserGroupInformation

import scala.util.{Failure, Success, Try}

object TestApp extends App {
    val testfileName = "testfile.txt"
    val testText = "Example text"
    val ugi: UserGroupInformation = UserGroupInformation.createRemoteUser("hdfs")

    ugi.doAs {
        Try {
            new PrivilegedExceptionAction[Unit] {
                override def run(): Unit = {
                    val fileService = new HDFSFileService("localhost", 9820, 128 * 1024 * 1024, 1)

                    val testfile = new File(testfileName)
                    testfile.delete
                    testfile.createNewFile
                    val testfileWriter = new BufferedWriter(new FileWriter(testfile))
                    testfileWriter.write(testText)
                    testfileWriter.close
                    fileService.moveFromLocalFile(testfileName, "/templates/" + testfileName)
                    println(testfile.exists == false)

                    //                val res = fileService.removeFile("/templates/" + testfileName)
                    //                println(res)
                }
            }
        } match {
            case Success(res) ⇒ res
            case Failure(e) ⇒ throw e
        }
    }
}
