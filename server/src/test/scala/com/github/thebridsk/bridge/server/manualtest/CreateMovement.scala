package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import scala.io.Source
import java.io.Closeable
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.HandInTable
import scala.annotation.tailrec
import com.github.thebridsk.bridge.server.backend.MovementCacheStoreSupport
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStoreConverters
import com.github.thebridsk.utilities.file.FileIO

class CreateMovement

object CreateMovement extends Main {

  import com.github.thebridsk.utilities.main.Converters._

  val cmdName = s"scala ${classOf[CreateMovement].getName}"

  banner(s"""
HTTP server for scoring duplicate and chicago bridge

Syntax:
  ${cmdName} options input [output]
  ${cmdName} --help
Options:""")

  val paramInput = trailArg[Path](name="input", descr="Input file", default=None, required=true)
  val paramOutput = trailArg[Path](name="output", descr="Output directory, default is current directory", default=Some(Path(".")), required=false)

  footer(s"""
The input file has the following syntax:

  tables <nt> boards <nb> <name> <comment>
  it ir ins iew iboards
  ...

or

  tables <nt> boards <nb> <name> <comment>
  table <it>
  ir
  ins
  iew
  iboards
  ...

iboards := 1-2,3,4

Comment lines start with '#'
blank lines are used as end of stanzas.

""")

  def execute(): Int = {

//    test()
//    return 0

    implicit val reader = new PushbackReader( Source.fromFile(paramInput().jfile, "UTF-8") )
    try {
      while (true) {
        processOneTables match {
          case Right(m) =>
            write(m)
          case Left(err) =>
            println( s"Error ${err}" )
            return 0
        }
      }
    } finally {
      reader.close()
    }

    0
  }

  def test(): Unit = {

    test( "3-6", 3::4::5::6::Nil )
    test( "3-6,8-10", 3::4::5::6::8::9::10::Nil )
    test("1,2,3", 1::2::3::Nil )


  }

  def test( s: String, l: List[Int] ): Unit = {
    getBoards(s) match {
      case Right(x) =>
        if (x != l) println( s"Failed on ${s}, expecting ${l}: ${x}" )
        else println( s"Ok on ${s}: ${x}" )
      case Left(err) =>
        println( s"Failed on ${s}: ${err}" )
    }
  }

  val movementCacheStoreSupport = {
    val converters = new BridgeServiceFileStoreConverters(true)
    import converters._
    new MovementCacheStoreSupport(false,false)
  }
  def write( m: Movement ) = {
    val rounds = m.hands
    val r = rounds.sortWith { (l,r) =>
      if (l.table != r.table) l.table < r.table
      else l.round < r.round
    }
    val mov = m.copy( hands = r)
    val yaml = movementCacheStoreSupport.toJSON(mov)
    val fname = "Movement."+m.name+movementCacheStoreSupport.getWriteExtension
    val filename = paramOutput() / fname
    FileIO.writeFileSafe(filename.toString(), yaml)
    println( s"Movement ${m.name} to ${filename}" )
  }

  val patternTablesLine = """tables +(\d+) +boards +(\d+) +([^ ]+)(?: +(.*))? *""".r
  /**
   * @param reader
   * @return An Either.  If Left, then an RC object, error was encountered
   *                     If Right, then a Movement object
   */
  def processOneTables( implicit reader: PushbackReader ): Either[RC,Movement] = {
    if (reader.hasNext()) {
      val line = reader.next()
      line match {
        case patternTablesLine(ntables,nboards,name,comment) =>
          val na = f"Howell${ntables.toInt*2}%02dT${ntables}B${nboards}"
          val c = if (comment == null) s"Howell ${ntables.toInt*2} teams ${ntables} tables ${nboards} boards" else comment
          val d = if (comment == null) s"Howell movements, ${ntables.toInt*2} teams, ${ntables} tables, ${nboards} boards" else comment
          val m = Movement( na, c, d, ntables.toInt*2, List() )
          processTableLong(m) match {
            case Right(newm) =>
              Right( checkForRelay( newm ) )
            case Left(err: NotTableLong) =>
              processTableShort(m) match {
                case Right(newm) =>
                  Right( checkForRelay( newm ) )
                case Left(err) =>
                  Left(err)
              }
            case Left(err) =>
              Left(err)
          }
        case x =>
          reader.pushback(x)
          Left(NotTables(line))
      }

    } else {
      Left(EOF)
    }
  }

