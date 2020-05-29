package com.gu.scrooge.backend.typescript

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import sys.process._
import com.twitter.scrooge.Compiler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TypescriptGeneratorSpec extends AnyFlatSpec with Matchers {

  def findFile(name: String): Path = Paths.get(this.getClass.getClassLoader.getResource(name).toURI)

  def copy(from: Path, to: Path): Unit = Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)

  case class NpmProject(
    resources: Path,
    output: Path,
    packageDirectory: Path
  )

  def forProject(name: String): NpmProject = NpmProject(
    resources = findFile(name),
    output = Paths.get("target","test"),
    packageDirectory = Paths.get("target","test", "@guardian").resolve(name),
  )

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

    val tsc = Process("tsc", npmProject.packageDirectory.toFile).!
    tsc shouldEqual 0
  }

  def generate(npmProject: NpmProject, thriftFiles: Seq[String]): Unit = {
    val compiler = new Compiler()
    compiler.destFolder = npmProject.output.toString
    thriftFiles.foreach(thriftFile => {
      compiler.thriftFiles += npmProject.resources.resolve(thriftFile).toString
    })
    compiler.language = "typescript"
    compiler.run()
  }

  val schoolProject: NpmProject = forProject("school")
  val externalProject: NpmProject = forProject("external")
  val schoolWithExternalProject: NpmProject = forProject("schoolWithExternal")

  "A Typescript generator" should "generate typescript for the school project" in {
    generate(schoolProject, Seq("university.thrift", "shared.thrift"))
  }

  it should "compile the typescript of the school project" in {
    compile(schoolProject)
  }

  it should "generate typescript for the external project" in {
    generate(externalProject, Seq("shared.thrift"))
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

}
