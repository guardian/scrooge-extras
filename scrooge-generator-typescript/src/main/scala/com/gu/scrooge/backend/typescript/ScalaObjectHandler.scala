package com.gu.scrooge.backend.typescript

import java.io.Writer
import java.lang.reflect.{Field, Method}
import java.util.List

import com.github.mustachejava.Iteration
import com.github.mustachejava.reflect.ReflectionObjectHandler

import scala.reflect.ClassTag
import scala.runtime.BoxedUnit
import scala.collection.JavaConverters._

/**
 * This class is copy of com.twitter.scrooge.mustache.ScalaObjectHandler as for some reason it's made private
 */
class ScalaObjectHandler extends ReflectionObjectHandler {

  // Allow any method or field
  override def checkMethod(member: Method): Unit = {}

  override def checkField(member: Field): Unit = {}

  override def coerce(value: AnyRef): Object = {
    value match {
      case m: scala.collection.Map[_, _] => mapAsJavaMap(m)
      case u: BoxedUnit => null
      case Some(some: AnyRef) => coerce(some)
      case None => null
      case _ => value
    }
  }

  override def iterate(
    iteration: Iteration,
    writer: Writer,
    value: AnyRef,
    scopes: List[AnyRef]
  ): Writer = {
    value match {
      case TraversableAnyRef(t) => {
        var newWriter = writer
        t foreach { next => newWriter = iteration.next(newWriter, coerce(next), scopes) }
        newWriter
      }
      case n: Number =>
        if (n.intValue() == 0) writer else iteration.next(writer, coerce(value), scopes)
      case _ => super.iterate(iteration, writer, value, scopes)
    }
  }

  override def falsey(
    iteration: Iteration,
    writer: Writer,
    value: AnyRef,
    scopes: List[AnyRef]
  ): Writer = {
    value match {
      case TraversableAnyRef(t) => {
        if (t.isEmpty) {
          iteration.next(writer, value, scopes)
        } else {
          writer
        }
      }
      case n: Number =>
        if (n.intValue() == 0) iteration.next(writer, coerce(value), scopes) else writer
      case _ => super.falsey(iteration, writer, value, scopes)
    }
  }

  val TraversableAnyRef: Def[Traversable[AnyRef]] = new Def[Traversable[AnyRef]]
  class Def[C: ClassTag] {
    def unapply[X: ClassTag](x: X): Option[C] = {
      x match {
        case c: C => Some(c)
        case _ => None
      }
    }
  }
}