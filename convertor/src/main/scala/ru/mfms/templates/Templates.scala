package ru.mfms.templates

import java.time.LocalDateTime
import java.util.UUID

import com.simplesys.common.Strings._
import ru.mfms.convertor.xml._
import ru.mfms.mfmd.integration.common.SmsMessage
import ru.mfms.mfmd.integration.connector.{OutMessage, Protocol}

object Templates {
    implicit def fixedTempl2template(fixedTempl: FixedTmpl)(implicit customer: String): template = {
        template(
            id = UUID.randomUUID().toString,
            value = fixedTempl.asStringAttr,
            customer = customer,
            status = Approved,
            defaultValues = fixedTempl.paramDefaultValues.map {
                _.paramRefWValue.map {
                    dv ⇒
                        defaultValue(
                            parameter = parameter(
                                paramName = dv.paramNameAttr,
                                paramCategory = dv.categoryAttr,
                                paramDescription = dv.paramDecriptionAttr
                            ),
                            defaultValue = dv.valueAttr
                        )
                }
            }.getOrElse(Seq()),
            structure = fixedTempl.structure.atps.map {
                _atp ⇒
                    atp(
                        parameter = _atp.paramRef.map(
                            paramRef ⇒
                                parameter(
                                    paramName = paramRef.paramNameAttr,
                                    paramDescription = paramRef.paramDescriptionAttr,
                                    paramCategory = paramRef.categoryAttr
                                )
                        ),
                        templatePart = templatePart(
                            lexem = _atp.tp.lexemAttr,
                            directionOfUse = directionOfUse.getObject(_atp.tp.directionOfUseAttr),
                            `type` = templatePartType.getObject(_atp.tp.typeAttr),
                            structureType = structureType.getObject(_atp.tp.structureTypeAttr)
                        )
                    )
            },
            examples = fixedTempl.examples.map {
                _.records.map {
                    record ⇒
                        OutMessage(
                            id = record.id.toString,
                            acnCode = record.acnCode,
                            channel = record.acnCode,
                            protocol = Protocol.HPX,
                            subject = record.subject,
                            address = record.address,
                            timestamp = record.timestamp,
                            startTime = record.startTime,
                            text = record.text
                        )
                }
            }.getOrElse(Seq()),
            history = Seq(historyElement(
                name = fixedTempl.history.created.ed.userAttr,
                date = fixedTempl.history.created.ed.atAttr.toString,
                `type` = created
            )) ++ fixedTempl.history.edited.map {
                _.eds.map {
                    ed ⇒
                        historyElement(
                            name = ed.userAttr,
                            date = ed.atAttr.toString,
                            `type` = modified
                        )
                }
            }.getOrElse(Seq())

        )
    }

    implicit def template2fixedTempl(template: template): FixedTmpl = {

        val paramDefaultValues =
            if (template.defaultValues.nonEmpty) Some(ParamDefaultValues(
                template.defaultValues.map {
                    item ⇒
                        ParamRefWValue(
                            categoryAttr = item.parameter.paramCategory,
                            paramDecriptionAttr = item.parameter.paramDescription,
                            paramNameAttr = item.parameter.paramName,
                            valueAttr = item.defaultValue
                        )
                }: _*
            )) else None

        val structure =
            Structure(
                template.structure.map {
                    item ⇒
                        val paramRef: Option[ParamRef] = item.parameter.map {
                            parameter ⇒
                                ParamRef(
                                    categoryAttr = parameter.paramCategory,
                                    paramDescriptionAttr = parameter.paramDescription,
                                    paramNameAttr = parameter.paramName
                                )
                        }

                        val tp: Tp = Tp(
                            directionOfUseAttr = directionOfUse.getName(item.templatePart.directionOfUse),
                            lexemAttr = item.templatePart.lexem,
                            structureTypeAttr = structureType.getName(item.templatePart.structureType),
                            typeAttr = templatePartType.getName(item.templatePart.`type`)
                        )

                        Atp(
                            paramRef = paramRef,
                            tp = tp
                        )
                }: _*
            )


        val examples = if (template.examples.nonEmpty) Some(Examples(
            template.examples.map {
                example ⇒
                    SmsMessage(
                        id = example.id,
                        timestamp = example.timestamp,
                        gtimestamp = None,
                        startTime = example.startTime,
                        acnCode = example.acnCode,
                        cnrCode = None,
                        subject = example.subject,
                        address = example.originalAddress,
                        text = example.text,
                        operatorUnitRegionCode = None,
                        dlvStatus = "",
                        dlvStatusAt = LocalDateTime.now(),
                        dlvError = None,
                        sentAt = None
                    )
            }.sortWith(_.id < _.id): _*)) else None

        val history: History = {
            val head = template.history.filter(_.`type` == ru.mfms.templates.created).head
            val tail = template.history.filter(_.`type` == ru.mfms.templates.modified).sortWith(_.date.toLocalDateTime().getMillis < _.date.toLocalDateTime().getMillis)

            val created = Created(
                ed = Ed(
                    atAttr = head.date.toLocalDateTime(),
                    userAttr = head.name
                ))

            val edited = if (tail.nonEmpty) Some(Edited(tail.map {
                hist ⇒
                    Ed(
                        atAttr = hist.date.toLocalDateTime(),
                        userAttr = hist.name
                    )
            }: _*)) else None

            History(
                created = created,
                edited = edited
            )
        }

        FixedTmpl(
            userComments = None,
            paramDefaultValues = paramDefaultValues,
            structure = structure,
            examples = examples,
            history = history,
            asStringAttr = template.value,
            isIgnoreDataAttr = false,
            isValidatedAttr = true
        )
    }
}
