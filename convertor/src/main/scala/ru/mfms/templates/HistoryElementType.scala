package ru.mfms.templates

import ru.mfms.convertor.scopt.SealedEnumRuntime

sealed trait historyElementType

case object created extends historyElementType
case object modified extends historyElementType

object historyElementType {
    private val values = SealedEnumRuntime.values[historyElementType]
    private val mappedKeys: Map[String, historyElementType] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[historyElementType, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): historyElementType = mappedKeys(objName)
    def getName(obj: historyElementType): String = mappedObject(obj)
}

