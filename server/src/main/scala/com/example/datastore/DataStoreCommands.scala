package com.example.datastore

import utils.main.Main
import com.example.version.VersionServer
import com.example.version.VersionShared
import java.io.File
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import java.util.logging.Level
import com.example.Server
import utils.main.Subcommand
import utils.logging.Logger
import utils.logging.Config
import scala.reflect.io.Path

trait DataStoreCommands

object DataStoreCommands extends Subcommand("datastore") {
  import DataStoreCommands._

  val log = Logger[DataStoreCommands]

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

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

  val cmdName = "${Server.cmdName} datastore"

  banner(s"""
Commands on datastore

Syntax:
  ${cmdName} [options] cmd
Options:""")

  shortSubcommandsHelp(true)

  val optionStore = opt[Path]("store",
                              short='s',
                              descr="The store directory, default=./store",
                              argName="dir",
                              default=Some("./store"))

  addSubcommand(ShowCommand)
  addSubcommand(SetNamesCommand)
  addSubcommand(ConvertToCommand)
  addSubcommand(ConvertBoardSetsAndMovementsCommand)

  def executeSubcommand(): Int = {
    log.severe("Unknown options specified")
    1
  }
}
