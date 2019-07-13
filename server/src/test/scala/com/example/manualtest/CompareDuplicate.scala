package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path

object CompareDuplicate extends Main {

  import com.github.thebridsk.utilities.main.Converters._

  val optionOne = trailArg[Path](name="one", descr="MatchDuplicate one", default=None, required=true)
  val optionTwo = trailArg[Path](name="two", descr="MatchDuplicate two", default=None, required=true)

  def execute() = {

    val md1 = optionOne.toOption.map(f => CheckTimes.readFromFile(f.toString())).map(j => CheckTimes.convertToMatch(j)).get
    val md2 = optionTwo.toOption.map(f => CheckTimes.readFromFile(f.toString())).map(j => CheckTimes.convertToMatch(j)).get

    val md2copy = md2.copy(id=md1.id)

    try {
      md1.equalsIgnoreModifyTime(md2copy,true)
      println("OK")
    } catch {
      case x: Exception =>
        logger.severe(s"""The two MatchDuplicates don't compare.\n  1 - ${optionOne.toOption.get}\n  2 - ${optionTwo.toOption.get}""", x)
    }

    0
  }

}
