import java.io.File

import com.simplesys.circe.Circe._
import com.simplesys.common.Strings._
import com.simplesys.common.XMLs._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import org.scalatest.FunSuite
import ru.mfms.convertor.xml.{FixedTmpl, XML}
import ru.mfms.templates.Templates._
import ru.mfms.templates._

import scala.io.Source
import scala.xml.Elem
import scala.xml
import ru.mfms.convertor._
//import io.circe.java8.time._ //Должен быть !!!!
import io.circe.java8.time._ //Должен быть !!!!
import scala.xml.Text

case class template1(id: String, value: String, customer: String, status: templateStatus /*, defaultValues: Seq[defaultValue], structure: Seq[atp], examples: Seq[OutMessage], history: Seq[historyElement]*/)

class TestSuit extends FunSuite {

    def getLines(resourceName: String): Iterator[String] = {
        val is = getClass.getClassLoader.getResourceAsStream(resourceName)
        Source.fromInputStream(is)(scala.io.Codec.UTF8).getLines().filter(_.nonEmpty)
    }

    test("timeParced") {
        println("2015-09-03T15:15:10.333".toLocalDateTime())
        println("2015-09-03T15:15:10.333+03:00".toLocalDateTime())
    }

    test("XML") {
        val a: Option[xml.Text] = Some(Text("1"))
        val b: Option[xml.Text] = Some(Text("2"))
        //None
        val c = true
        val res = <test a={a} b={b} c={c.toString}></test>
        println(res.toString())
    }

    test("XML1") {
        val file = new File("convertor/src/main/loaded_templates1/fixed_a3.save.xml")
        val fixedTemplaterActor = getFixedTemplaterActor(file)
        implicit val customer: String = fixedTemplaterActor.acnAttr

        val a: Seq[FixedTmpl] = XML.getFixedTmplFromXML(file)
        val aa: Seq[template] = a.map {
            item ⇒
                val _item: template = item
                _item
        }

        //        a.foreach(println)
        //        println(s"${newLine.newLine} ================================================================================================================================================================")
        //        aa.foreach(println)
        //        println(s"${newLine.newLine} ================================================================================================================================================================")

        //XML.getXMLFromSeqTemplates(aa).foreach(item ⇒ println(item._2.toPrettyString()))

        val rootTag: Elem = <fixedTemplateHolder/>
        XML.getXMLFromSeqTemplates(aa).foreach {
            case (customer, item) ⇒
                item.foreach { st ⇒
                    println(st.toPrettyString())
                    st
                }
        }
    }

    test("json") {
        val json = parse(getLines("seqtemplate.json").mkString).getOrElse(Json.Null)
        /* val t = Vector(template(
             id = "60cf738e-28c8-474d-bff7-d987ad0ebb07",
             value = "пароль_$T{$STRING}_оплата_$MONEY_$T{$VARSTRING}",
             customer = "a3",
             status = Candidate,
             defaultValues = Seq(
                 defaultValue(
                     parameter = parameter(
                         paramName = "Тип транзакции",
                         paramDescription = Some(""),
                         paramCategory = "Транзакционные данные"
                     ),
                     defaultValue = "PMT"
                 )),
             structure = Seq(
                 atp(
                     parameter = None,
                     templatePart = templatePart(
                         lexem = None,
                         directionOfUse = MustBe,
                         `type` = TemplatePartTypeEnumLexem,
                         structureType = SimplePart
                     )
                 )
             ),
             examples = Seq.empty,
             history = Seq.empty
         ))

         val json = t.asJson*/

        println(json.spaces41)

        println(json.as[Vector[template]].getOrElse(null))
    }

    test("Long") {
        println("5368502.001".toBigDecimal.toLong)
    }
}