  def checkForRelay( m: Movement ) = {
    val r = m.hands.sortWith { (l,r) =>
      if (l.table != r.table) l.table < r.table
      else l.round < r.round
    }

    val rounds = r.groupBy( r => r.round ).map { entry =>
      val (round, rounds) = entry
      val rr = rounds.sortWith( (l,r) => l.table < r.table )
      rr
    }.toList

    val relay = rounds.find { tables =>
      val boards = tables.flatMap( t => t.boards )
      val distinct = boards.distinct
      boards.size != distinct.size
    }

    val short = relay.map( r => s"${m.short} relay" ).getOrElse(m.short)
    val name = relay.map( r => s"${m.name}Relay" ).getOrElse(m.name)
    val desc = relay.map( r => s"${m.description}, relay" ).getOrElse(m.description)

    m.copy( name=name, hands = r, short=short, description=desc)
  }

//  it ir ins iew iboards
  val patternTableShort = """ *(\d+) *(\d+) *(\d+) *(\d+) *(.*)""".r
  /**
   * @param reader
   * @return An Either.  If Left, then an RC object, error was encountered
   *                     If Right, then a Movement object
   */
  def processTableShort( m: Movement )( implicit reader: PushbackReader ): Either[RC,Movement] = {

    @tailrec
    def procShort( rm: Movement, counter: Int ): Either[RC,Movement] = {
      if (reader.hasNext()) {
        val line = reader.next()
        line match {
          case patternTableShort(sitable,siround,sins,siew,siboards) =>
            getBoards(siboards) match {
              case Right(iboards) =>
                val newm = rm.copy(hands = HandInTable( sitable.toInt, siround.toInt, sins.toInt, siew.toInt, iboards )::rm.hands)
                procShort(newm,counter+1)
              case Left(err) =>
                Left(err)
            }
          case _ =>
            reader.pushback(line)
            if (counter==0) Left(NotTableShort(line))
            else Right(rm)
        }
      } else {
        if (counter==0) Left(NotTableShort("eof"))
        else Right(rm)
      }
    }

    procShort(m,0)
  }

  val patternTableLine = """table +(\d+) *""".r
  val patternInt = """ *(\d+) *""".r

  def getInt( line: String ): Either[RC,Int] = {
    line match {
      case patternInt(si) =>
        Right( si.toInt )
      case _ =>
        Left(NotInt(line))
    }
  }

  def get3Ints( implicit reader: PushbackReader ): Either[RC,(Int,Int,Int)] = {
    if (reader.hasNext()) {
      val line1 = reader.next()
      getInt(line1) match {
        case Right(i1) =>
          if (reader.hasNext()) {
            val line2 = reader.next()
            getInt(line2) match {
              case Right(i2) =>
                if (reader.hasNext()) {
                  val line3 = reader.next()
                  getInt(line3) match {
                    case Right(i3) =>
                      Right((i1,i2,i3))
                    case Left(err) =>
                      reader.pushback(line3)
                      reader.pushback(line2)
                      reader.pushback(line1)
                      Left(err)
                  }
                } else {
                  reader.pushback(line2)
                  reader.pushback(line1)
                  Left(EOF)
                }
              case Left(err) =>
                reader.pushback(line2)
                reader.pushback(line1)
                Left(err)
            }
          } else {
            reader.pushback(line1)
            Left(EOF)
          }
        case Left(err) =>
          reader.pushback(line1)
          Left(err)
      }
    } else {
      Left(EOF)
    }
  }

