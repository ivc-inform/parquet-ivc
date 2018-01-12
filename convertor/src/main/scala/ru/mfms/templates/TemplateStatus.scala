package ru.mfms.templates

import ru.mfms.convertor.scopt.SealedEnumRuntime

sealed trait templateStatus
case object Candidate extends templateStatus
case object Approved extends templateStatus
case object Production extends templateStatus

object templateStatus {
    private val values = SealedEnumRuntime.values[templateStatus]
    private val mappedKeys: Map[String, templateStatus] = values.map(x => (x.toString, x))(collection.breakOut)
    private val mappedObject: Map[templateStatus, String] = values.map(x => (x, x.toString))(collection.breakOut)

    def getObject(objName: String): templateStatus = mappedKeys(objName)
    def getName(obj: templateStatus): String = mappedObject(obj)
}


