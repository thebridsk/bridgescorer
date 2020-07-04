package com.github.thebridsk.bridge.datastore

import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.bridge.server.version.VersionServer
import com.github.thebridsk.bridge.data.version.VersionShared
import java.io.File
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import java.util.logging.Level
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Config
import scala.reflect.io.Path

trait DataStoreCommands

object DataStoreCommands extends Subcommand("datastore") {

  val log = Logger[DataStoreCommands]()

  private var savelevel: Level = null

  override def init() = {
    savelevel = Config.getLevelOnConsoleHandler()
    if (savelevel == null || savelevel.intValue() > Level.INFO.intValue()) {
      Config.setLevelOnConsoleHandler(Level.INFO)
    }
    0
  }

  override def cleanup() = {
    Config.setLevelOnConsoleHandler(savelevel)
  }

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val cmdName = s"${Server.cmdName} ${name}"

  descr("Various commands act on the datastore")

  banner(s"""
Commands on datastore

Syntax:
  ${cmdName} [options] cmd
Options:""")

  shortSubcommandsHelp(true)

  val optionStore = opt[Path](
    "store",
    short = 's',
    descr = "The store directory, default=./store",
    argName = "dir",
    default = Some("./store")
  )

  addSubcommand(ShowCommand)
  addSubcommand(SetNamesCommand)
  addSubcommand(ConvertToCommand)
  addSubcommand(ConvertBoardSetsAndMovementsCommand)
  addSubcommand(Copy)

  def executeSubcommand(): Int = {
    log.severe("Unknown options specified")
    1
  }
}
