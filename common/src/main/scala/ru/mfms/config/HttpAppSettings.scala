package ru.mfms.config

import scala.concurrent.duration.Duration

case class Zoo(timeout: Duration, connectString: String, path:String)

case class HttpAppSettings(name: String, http: HttpSettings, hdfs: Hdfs, zoo: Zoo)

case class HttpSettings(host: String,
                        port: Int,
                        pathUpload: String,
                        pathDownload: String,
                        pathDelete: String,
                        pathGetSchema: String,
                        pathAppend: String,
                        server: ServerSettings
                       )

case class ServerSettings(parallelism: Int)
case class Hdfs(host: String, port: Int, path: String, templatesFileName: String, replication: Int, username: String, blockSize: Int)

case class CoverterAppSettings(baseDir: String, hdfs: Hdfs, zoo: Zoo)
