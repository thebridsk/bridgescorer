package com.github.thebridsk.bridge.fullserver.test.selenium
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.browserpages.Session

class DirectorSession extends Session( "Director")
class CompleteSession extends Session( "Complete")
class TableSession( val table: String ) extends Session( s"Table ${table}" ) {
  val number = Id.tableIdToTableNumber(table)
}
