package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main
import scala.reflect.io.Path
import com.github.thebridsk.bridge.backend.BridgeService
import scala.concurrent.ExecutionContext
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.MatchDuplicate
import scala.concurrent.Future
import com.github.thebridsk.bridge.backend.BridgeResources
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.github.thebridsk.bridge.data.rest.JsonSupport
import com.github.thebridsk.bridge.backend.resource.FileIO

object GenerateDemoMatchDuplicate extends Main {

  import com.github.thebridsk.utilities.main.Converters._

  val optionStore = opt[Path]("store", short='s', descr="The store directory, default=./store", argName="dir", default=Some("../testdata"))

  val optionOut = opt[Path]("out", short='o', descr="The output file, default=src/main/public/demo/demoMatchDuplicates.json", argName="outfile", default=Some("src/main/public/demo/demoMatchDuplicates.json"))

  val paramArgs = trailArg[List[String]](
      name = "ids",
      descr = "Ids of matches to add to demo",
      required = true
      )

  def execute() = {
    implicit val ec = ExecutionContext.global
    val store = BridgeService(optionStore.toOption.get)

    val support = BridgeResources( yaml=false )

    val frmds = paramArgs.toOption.get.map{ sid => sid.asInstanceOf[Id.MatchDuplicate] }.
                        map { id =>
                          store.duplicates.read(id)
                        }
    val fmds = Future.foldLeft(frmds)(List[MatchDuplicate]()) { (ac,v) =>
      v match {
        case Right(md) => md::ac
        case Left(err) => ac
      }
    }
    val list = Await.result(fmds, Duration("60s"))

    import play.api.libs.json._
    import JsonSupport._

    val json = writeJson(list)

    val out = optionOut.toOption.get

    FileIO.writeFile(out.jfile, json)

    0
  }
}
