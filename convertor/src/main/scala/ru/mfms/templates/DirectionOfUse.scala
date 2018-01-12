package ru.mfms.templates

import ru.mfms.convertor.scopt.SealedEnumRuntime

sealed trait directionOfUse
case object MustBe extends directionOfUse
case object TreatAs extends directionOfUse

object directionOfUse {
    private val values = SealedEnumRuntime.values[directionOfUse]
    private val mappedKeys: Map[String, directionOfUse] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[directionOfUse, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): directionOfUse = mappedKeys(objName)
    def getName(obj: directionOfUse): String = mappedObject(obj)
}

