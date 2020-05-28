package com.gu.scrooge.backend.typescript

import java.io.{File, FileWriter}
import com.twitter.scrooge.backend.{Generator, GeneratorFactory, ServiceOption}
import com.twitter.scrooge.frontend.{ParseException, ResolvedDocument, ScroogeInternalException}
import com.twitter.scrooge.ast._
import scala.collection.mutable

class TypescriptGeneratorFactory extends GeneratorFactory {
  val language = "typescript"

  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = {
    new TypescriptGenerator(doc, defaultNamespace, experimentFlags)
  }
}

class TypescriptGenerator(
  resolvedDoc: ResolvedDocument,
  defaultNamespace: String,
  experimentFlags: Seq[String],
) extends Generator(resolvedDoc) {

//  private[this] val keywords = Set(
//    "break", "case", "catch", "class", "const", "continue", "debugger",
//    "default", "delete", "do", "else", "enum", "export", "extends", "false",
//    "finally", "for", "function", "if", "import", "in", "instanceof", "new",
//    "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof",
//    "var", "void", "while", "with")
//
//  def protect(identifier: String): String = if (keywords.contains(identifier.toLowerCase)) {
//    s"_$identifier"
//  } else {
//    identifier
//  }


  override val namespaceLanguage = "typescript"
  private val document: Document = resolvedDoc.document
  private val namespace: Identifier = {
    document.namespace("typescript") getOrElse {
      throw new ParseException(
        s"You must specify a typescript namespace in your thrift definition, for instance: #@namespace $namespaceLanguage guardian/packageName"
      )
    }
  }

  def filename(sid: SimpleID): String = sid.toCamelCase.name
  def typeName(sid: SimpleID): String = sid.toTitleCase.name
  def companionName(sid: SimpleID): String = s"${sid.toTitleCase.name}Serde"

  def thriftType(t: FunctionType): String = t match {
    case Void => "Thrift.Type.VOID"
    case OnewayVoid => "Thrift.Type.VOID"
    case TBool => "Thrift.Type.BOOL"
    case TByte => "Thrift.Type.BYTE"
    case TI16 => "Thrift.Type.I16"
    case TI32 => "Thrift.Type.I32"
    case TI64 => "Thrift.Type.I64"
    case TDouble => "Thrift.Type.DOUBLE"
    case TString => "Thrift.Type.STRING"
    case _: StructType => "Thrift.Type.STRUCT"
    case _: ListType => "Thrift.Type.LIST"
    case _: MapType => "Thrift.Type.MAP"
    case _: SetType => "Thrift.Type.SET"
    case _: EnumType => "Thrift.Type.I32"
    case _ => throw new Exception(s"${t.toString} not implemented yet")
  }

  def typescriptType(t: FunctionType): String = t match {
    case Void => "void"
    case OnewayVoid => "void"
    case TBool => "boolean"
    case TByte => "number"
    case TI16 => "number"
    case TI32 => "number"
    case TI64 => "Int64"
    case TDouble => "number"
    case TString => "string"
    case TBinary => "Buffer"
    case n: NamedType => n.sid.toTitleCase.name
    case MapType(k @ (TI16 | TI32 | TString | EnumType(_, _)), v, _) => {
      s"{[key: ${typescriptType(k)}]: ${typescriptType(v)}}"
    }
    case MapType(k, v, _) => throw new ScroogeInternalException(s"Typescript maps can only have a string or a number as the key, found map<$k, $v>")
    case SetType(x, _) => s"${typescriptType(x)}[]"
    case ListType(x, _) => s"${typescriptType(x)}[]"
    case r: ReferenceType =>
      throw new ScroogeInternalException("ReferenceType should not appear in backend")
    case _ => throw new ScroogeInternalException("unknown type")
  }

  def constValue(rhs: RHS): String = rhs match {
    case BoolLiteral(value) => value.toString
    case IntLiteral(value) => value.toString
    case DoubleLiteral(value) => value.toString
    case StringLiteral(value) => s""""$value""""
    case NullLiteral => "null"
    case ListRHS(elems) => elems.map(constValue).mkString("[", ",", "]")
    case SetRHS(elems) => elems.map(constValue).mkString("[", ",", "]")
    case MapRHS(elems) => elems
      .map { case (k, v) => s"${constValue(k)}: ${constValue(v)}"}
      .mkString("{", ",", "}")
    case _ => throw new ScroogeInternalException(s"Unsupported constant type: $rhs")
  }

  def namespacedFolder(destFolder: File, namespace: String, dryRun: Boolean): File = {
    val cleanedNamespace = namespace.replaceAllLiterally("_at_", "@").replace('.', File.separatorChar)
    val file = new File(destFolder, cleanedNamespace)
    if (!dryRun) file.mkdirs()
    file
  }

  def toTsField(
    fieldType: FieldType,
    index: Option[Int] = None,
    name: Option[String] = None,
    requiredness: Option[Requiredness] = None,
    variableName: String,
    depth: Int = 1,
    isLastField: Boolean = false
  ): TsField = {
    def simpleField(readingStatement: String, writingStatement: String): TsSimpleField = TsSimpleField(
      index = index,
      name = name,
      isOptional = requiredness.exists(_.isOptional),
      variableName = variableName,
      depth = depth,
      typescriptType = typescriptType(fieldType),
      thriftType = thriftType(fieldType),
      readingStatement = readingStatement,
      writingStatement = writingStatement,
      isLastField = isLastField
    )

    fieldType match {
      case TBool => simpleField("readBool", "writeBool")
      case TByte => simpleField("readByte", "writeByte")
      case TI16 => simpleField("readI16", "writeI16")
      case TI32 => simpleField("readI32", "writeI32")
      case TI64 => simpleField("readI64", "writeI64")
      case TDouble => simpleField("readDouble", "writeDouble")
      case TBinary => simpleField("readBinary", "writeBinary")
      case TString => simpleField("readString", "writeString")
      case _: EnumType => simpleField("readI32", "writeI32")
      case struct: StructType => TsNamedField(
        index = index,
        depth = depth,
        name = name,
        isOptional = requiredness.exists(_.isOptional),
        variableName = variableName,
        typescriptType(fieldType),
        thriftType = thriftType(fieldType),
        companionName = companionName(struct.sid),
        isLastField = isLastField
      )
      case lt: ListType => TsCollectionField(
        index = index,
        depth = depth,
        name = name,
        isOptional = requiredness.exists(_.isOptional),
        variableName = variableName,
        typescriptType = typescriptType(fieldType),
        thriftType = thriftType(fieldType),
        nestedType = toTsField(lt.eltType, depth = depth + 1, variableName = s"value${depth + 1}"),
        startReadingStatement = "readListBegin",
        endReadingStatement = "readListEnd",
        startWritingStatement = "writeListBegin",
        endWritingStatement = "writeListEnd",
        isLastField = isLastField,
      )
      case st: SetType => TsCollectionField(
        index = index,
        depth = depth,
        name = name,
        isOptional = requiredness.exists(_.isOptional),
        variableName = variableName,
        typescriptType = typescriptType(fieldType),
        thriftType = thriftType(fieldType),
        nestedType = toTsField(st.eltType, depth = depth + 1, variableName = s"value${depth + 1}"),
        startReadingStatement = "readSetBegin",
        endReadingStatement = "readSetEnd",
        startWritingStatement = "writeSetBegin",
        endWritingStatement = "writeSetEnd",
        isLastField = isLastField,
      )
      case mt: MapType => TsMapField(
        index = index,
        depth = depth,
        name = name,
        isOptional = requiredness.exists(_.isOptional),
        variableName = variableName,
        typescriptType = typescriptType(fieldType),
        thriftType = thriftType(fieldType),
        keyType = toTsField(mt.keyType, depth = depth + 1, variableName = s"key${depth + 1}"),
        keyIsNumber = mt.keyType == TI16 || mt.keyType == TI32 || mt.keyType.isInstanceOf[EnumType],
        valueType = toTsField(mt.valueType, depth = depth + 1, variableName = s"value${depth + 1}"),
        isLastField = isLastField,
      )
      case _ => throw new Exception(s"${fieldType.toString} not implemented yet")
    }
  }

  def importsForStruct(structSource: StructLike): Seq[TsImport] = {
    def isExternalDependency(current: String, dependency: Option[String]): Boolean = {
      dependency.isDefined && !dependency.contains(current) && !dependency.exists(_.startsWith(current))
    }
    def importLocation(namedType: NamedType): String = {
      val namespaceOfTheNamedType = for {
        prefix <- namedType.scopePrefix
        importedDocument <- resolvedDoc.resolver.includeMap.get(prefix.fullName)
        namespace <- importedDocument.document.namespace(namespaceLanguage)
      } yield namespace.fullName

      if (isExternalDependency(namespace.fullName, namespaceOfTheNamedType)) {
        val prefix = namespaceOfTheNamedType.get
          .replaceAllLiterally("_at_", "@")
          .replace('.', '/')
        s"$prefix/${namedType.sid.toCamelCase.name}"
      } else if (namespaceOfTheNamedType.exists(_.startsWith(namespace.fullName))) {
        val relativePath = namespaceOfTheNamedType.get.drop(namespace.fullName.length + 1)
        s"./$relativePath/${namedType.sid.toCamelCase.name}"
      } else {
        s"./${namedType.sid.toCamelCase.name}"
      }
    }

    // maps the source file to the type to import
    val importMappings = structSource.fields.map(_.fieldType).foldLeft(Seq.empty[(String, String)]) {
      case (agg, struct: StructType) => agg ++ Seq(
        importLocation(struct) -> companionName(struct.sid),
        importLocation(struct) -> typeName(struct.sid)
      )
      case (agg, enum: EnumType) => agg ++ Seq(importLocation(enum) -> typeName(enum.sid))
      case (agg, _) => agg
    }

    importMappings
      .groupBy(_._1) // group by file
      .toSeq
      .map { case (file, mapping) => TsImport(mapping.map(_._2), file) }
      .sortBy(_.file)
  }

  override def apply(
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false,
    genAdapt: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val packageDir = namespacedFolder(outputPath, namespace.fullName, dryRun)

    def renderFile(templateName: String, templateParams: Product, filename: String) = {
      val fileContent = MustacheUtils.renderTemplate(namespaceLanguage, templateName, templateParams)
      val file = new File(packageDir, s"${filename}.ts")
      if (!dryRun) {
        val writer = new FileWriter(file)
        try {
          writer.write(fileContent)
        } finally {
          writer.close()
        }
      }
      file
    }

    def renderStruct(struct: StructLike, templateName: String): File = {
      val lastField = struct.fields.last
      renderFile(
        templateName = templateName,
        templateParams = TsStruct(
          typeName = typeName(struct.sid),
          companionName = companionName(struct.sid),
          fields = struct.fields.map(field => toTsField(
            fieldType = field.fieldType,
            index = Some(field.index),
            name = Some(field.originalName),
            requiredness = Some(field.requiredness),
            variableName = s"value1",
            isLastField = field == lastField
          )),
          imports = importsForStruct(struct),
          defaults = struct.fields
            .map(f => f.sid -> f.default )
            .collect { case (sid, Some(default)) => TsDefaultValue(sid.toCamelCase.name, constValue(default)) }
        ),
        filename(struct.sid)
      )
    }

    if (document.consts.nonEmpty) {
      generatedFiles += renderFile(
        templateName = "const.mustache",
        templateParams = TsConsts(
          entries = document.consts.map(c => {
            TsConstEntry(
              name = c.sid.toUpperCase.name,
              typescriptType = typescriptType(c.fieldType),
              value = constValue(c.value)
            )
          })
        ),
        filename = s"constants"
      )
    }

    generatedFiles ++= document.enums.map { enum =>
      renderFile(
        templateName = "enum.mustache",
        templateParams = TsEnum(
          typeName = enum.sid.toTitleCase.name,
          entries = enum.values.map(v => TsEnumEntry(v.sid.toUpperCase.name, v.value)),
        ),
        filename(enum.sid)
      )
    }

    generatedFiles ++= document.structs.map {
      case union: Union => renderStruct(union, "union.mustache")
      case struct: StructLike => renderStruct(struct, "struct.mustache")
    }

    generatedFiles
  }
}
