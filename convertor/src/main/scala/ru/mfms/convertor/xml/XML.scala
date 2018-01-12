package ru.mfms.convertor.xml

import java.io.{File, FileReader}
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.Implicits._
import com.scalawilliam.xs4s.XmlElementExtractor
import com.simplesys.common.Strings._
import com.typesafe.scalalogging.LazyLogging
import ru.mfms.mfmd.integration.common.{ParsingEntity, SmsMessage}
import ru.mfms.templates.Templates._
import ru.mfms.templates.template

import scala.collection.immutable
import scala.util.{Failure, Success, Try}
import scala.xml._

object XML extends LazyLogging {
    def getCountFixedTempl(inputXmlFile: File): Int = {
        val xmlInputfactory = XMLInputFactory.newInstance()
        val splitter = XmlElementExtractor.collectElements(_.last == "fixedTmpl")

        val fileReader = new FileReader(inputXmlFile)
        val reader = xmlInputfactory.createXMLEventReader(fileReader)

        reader.toIterator.scanCollect(splitter.Scan).foldLeft(0)((counter, _) ⇒ counter + 1)
    }

    def getFixedTmplFromXML(inputXmlFile: File): Stream[FixedTmpl] = {

        val xmlInputfactory = XMLInputFactory.newInstance()
        val splitter = XmlElementExtractor.collectElements(_.last == "fixedTmpl")

        val fileReader = new FileReader(inputXmlFile)
        val reader = xmlInputfactory.createXMLEventReader(fileReader)

        Try {

            for {
                fixedTmpl ← reader.toIterator.scanCollect(splitter.Scan)

                userComments: Option[String] = (fixedTmpl \ "userComments" map (_.text)).headOption

                asStringAttr: String = fixedTmpl \@ "asString"

                isIgnoreDataAttr: Boolean = (fixedTmpl \@ "isIgnoreData").asBoolean

                isValidatedAttr: Boolean = (fixedTmpl \@ "isValidated").asBoolean

                paramDefaultValues: Option[ParamDefaultValues] = {
                    val res = (fixedTmpl \ "paramDefaultValues").flatMap {
                        paramDefaultValues ⇒
                            (paramDefaultValues \ "paramRefWValue").map {
                                paramRefWValue ⇒
                                    ParamRefWValue(
                                        categoryAttr = paramRefWValue \@ "category",
                                        paramDecriptionAttr = (paramRefWValue \ "@paramDecription").map(_.text).headOption,
                                        paramNameAttr = paramRefWValue \@ "paramName",
                                        valueAttr = paramRefWValue \@ "value"
                                    )
                            }
                    }
                    if (res.nonEmpty) Some(ParamDefaultValues(res: _*)) else None
                }

                structure = Structure((fixedTmpl \ "structure").flatMap {
                    structure ⇒
                        (structure \ "atp").map {
                            atp ⇒
                                Atp(
                                    paramRef = (atp \ "paramRef").map {
                                        paramRef ⇒
                                            ParamRef(
                                                categoryAttr = paramRef \@ "category",
                                                paramDescriptionAttr = (paramRef \ "@paramDescription").map(_.text).headOption,
                                                paramNameAttr = paramRef \@ "paramName"
                                            )
                                    }.headOption,
                                    tp = Tp(
                                        directionOfUseAttr = atp \ "tp" \@ "directionOfUse",
                                        lexemAttr = (atp \ "tp" \ "@lexem").map(_.text).headOption,
                                        structureTypeAttr = atp \ "tp" \@ "structureType",
                                        typeAttr = atp \ "tp" \@ "type"
                                    )
                                )
                        }
                }: _*)

                examples = {
                    val res: Seq[SmsMessage] = (fixedTmpl \ "examples").flatMap(examples ⇒ (examples \ "record").map(SmsMessage.fromXML))
                    if (res.nonEmpty) Some(Examples(res: _*)) else None
                }

                history = History(
                    created = (fixedTmpl \ "history" \ "created").map {
                        created ⇒
                            Created(
                                ed = Ed(
                                    atAttr = (created \ "ed" \@ "at").toLocalDateTime(),
                                    userAttr = created \ "ed" \@ "user"
                                )
                            )
                    }.head,
                    edited = (fixedTmpl \ "history" \ "edited").map {
                        edited ⇒
                            Edited(
                                (edited \ "ed").map {
                                    ed ⇒
                                        Ed(
                                            atAttr = (ed \@ "at").toLocalDateTime(),
                                            userAttr = ed \@ "user"
                                        )
                                }: _*
                            )
                    }.headOption
                )

            } yield FixedTmpl(
                userComments = userComments,
                paramDefaultValues = paramDefaultValues,
                structure = structure,
                examples = examples,
                history = history,
                asStringAttr = asStringAttr,
                isIgnoreDataAttr = isIgnoreDataAttr,
                isValidatedAttr = isValidatedAttr
            )
        } match {
            case Success(res) ⇒
                res.toStream
            case Failure(e) ⇒
                reader.close()
                throw e
        }
    }

    //@formatter:off
    def getXMLFromSeqTemplates(seq: Seq[template]): Map[String, Seq[Elem]] = {
        seq.groupBy(_.customer) map {
            case (customer: String, seqTemplate: Seq[template]) ⇒
                val fixTemps: Seq[Elem] = seqTemplate.map {
                    template ⇒
                        val fixTemp: FixedTmpl = template
                        <fixedTmpl userComments={fixTemp.userComments.map(Text(_))} asString={fixTemp.asStringAttr} isValidatedAttr={fixTemp.isValidatedAttr.toString} isIgnoreDataAttr={fixTemp.isIgnoreDataAttr.toString}>
                            {fixTemp.paramDefaultValues.map {
                            paramDefaultValues ⇒
                                <paramDefaultValues>
                                    {paramDefaultValues.paramRefWValue.map(paramRefWValue ⇒ <paramRefWValue category={paramRefWValue.categoryAttr} paramDecription={paramRefWValue.paramDecriptionAttr.map(Text(_))} paramName={paramRefWValue.paramNameAttr} value={paramRefWValue.valueAttr}/>)}
                                </paramDefaultValues>
                                                            }.getOrElse(NodeSeq.Empty)
                            }
                            <structure>
                            {fixTemp.structure.atps.map {
                                atp ⇒
                                    <atp>{atp.paramRef.map(paramRef ⇒ <paramRef category={paramRef.categoryAttr} paramDescription={paramRef.paramDescriptionAttr.map(Text(_))} paramName={paramRef.paramNameAttr}/>).getOrElse(NodeSeq.Empty)}<tp directionOfUse={atp.tp.directionOfUseAttr} structureType={atp.tp.structureTypeAttr} type={atp.tp.typeAttr} lexem={atp.tp.lexemAttr.map(Text(_))}/>
                                    </atp>
                            }}
                            </structure>
                            {fixTemp.examples.map {examples ⇒ <examples>{examples.records.map(record ⇒ <record>{record.toXML}</record>)}</examples>}.getOrElse(NodeSeq.Empty)}
                        <history>
                           <creaded>
                               <ed at={fixTemp.history.created.ed.atAttr.asString()} user={fixTemp.history.created.ed.userAttr} />
                           </creaded>
                            {fixTemp.history.edited.map{ edited ⇒ <edited>{edited.eds.map{ed ⇒ <ed at={ed.atAttr.asString()} user={ed.userAttr} />}}</edited> }.getOrElse(NodeSeq.Empty)}
                        </history>
                        </fixedTmpl>

                }
                customer → fixTemps
        }
    }
    //@formatter:on
}
