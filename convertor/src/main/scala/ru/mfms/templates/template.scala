package ru.mfms.templates

import java.io.ByteArrayOutputStream

import com.sksamuel.avro4s.{AvroDoc, AvroInputStream, AvroOutputStream}
import ru.mfms.mfmd.integration.connector.OutMessage

import scala.collection.immutable

case class parameter(paramName: String, paramDescription: Option[String], paramCategory: String)

case class templatePart(lexem: Option[String], directionOfUse: directionOfUse, `type`: templatePartType, structureType: structureType)

@AvroDoc("History of template changes")
case class historyElement(name: String, date: String, `type`: historyElementType)

@AvroDoc("Contains array of atp??")
case class atp(parameter: Option[parameter], templatePart: templatePart)

@AvroDoc("Contains array of default values")
case class defaultValue(parameter: parameter, defaultValue: String)

case class template(id: String, value: String, customer: String, status: templateStatus, defaultValues: Seq[defaultValue], structure: Seq[atp], examples: Seq[OutMessage], history: Seq[historyElement])

object templateSerDeser {
    implicit def template2BytesArray(seq: Seq[template]): Array[Byte] = {
        val baos = new ByteArrayOutputStream()
        val output = AvroOutputStream.binary[template](baos)
        seq.foreach(output.write)
        output.close()
        baos.toByteArray
    }

    implicit def bytesArray2TemplatesSeq(bytes: Array[Byte]): immutable.Seq[template] = {
        val is = AvroInputStream.binary[template](bytes)
        val res = is.iterator.to[immutable.Seq]
        is.close()
        res
    }
}

