package com.github.thebridsk.bridge.datastore

import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import scala.reflect.io.Path
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestionsCalculation
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import java.text.SimpleDateFormat
import java.util.Date
import com.github.thebridsk.bridge.data.duplicate.suggestion.NeverPair
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.datastore.stats.DuplicateStatsCommand

trait ShowCommand

object ShowCommand extends Subcommand("show") {

  val log = Logger[ShowCommand]

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("show information about a datastore")

  banner(s"""
Show information about a datastore

Syntax:
  ${DataStoreCommands.cmdName} show options
Options:""")

  addSubcommand(ShowNamesCommand)
  addSubcommand(ShowBoardsetsAndMovementsCommand)
  addSubcommand(ShowSuggestionCommand)
  addSubcommand(ShowPartnersOfCommand)
  addSubcommand(ShowTeamDeclarerCommand)
  addSubcommand(DuplicateStatsCommand)

  shortSubcommandsHelp(true)

//  footer(s""" """)

  def executeSubcommand(): Int = {
    log.severe("Unknown subcommand specified")
    1
  }
}

object ShowNamesCommand extends Subcommand("names") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("show all names in datastore")

  banner(s"""
Show names in a datastore

Syntax:
  ${DataStoreCommands.cmdName} show names
Options:""")

//  footer(s""" """)

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  def executeSubcommand(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore(storedir)
    log.info((await(datastore.getAllNames()) match {
      case Right(list) => list
      case Left(error) => List()
    }).mkString("\n"))
    0
  }
}

object ShowSuggestionCommand extends Subcommand("suggestion") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("show all names in datastore")

  banner(s"""
Show names in a datastore

Syntax:
  ${DataStoreCommands.cmdName} show suggestion args
Options:""")

  val optionNeverPair = opt[List[String]](
    "neverpair",
    short = 'n',
    descr = "never pair players.  Value is player1,player2",
    argName = "pair",
    default = None
  )

  val players = (1 to 8).map { i =>
    trailArg[String](
      s"player${i}",
      descr = s"player ${i}",
      required = true,
      default = None,
      hidden = false
    )
  }.toList

  validate(
    players(0),
    players(1),
    players(2),
    players(3),
    players(4),
    players(5),
    players(6),
    players(7)
  ) { (p1, p2, p3, p4, p5, p6, p7, p8) =>
    val distinct = List(p1, p2, p3, p4, p5, p6, p7, p8).distinct
    val l = distinct.length
    if (l == 8) Right(Unit)
    else Left(s"Did not specify 8 distinct names: ${distinct}")
  }

//  footer(s""" """)

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  val patternPair = """([^,]+),([^,]+)""".r

  def executeSubcommand(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore(storedir)
    await(datastore.getDuplicateSummaries()) match {
      case Right(summary) =>
        val neverPair = optionNeverPair.toOption.map(
          lnp =>
            lnp.flatMap { pair =>
              pair match {
                case patternPair(p1, p2) =>
                  NeverPair(p1, p2) :: Nil
                case _ =>
                  log.severe(
                    s"""Never pair option not valid, ignoring: ${pair}"""
                  )
                  Nil
              }
            }
        )
        val input =
          DuplicateSuggestions(players.map(sc => sc()), neverPair = neverPair)
        val sug = DuplicateSuggestionsCalculation.calculate(input, summary)
        log.info(s"Took ${sug.calcTimeMillis} milliseconds to calculate")
//        sug.suggestions.foreach { list =>
//          list.zipWithIndex.foreach { e =>
//            val (suggestion,i) = e
//            val pairs = suggestion.players.map { p => p.toString() }
//            log.info( s"""${i+1}: ${pairs.mkString(", ")}""")
//          }
//        }
        log.info(
          "The numbers in parentheses are ( lasttime played together, number of times played together)"
        )
        sug.suggestions.foreach { list =>
          list.zipWithIndex.foreach { e =>
            val (sg, i) = e
            val s = sg.players
              .sortWith { (l, r) =>
                if (l.lastPlayed == r.lastPlayed) l.timesPlayed < r.timesPlayed
                else l.lastPlayed < r.lastPlayed
              }
              .map(
                p =>
                  f"${p.player1}%8s-${p.player2}%-8s (${p.lastPlayed}%2d,${p.timesPlayed}%2d)"
              )
              .mkString(", ")
            log.info(f"  ${i + 1}%3d: ${s}%s")
          }
        }
        0
      case Left(error) =>
        log.severe(s"Error getting matches: ${error}")
        1
    }
  }
}

