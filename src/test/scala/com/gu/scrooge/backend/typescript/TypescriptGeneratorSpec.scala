package com.gu.scrooge.backend.typescript

import java.io.File
import java.net.URL
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import sys.process._
import com.twitter.scrooge.Compiler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TypescriptGeneratorSpec extends AnyFlatSpec with Matchers {

  def findFile(name: String): URL = this.getClass.getClassLoader.getResource(name)

  def copy(from: String, to: String): Unit = {
    Files.copy(Paths.get(findFile(from).toURI), Paths.get(to), StandardCopyOption.REPLACE_EXISTING)
  }

  "A Typescript generator" should "generate typescript" in {
    val compiler = new Compiler()
    compiler.destFolder = "target/test/"
    compiler.thriftFiles += findFile("school/university.thrift").getFile
    compiler.thriftFiles += findFile("school/shared.thrift").getFile
    compiler.language = "typescript"
    compiler.run()
  }

  it should "compile" in {
    copy("school/package.json", "target/test/@guardian/school/package.json")
    copy("school/tsconfig.json", "target/test/@guardian/school/tsconfig.json")

    val npmInstall = Process("npm install", new File("target/test/@guardian/school/")).!
    npmInstall shouldEqual 0

    val tsc = Process("tsc", new File("target/test/@guardian/school/")).!
    tsc shouldEqual 0
  }

}
