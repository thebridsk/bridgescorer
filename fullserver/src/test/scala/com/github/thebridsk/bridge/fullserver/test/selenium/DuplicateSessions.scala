package com.github.thebridsk.bridge.fullserver.test.selenium

import com.github.thebridsk.browserpages.Session
import com.github.thebridsk.bridge.data.Table

class DirectorSession extends Session( "Director")
class CompleteSession extends Session( "Complete")

class TableSession( val table: Table.Id ) extends Session( s"Table ${table}" ) {
  def this( t: String ) = this(Table.id(t))
  val number = table.toNumber
}
