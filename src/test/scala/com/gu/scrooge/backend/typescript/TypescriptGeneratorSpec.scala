package com.gu.scrooge.backend.typescript

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import sys.process._
import com.twitter.scrooge.Compiler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TypescriptGeneratorSpec extends AnyFlatSpec with Matchers {

  def findFile(name: String): Path = Paths.get(this.getClass.getClassLoader.getResource(name).toURI)

  def copy(from: Path, to: Path): Unit = Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)

  val schoolResources: Path = findFile("school")
  val output: Path = Paths.get("target","test")
  val schoolPackage: Path = output.resolve("@guardian").resolve("school")

  "A Typescript generator" should "generate typescript" in {
    val compiler = new Compiler()
    compiler.destFolder = "target/test/"
    compiler.thriftFiles += schoolResources.resolve("university.thrift").toString
    compiler.thriftFiles += schoolResources.resolve("shared.thrift").toString
    compiler.language = "typescript"
    compiler.run()
  }

  it should "compile" in {
    copy(schoolResources.resolve("package.json"), schoolPackage.resolve("package.json"))
    copy(schoolResources.resolve("tsconfig.json"), schoolPackage.resolve("tsconfig.json"))

    val npmInstall = Process("npm install", schoolPackage.toFile).!
    npmInstall shouldEqual 0

    val tsc = Process("tsc", schoolPackage.toFile).!
    tsc shouldEqual 0
  }

}
