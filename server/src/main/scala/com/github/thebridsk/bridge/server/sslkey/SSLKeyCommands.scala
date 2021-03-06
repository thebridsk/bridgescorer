package com.github.thebridsk.bridge.sslkey

import org.rogach.scallop._
import scala.concurrent.duration.Duration
import java.util.logging.Level
import com.github.thebridsk.bridge.server.Server
import com.github.thebridsk.utilities.main.Subcommand
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.utilities.logging.Config
import scala.reflect.io.Path

trait SSLKeyCommands

object SSLKeyCommands extends Subcommand("sslkey") {

  val log: Logger = Logger[SSLKeyCommands]()

  private var savelevel: Level = null

  override def init(): Int = {
    savelevel = Config.getLevelOnConsoleHandler()
    if (savelevel == null || savelevel.intValue() > Level.INFO.intValue()) {
      Config.setLevelOnConsoleHandler(Level.INFO)
    }
    0
  }

  override def cleanup(): Unit = {
    Config.setLevelOnConsoleHandler(savelevel)
  }

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import com.github.thebridsk.utilities.main.Converters._

  val cmdName: String = s"${Server.cmdName} sslkey"

  descr("Various commands to generate ssl keys")

  banner(s"""
Commands on sslkey

Syntax:
  ${cmdName} [options] cmd
Options:""")

  shortSubcommandsHelp(true)

  val optionKeyDir: ScallopOption[Path] = opt[Path](
    "dir",
    short = 'd',
    descr = "The directory that has/will get the keys, default=./key",
    argName = "dir",
    default = Some("./key")
  )

  addSubcommand(GenerateSelfSigned)
  addSubcommand(GenerateServerCert)
  addSubcommand(GenerateCA)
  addSubcommand(ValidateCert)

  def executeSubcommand(): Int = {
    log.severe("Unknown options specified")
    1
  }
}
