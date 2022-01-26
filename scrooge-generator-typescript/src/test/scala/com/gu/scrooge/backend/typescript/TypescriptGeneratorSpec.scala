package com.gu.scrooge.backend.typescript

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import com.gu.thriftTest.school.School
import com.gu.thriftTest.school.common.Denomination.FullName
import com.gu.thriftTest.school.common.Type.{Human, Robot}
import com.gu.thriftTest.school.common.{Denomination, Student}

import sys.process._
import com.twitter.scrooge.{Compiler, ScroogeConfig, ThriftStruct, ThriftUtil}
import org.apache.thrift.protocol.{TCompactProtocol, TProtocol}
import org.apache.thrift.transport.TMemoryBuffer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class TypescriptGeneratorSpec extends AnyFlatSpec with Matchers {

  def findFile(name: String): Path = Paths.get(this.getClass.getClassLoader.getResource(name).toURI)

  def copy(from: Path, to: Path): Unit = {
    to.toFile.mkdirs()
    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
  }

  case class NpmProject(
    packageName: String,
    resources: Path,
    output: Path,
    packageDirectory: Path
  )

  def forProject(packageName: String, resourceFolder: String): NpmProject = {
    val packageElems = packageName.split('/').toList
    val packagePath = Paths.get(packageElems.head, packageElems.tail: _*)
    val output = Paths.get("target","test")

    NpmProject(
      packageName = packageName,
      resources = findFile(resourceFolder),
      output = output,
      packageDirectory = output.resolve(packagePath),
    )
  }

  def compile(npmProject: NpmProject): Unit = {
    copy(
      npmProject.resources.resolve("package.json"),
      npmProject.packageDirectory.resolve("package.json")
    )
    copy(
      npmProject.resources.resolve("tsconfig.json"),
      npmProject.packageDirectory.resolve("tsconfig.json")
    )

    val npmInstall = Process("npm install", npmProject.packageDirectory.toFile).!
    npmInstall shouldEqual 0

    val tsc = Process("./node_modules/.bin/tsc", npmProject.packageDirectory.toFile).!
    tsc shouldEqual 0
  }

  def generate(npmProject: NpmProject, thriftFiles: Seq[String]): Unit = {
    val scroogeConfig = ScroogeConfig(
      destFolder = npmProject.output.toString,
      thriftFiles = thriftFiles.map (thriftFile => {
        npmProject.resources.resolve(thriftFile).toString
      }).toList,
      defaultNamespace = npmProject.packageName,
      language = "typescript"
    )
    val compiler = new Compiler(scroogeConfig)
    compiler.run()
  }

  def serialise(obj: ThriftStruct): Array[Byte] = {
    val buffer = new TMemoryBuffer(16384)
    val protocol = new TCompactProtocol(buffer)
    obj.write(protocol)
    buffer.getArray
  }

  def deserialise[A](inputBuffer: Array[Byte], read: TProtocol => A): A = {
    val buffer = new TMemoryBuffer(16384)
    buffer.write(inputBuffer)
    val protocol = new TCompactProtocol(buffer)
    read(protocol)
  }

  val schoolProject: NpmProject = forProject("@guardian/school", "school")
  val externalProject: NpmProject = forProject("@guardian/external", "external")
  val schoolWithExternalProject: NpmProject = forProject("@guardian/schoolWithExternal", "schoolWithExternal")
  val decodeEncodeProject: NpmProject = forProject("@guardian/decode-encode", "decode-encode")
  val entityProject: NpmProject = forProject("@guardian/content-entity-model", "entity")
  val noInt64Project: NpmProject = forProject("@guardian/no-int64", "no-int64")

  val school: School = {
    val harry: Student = Student(FullName("Harry Potter"), 10, Set(0), Some(Human))
    val hermione: Student = Student(FullName("Hermione Granger"), 10, Set(0), Some(Human))
    School(
      schoolName = Some("Hogwarts School of Witchcraft and Wizardry"),
      students = Seq(harry, hermione),
      crazyNestedList = Seq(Set(Seq(harry), Seq(hermione))),
      classes = Map("Magic" -> harry),
      emptyMap = Some(Map.empty),
      emptyList = Some(List.empty)
    )
  }

  "The existing scala thrift generator" should "serialise and deserialise data correctly" in {
    val buffer = serialise(school)
    val newSchool = deserialise(buffer, School.decode)
    school shouldEqual newSchool
  }

  "A Typescript generator" should "generate typescript for the school project" in {
    generate(schoolProject, Seq("university.thrift", "shared.thrift"))
  }

  it should "compile the typescript of the school project" in {
    compile(schoolProject)
  }

  it should "generate typescript for the entity project" in {
    generate(entityProject, Seq(
      "entity.thrift",
      "shared.thrift",
      "entities/film.thrift",
      "entities/game.thrift",
      "entities/organisation.thrift",
      "entities/person.thrift",
      "entities/place.thrift",
      "entities/restaurant.thrift",
    ))
  }

  it should "compile the typescript of the entity project" in {
    compile(entityProject)
  }

  it should "generate typescript for the external project" in {
    generate(externalProject, Seq("shared.thrift"))
  }

  it should "encode and decode thrift generated by the scala generator" in {
    val inputBuffer = serialise(school)
    decodeEncodeProject.packageDirectory.toFile.mkdirs()
    Files.write(decodeEncodeProject.packageDirectory.resolve("input.blob"), inputBuffer)

    copy(decodeEncodeProject.resources.resolve("decode-encode.ts"), decodeEncodeProject.packageDirectory.resolve("decode-encode.ts"))
    compile(decodeEncodeProject)

    val nodeReturnCode = Process("node decode-encode.js input.blob output.blob", decodeEncodeProject.packageDirectory.toFile).!
    nodeReturnCode shouldEqual 0

    val outputBuffer = Files.readAllBytes(decodeEncodeProject.packageDirectory.resolve("output.blob"))
    val newSchool = deserialise(outputBuffer, School.decode)

    school shouldEqual newSchool
  }

  it should "compile the typescript of the external project" in {
    compile(externalProject)
  }

  it should "generate typescript for the school with external dependency project" in {
    generate(schoolWithExternalProject, Seq("university.thrift"))
  }

  it should "compile the typescript of the school with external dependency project" in {
    compile(schoolWithExternalProject)
  }

  it should "generate and compile typescript without the int64 import if there's no int64 used in the file" in {
    generate(noInt64Project, Seq("noint64.thrift"))
    compile(noInt64Project)
    val generatedFile = noInt64Project.packageDirectory.resolve("noInt64.ts").toFile

    generatedFile.exists shouldBe true
    val source = Source.fromFile(generatedFile)
    source.mkString.contains("import Int64 from 'node-int64';") shouldBe false
  }

}
