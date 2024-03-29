package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs

object TestMixing extends MainNoArgs {

  case class Entry(a: Int, b: Int, mix: Double) {
    override def toString(): String = {
      f"""$a%2d, $b%2d, $mix%.4f"""
    }
  }

  def execute(): Int = {

    val mina = 2
    val maxa = 10
    val minb = 2
    val maxb = 10

    def normalizeA(a: Int) = 1 - (a - mina + 0.0) / (maxa - mina)
    def normalizeB(b: Int) = (b - minb + 0.0) / (maxb - minb)

    def mix(a: Int, b: Int) = {
      val weightA = 0.51

      normalizeA(a) * weightA + normalizeB(b) * (1 - weightA)
    }

    val all = (for (
      a <- mina to maxa;
      b <- minb to maxb
    ) yield {
      Entry(a, b, mix(a, b))
    }).toList

    val sorted = all.sortWith((l, r) => l.mix < r.mix)

    println("Sorted")
    println("a,b,mix")
    println(sorted.mkString("\n  ", "\n  ", ""))

    0
  }
}
