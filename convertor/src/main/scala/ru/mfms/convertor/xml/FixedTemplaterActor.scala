package ru.mfms.convertor.xml

import java.time.LocalDateTime

import ru.mfms.mfmd.integration.common.SmsMessage

case class FixedTemplaterActor(acnAttr: String, fixedTemplateHolders: FixedTemplateHolder*)

case class FixedTemplateHolder(fixedTmpls: FixedTmpl*)

case class FixedTmpl(userComments: Option[String], paramDefaultValues: Option[ParamDefaultValues], structure: Structure, examples: Option[Examples], history: History, asStringAttr: String, isIgnoreDataAttr: Boolean, isValidatedAttr: Boolean)

case class ParamDefaultValues(paramRefWValue: ParamRefWValue*)

case class Structure(atps: Atp*)

case class Examples(records: SmsMessage*)

case class History(created: Created, edited: Option[Edited])

case class ParamRefWValue(categoryAttr: String, paramDecriptionAttr: Option[String], paramNameAttr: String, valueAttr: String)

case class Atp(paramRef: Option[ParamRef], tp: Tp)

case class ParamRef(categoryAttr: String, paramDescriptionAttr: Option[String], paramNameAttr: String)

case class Tp(directionOfUseAttr: String, lexemAttr: Option[String], structureTypeAttr: String, typeAttr: String)

case class Created(ed: Ed)

case class Edited(eds: Ed*)

case class Ed(atAttr: LocalDateTime, userAttr: String)
