package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import org.rogach.scallop.ScallopOption

object TestOptionList extends Main {


  val optionA: ScallopOption[List[String]] = opt[List[String]]( "a" )
  val optionB: ScallopOption[String] = opt[String]( "b" )

  def execute(): Int = {

    println( s"a is ${optionA.toOption}" )
    println( s"b is ${optionB.toOption}" )

    0
  }


}
