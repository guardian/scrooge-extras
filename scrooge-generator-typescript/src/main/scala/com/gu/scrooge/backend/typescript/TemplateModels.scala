package com.gu.scrooge.backend.typescript

case class TsStruct(
  typeName: String,
  companionName: String,
  fields: Seq[TsField],
  imports: Seq[TsImport],
  defaults: Seq[TsDefaultValue],
)

trait TsField {
  def index: Option[Int]
  def depth: Int
  def name: Option[String]
  def isOptional: Boolean
  def variableName: String
  def isCollection: Boolean
  def isMap: Boolean
  def isSimple: Boolean
  def isNamed: Boolean
  def typescriptType: String
  def thriftType: String
  def isLastField: Boolean

  // Using nested templates as using the native mustache import feature result in stack overflows
  lazy val readField: String = MustacheUtils.renderTemplate("typescript", "read_field.mustache", this)
  lazy val writeField: String = MustacheUtils.renderTemplate("typescript", "write_field.mustache", this)
}

case class TsSimpleField(
  index: Option[Int],
  depth: Int,
  name: Option[String],
  isOptional: Boolean,
  variableName: String,
  typescriptType: String,
  thriftType: String,
  readingStatement: String,
  writingStatement: String,
  isLastField: Boolean,
) extends TsField {
  override def isCollection: Boolean = false
  override def isMap: Boolean = false
  override def isSimple: Boolean = true
  override def isNamed: Boolean = false
}

case class TsNamedField(
  index: Option[Int],
  depth: Int,
  name: Option[String],
  isOptional: Boolean,
  variableName: String,
  typescriptType: String,
  thriftType: String,
  companionName: String,
  isLastField: Boolean,
) extends TsField {
  override def isCollection: Boolean = false
  override def isMap: Boolean = false
  override def isSimple: Boolean = false
  override def isNamed: Boolean = true
}

case class TsCollectionField(
  index: Option[Int],
  depth: Int,
  name: Option[String],
  isOptional: Boolean,
  variableName: String,
  typescriptType: String,
  thriftType: String,
  nestedType: TsField,
  startReadingStatement: String,
  endReadingStatement: String,
  startWritingStatement: String,
  endWritingStatement: String,
  isLastField: Boolean,
) extends TsField {
  override def isCollection: Boolean = true
  override def isMap: Boolean = false
  override def isSimple: Boolean = false
  override def isNamed: Boolean = false
}

case class TsMapField(
  index: Option[Int],
  depth: Int,
  name: Option[String],
  isOptional: Boolean,
  variableName: String,
  typescriptType: String,
  thriftType: String,
  keyType: TsField,
  keyIsNumber: Boolean,
  valueType: TsField,
  isLastField: Boolean,
) extends TsField {
  override def isCollection: Boolean = false
  override def isMap: Boolean = true
  override def isSimple: Boolean = false
  override def isNamed: Boolean = false
}


case class TsImport(
  types: Seq[String],
  file: String
)

case class TsDefaultValue(
  attributeName: String,
  value: String
)

case class TsEnum(
  typeName: String,
  entries: Seq[TsEnumEntry],
)

case class TsEnumEntry(
  name: String,
  value: Int,
)

case class TsConsts(
  entries: Seq[TsConstEntry]
)

case class TsConstEntry(
  name: String,
  typescriptType: String,
  value: String
)