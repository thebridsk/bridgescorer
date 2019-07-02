package com.example.datastore

import utils.main.Subcommand
import utils.logging.Logger
import org.rogach.scallop._
import scala.concurrent.duration.Duration
import scala.reflect.io.Path
import com.example.backend.BridgeServiceFileStore
import java.io.File
import java.io.Reader
import java.io.BufferedReader
import scala.io.Source
import scala.io.BufferedSource
import scala.util.Left
import java.io.InputStream
import com.example.backend.BridgeServiceInMemory
import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.data.MatchChicago
import com.example.data.MatchRubber
import akka.http.scaladsl.model.StatusCodes
import com.example.backend.resource.FileIO
import com.example.backend.BridgeServiceFileStoreConverters
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

trait ConvertBoardSetsAndMovementsCommand

object ConvertBoardSetsAndMovementsCommand extends Subcommand("convertboards") {
  import DataStoreCommands.optionStore

  val log = Logger[ConvertBoardSetsAndMovementsCommand]

  implicit def dateConverter: ValueConverter[Duration] =
    singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

  val validValues = List("yaml", "json")

  descr("converts a database to use yaml or json")

  banner(s"""
Converts boardsets and movements to use yaml or json.  The updated files are written to the current directory.

Syntax:
  ${DataStoreCommands.cmdName} convertboards type
Options:""")

  val paramType = trailArg[String](
    name = "type",
    required = true,
    validate = (s) => validValues.contains(s),
    descr = s"""The type to convert to. Valid values: ${validValues
      .mkString("\"", "\", \"", "\"")}."""
  )

  footer(s"""
""")

  def executeSubcommand(): Int = {

    val storedir = optionStore().toDirectory
    val t = paramType()
    try {

      log.info(s"Converting datastore ${storedir} to ${t}")

      val useYaml = t == "yaml"
      val datastore = new BridgeServiceFileStore(storedir, useYaml = useYaml)

      val converters = new BridgeServiceFileStoreConverters(useYaml)

      Await.result(datastore.boardSets.readAll(), 30.seconds) match {
        case Right(map) =>
          map.values.foreach { bs =>
            val name = bs.name
            val data = converters.boardSetJson.toJson(bs)
            FileIO.writeFile(new File("Boardset" + name + "." + t), data)
          }
        case Left((status, msg)) =>
          log.warning(s"Error getting boardsets: ${msg}")
      }

      Await.result(datastore.movements.readAll(), 30.seconds) match {
        case Right(map) =>
          map.values.foreach { m =>
            val name = m.name
            val data = converters.movementJson.toJson(m)
            FileIO.writeFile(new File("Movement" + name + "." + t), data)
          }
        case Left((status, msg)) =>
          log.warning(s"Error getting movements: ${msg}")
      }

      log.info(s"Done converting boardsets and movements ${storedir} to ${t}")

      0
    } catch {
      case x: Exception =>
        log.severe(
          s"Error converting boardsets and movements ${storedir} to ${t}",
          x
        )
        1
    }

  }
}
