package ru.mfms.http.structs

case class DownloadData(clients: Vector[String] = Vector.empty, format: String = "json", details: Boolean = false) {
    override def toString = s"===================>>> Download: [clients: ${clients.mkString("[", ",", "]")}, format: $format, details: $details]"
}
