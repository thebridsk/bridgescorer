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
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait ConvertToCommand

object ConvertToCommand extends Subcommand("convertto") {
  import DataStoreCommands.optionStore

  val log = Logger[ConvertToCommand]

  implicit def dateConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration(_))

  import utils.main.Converters._

  val validValues = List("yaml","json")

  descr("converts a database to use yaml or json")

  banner(s"""
Converts a database to use yaml or json

Syntax:
  ${DataStoreCommands.cmdName} convertto type
Options:""")

  val paramType = trailArg[String]( name="type",
                                    required=true,
                                    validate= (s)=>validValues.contains(s),
                                    descr=s"""The type to convert to. Valid values: ${validValues.mkString("\"", "\", \"", "\"")}.""")

  footer(s"""
""")

  def executeSubcommand(): Int = {

    val storedir = optionStore().toDirectory
    val t = paramType()
    try {

      log.info(s"Converting datastore ${storedir} to ${t}")

      val datastore = new BridgeServiceFileStore( storedir, useYaml=t=="yaml" )

      Await.result( datastore.rubbers.readAll(), 30.seconds )
      Await.result( datastore.chicagos.readAll(), 30.seconds )
      Await.result( datastore.duplicates.readAll(), 30.seconds )
      Await.result( datastore.duplicateresults.readAll(), 30.seconds )
      Await.result( datastore.boardSets.readAll(), 30.seconds )
      Await.result( datastore.movements.readAll(), 30.seconds )

      log.info(s"Done converting datastore ${storedir} to ${t}")

      0
    } catch {
      case x: Exception =>
        log.severe(s"Error converting datastore ${storedir} to ${t}", x)
        1
    }

  }
}
