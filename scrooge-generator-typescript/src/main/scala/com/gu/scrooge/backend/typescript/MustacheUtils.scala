package com.gu.scrooge.backend.typescript

import java.io.StringWriter

import com.github.mustachejava.{DefaultMustacheFactory, Mustache}

import scala.collection.concurrent.TrieMap

object MustacheUtils {

  private val templateCache = new TrieMap[String, Mustache]

  private def compileTemplate(namespace: String, template: String): Mustache = {
    val mustacheFactory = new DefaultMustacheFactory(s"$namespace/")
    mustacheFactory.setObjectHandler(new ScalaObjectHandler)
    mustacheFactory.compile(template)
  }

  def renderTemplate(namespace: String, template: String, data: Any): String = {
    val sw = new StringWriter()
    val mustache = templateCache.getOrElseUpdate(template, compileTemplate(namespace, template))
    mustache.execute(sw, data).flush()
    sw.toString
  }
}
