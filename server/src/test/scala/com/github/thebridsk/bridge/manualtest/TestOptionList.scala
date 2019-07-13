package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main

object TestOptionList extends Main {


  val optionA = opt[List[String]]( "a" )
  val optionB = opt[String]( "b" )

  def execute() = {

    println( s"a is ${optionA.toOption}" )
    println( s"b is ${optionB.toOption}" )

    0
  }


}
