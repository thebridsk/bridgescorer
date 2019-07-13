package com.github.thebridsk.bridge.util

import scala.annotation.tailrec

class Version(val version: String) extends Ordered[Version] {

  val components = version.split("\\.").toList

  override def equals(other: Any) = {
    other match {
      case v: Version => v.version == version
      case _          => false
    }
  }

  override def hashCode() = version.hashCode()

  /** Result of comparing `this` with operand `that`.
    *
    * Implement this method to determine how instances of A will be sorted.
    *
    * Returns `x` where:
    *
    *   - `x < 0` when `this < that`
    *
    *   - `x == 0` when `this == that`
    *
    *   - `x > 0` when  `this > that`
    *
    */
  def compare(that: Version): Int = {

    @tailrec
    def comp(values: List[(String, String)]): Int = {
      if (values.isEmpty) 0
      else {
        val (me, other) = values.head
        val rc = me.compare(other)
        if (rc == 0) comp(values.tail)
        else {
          try {
            me.toInt.compare(other.toInt)
          } catch {
            case x: Exception =>
              rc
          }
        }
      }
    }

    val rc = comp(components.zip(that.components))
    if (rc == 0) {
      components.length.compare(that.components.length)
    } else {
      rc
    }
  }

  override def toString(): String = version
}

object Version {

  def apply(version: String) = new Version(version)

//  val version: String = "1.0.2-SNAPSHOT-cd6b6f20d04a785b3b7dd268eeb9b7ca7de1a81c-SNAPSHOT-master"

  val pattern = """([0-9a-zA-Z.]+).*""".r

  def create(version: String) = {
    version match {
      case pattern(v) => new Version(v)
      case _ =>
        throw new Exception(s"Version string is not valid: ${version}")
    }
  }

  def main(args: Array[String]): Unit = {
    val v05 = Version("v0.5")
    val v10 = Version("v1.0")
    val v101 = Version("v1.0.1")
    val v11 = Version("v1.1")

    test(v05, v05)
    test(v05, v10)
    test(v05, v101)
    test(v05, v11)

    test(v10, v05)
    test(v10, v10)
    test(v10, v101)
    test(v10, v11)

    test(v101, v05)
    test(v101, v10)
    test(v101, v101)
    test(v101, v11)

    test(v11, v05)
    test(v11, v10)
    test(v11, v101)
    test(v11, v11)

  }

  def test(v1: Version, v2: Version) = {
    val lt = v1 < v2
    val eq = v1 == v2
    val gt = v1 > v2

    if (lt) println(s"${v1} is less than ${v2}")
    if (eq) println(s"${v1} is equal to ${v2}")
    if (gt) println(s"${v1} is greater than ${v2}")
  }
}
