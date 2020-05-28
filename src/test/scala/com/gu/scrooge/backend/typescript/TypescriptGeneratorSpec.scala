package com.gu.scrooge.backend.typescript

import com.twitter.scrooge.Compiler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TypescriptGeneratorSpec extends AnyFlatSpec with Matchers {

  def getFile(name: String): String = this.getClass.getClassLoader.getResource(name).getFile

  "A Typescript generator" should "generate typescript" in {
    val compiler = new Compiler()
    compiler.destFolder = "target/test/"
    compiler.thriftFiles += getFile("test.thrift")
    compiler.thriftFiles += getFile("shared.thrift")
    compiler.language = "typescript"
    compiler.run()
  }

}