  val patternBoards = """ *(\d+)(?: *- *(\d+))?""".r
  def getBoards( value: String ): Either[RC,List[Int]] = {
    val xx = patternBoards.findAllMatchIn(value).map { m =>
      m.subgroups
    }.flatMap { list =>
      if (list.size == 2) {
        val si = list.head
        val ei = list.tail.head
        if (ei == null) si.toInt::Nil
        else (si.toInt to ei.toInt).toList
      } else {
        return Left(NotBoards(s"${list}"))
      }
    }.toList
    Right(xx)
  }

  def getBoardsLine( implicit reader: PushbackReader ): Either[RC,List[Int]] = {
    if (reader.hasNext()) {
      val line = reader.next()
      getBoards(line)
    } else {
      Left(EOF)
    }
  }

  /**
   * @param reader
   * @return An Either.  If Left, then an RC object, error was encountered
   *                     If Right, then a Movement object
   */
  def processTableLong( m: Movement )( implicit reader: PushbackReader ): Either[RC,Movement] = {

    @tailrec
    def procRound( rm: Movement, itable: Int, counter: Int ): Either[RC,Movement] = {
      processTableLongRound(rm,itable) match {
        case Right(newm) =>
          procRound(newm,itable,counter+1)
        case Left(err) =>
          if (counter == 0) Left(err)
          else if (err.isInstanceOf[NotInt] || err == EOF ) Right(rm)
          else Left(err)
      }
    }

    @tailrec
    def procTable( rm: Movement, counter: Int ): Either[RC,Movement] = {
      if (reader.hasNext()) {
        reader.next() match {
          case patternTableLine(sitable) =>
            val itable = sitable.toInt

            procRound(rm, itable, 0) match {
              case Right(newm) =>
                procTable(newm, counter+1)
              case Left(err) =>
                Left(err)
            }

          case x =>
            reader.pushback(x)
            if (counter == 0) Left(NotTableLong(x))
            else Right(rm)
        }

      } else {
        if (counter == 0) Left(NotTableLong("eof"))
        else Right(rm)
      }
    }

    procTable(m,0)

  }

  /**
   * @param reader
   * @return An Either.  If Left, then an RC object, error was encountered
   *                     If Right, then a Movement object
   */
  def processTableLongRound( m: Movement, itable: Int )( implicit reader: PushbackReader ): Either[RC,Movement] = {
    get3Ints match {
      case Right((iround,ins,iew)) =>
        getBoardsLine match {
          case Right(iboards) =>
            val round = HandInTable( itable, iround, ins, iew, iboards )
            Right(m.copy( hands=round::m.hands))
          case Left(err) =>
            Left(err)
        }
      case Left(err) =>
        Left(err)
    }
  }

}

sealed trait RC
case object EOF extends RC
case class NotTables( msg: String ) extends RC
case class NotTableLong( msg: String ) extends RC
case class NotTableShort( msg: String ) extends RC
case class NotInt( msg: String ) extends RC
case class NotBoards( msg: String ) extends RC

class PushbackReader( src: Source ) extends Closeable {

  private var pushedback = List[String]()

  private val iterator = src.getLines()

  def close() = {
    src.close()
    pushedback = List()
  }

  def pushback( line: String ) = synchronized {
    pushedback = line::pushedback
  }

  def next(): String = synchronized {
    if (pushedback.isEmpty) {
      while (true) {
        val line = iterator.next().trim
        if (line.length()!=0 && line.charAt(0) != '#') {
          return line
        }
      }
      throw new NoSuchElementException
    }
    else {
      val l = pushedback.head
      pushedback = pushedback.tail
      l
    }
  }

  def hasNext(): Boolean = synchronized {
    pushedback.headOption.map( l => true ).getOrElse {
      while (iterator.hasNext) {
        val line = iterator.next().trim
        if (line.length()!=0 && line.charAt(0) != '#') {
          pushback(line)
          return true
        }
      }
      return false
    }
  }
}
