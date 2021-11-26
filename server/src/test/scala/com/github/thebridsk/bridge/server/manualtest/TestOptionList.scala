package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import org.rogach.scallop.ScallopOption
import com.github.thebridsk.utilities.main.MainConf

class TestOptionListConf extends MainConf {

  val optionA: ScallopOption[List[String]] = opt[List[String]]("a")
  val optionB: ScallopOption[String] = opt[String]("b")

}

object TestOptionList extends Main[TestOptionListConf] {

  import config._

  def execute(): Int = {

    println(s"a is ${optionA.toOption}")
    println(s"b is ${optionB.toOption}")

    0
  }

}