object ShowBoardsetsAndMovementsCommand extends Subcommand("boardsets") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("show all boardsets and movements")

  banner(s"""
Show boardsets and movements

Syntax:
  ${DataStoreCommands.cmdName} show boardsets
Options:""")

//  footer(s""" """)

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  def executeSubcommand(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore(storedir)

    await(datastore.boardSets.readAll()) match {
      case Right(map) =>
        map.values.foreach { bs =>
          val name = bs.name
          val desc = bs.description
          val n = bs.boards.length
          log.info(s"${name} ${n} ${desc}")
        }
      case Left((status, msg)) =>
        log.warning(s"Error getting boardsets: ${msg}")
    }

    await(datastore.movements.readAll()) match {
      case Right(map) =>
        map.values.foreach { m =>
          val name = m.name
          val desc = m.description
          val team = m.numberTeams
          log.info(s"${name} ${team} ${desc}")
        }
      case Left((status, msg)) =>
        log.warning(s"Error getting movements: ${msg}")
    }

    0
  }
}

object ShowPartnersOfCommand extends Subcommand("partnersof") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  descr("show partners of a player")

  banner(s"""
Show partners of a player

Syntax:
  ${DataStoreCommands.cmdName} show partnersof
Options:""")

  val player = trailArg[String](
    "player",
    descr = "player",
    required = true,
    default = None,
    hidden = false
  )

//  footer(s""" """)

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  val sdf = new SimpleDateFormat("MM/dd/YYYY")

  def executeSubcommand(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore(storedir)

    val p = player()

    log.info(s"Partners of ${p}")

    val fut = datastore.getDuplicateSummaries().map { rl =>
      rl match {
        case Right(summaries) =>
          summaries.sortWith((l, r) => l.created > r.created).foreach {
            summary =>
              val date = sdf.format(new Date(summary.created.toLong))
              if (summary.containsPlayer(p)) {
                val partner = summary.teams
                  .find(dse => dse.team.player1 == p || dse.team.player2 == p)
                  .map { dse =>
                    if (dse.team.player1 == p) dse.team.player2
                    else dse.team.player1
                  }
                  .get
                log.info(f"""${summary.id}%4s ${date} ${partner}""")
              } else {
                log.info(f"""${summary.id}%4s ${date} -""")
              }
          }
        case Left((status, msg)) =>
          log.warning(s"Error getting summaries: ${msg}")
      }
    }

    await(fut)

    0
  }
}

object ShowTeamDeclarerCommand extends Subcommand("declarer") {
  import DataStoreCommands.optionStore
  import ShowCommand.log

  import com.github.thebridsk.utilities.main.Converters._

  descr("show the number of times team was declarer in match")

  banner(s"""
show the number of times team was declarer in match

Syntax:
  ${DataStoreCommands.cmdName} show declarer
Options:""")

//  footer(s""" """)

  def await[T](fut: Future[T]) = Await.result(fut, 30.seconds)

  val sdf = new SimpleDateFormat("MM/dd/YYYY")

  def executeSubcommand(): Int = {
    val storedir = optionStore().toDirectory
    log.info(s"Using datastore ${storedir}")
    val datastore = new BridgeServiceFileStore(storedir)

    val fut = datastore.duplicates.readAll().map { rl =>
      rl match {
        case Right(dups) =>
          dups.values.toList.sortWith((l, r) => l.created > r.created).foreach {
            dup =>
              val counts = dup.boards
                .flatMap { b =>
                  b.hands.flatMap { dh =>
                    dh.played.map { h =>
                      h.declarer match {
                        case "N" | "S" =>
                          dh.nsTeam
                        case "E" | "W" =>
                          dh.ewTeam
                      }
                    }
                  }
                }
                .foldLeft(Map[Id.Team, Int]()) { (ac, v) =>
                  val c = ac.get(v).getOrElse(0)
                  ac + (v -> (c + 1))
                }
                .toList
                .sortBy { e =>
                  e._2
                }
                .map { e =>
                  val (k, v) = e
                  f"${k}=${v}%2s"
                }
              val d = sdf.format(new Date(dup.created.toLong))
              log.info(f"""${dup.id}%-4s ${d} ${counts.mkString("  ")}""")
          }
        case Left((status, msg)) =>
          log.warning(s"Error getting movements: ${msg}")
      }
    }

    await(fut)

    0
  }
}
