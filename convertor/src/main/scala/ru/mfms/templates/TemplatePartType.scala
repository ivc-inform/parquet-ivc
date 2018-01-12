package ru.mfms.templates

import com.typesafe.scalalogging.LazyLogging
import ru.mfms.convertor.scopt.SealedEnumRuntime

sealed trait templatePartType
case object Unknown extends templatePartType
case object TemplatePartTypeEnumLexem extends templatePartType
case object TemplatePartTypeEnumCardNumber extends templatePartType
case object TemplatePartTypeEnumMoney extends templatePartType
case object TemplatePartTypeEnumString extends templatePartType
case object TemplatePartTypeEnumLocalDate extends templatePartType
case object TemplatePartTypeEnumLocalDateTime extends templatePartType
case object TemplatePartTypeEnumCurrency extends templatePartType
case object TemplatePartTypeEnumDouble extends templatePartType
case object TemplatePartTypeEnumLong extends templatePartType
case object TemplatePartTypeEnumLocalTime extends templatePartType
case object TemplatePartTypeEnumPhone extends templatePartType
case object TemplatePartTypeEnumDateTime extends templatePartType
case object TemplatePartTypeEnumURL extends templatePartType

object templatePartType extends LazyLogging{
    private val values = SealedEnumRuntime.values[templatePartType]
    private val mappedKeys: Map[String, templatePartType] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[templatePartType, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): templatePartType = mappedKeys get objName match {
        case None ⇒
            logger error s"\n //////////////// Unknown templatePartType : '$objName' //////////////////////////////"
            Unknown
        case Some(x) ⇒ x
    }
    def getName(obj: templatePartType): String = mappedObject(obj)
}

