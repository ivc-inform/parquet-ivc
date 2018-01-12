package ru.mfms.templates

import ru.mfms.convertor.scopt.SealedEnumRuntime

sealed trait structureType
case object SimplePart extends structureType
case object VariableSeqPart extends structureType

object structureType {
    private val values = SealedEnumRuntime.values[structureType]
    private val mappedKeys: Map[String, structureType] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[structureType, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): structureType = mappedKeys(objName)
    def getName(obj: structureType): String = mappedObject(obj)
}
