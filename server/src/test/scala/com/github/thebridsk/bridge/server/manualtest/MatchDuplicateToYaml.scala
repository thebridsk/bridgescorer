package com.github.thebridsk.bridge.server.manualtest

import com.github.thebridsk.utilities.main.MainNoArgs
import com.github.thebridsk.bridge.server.backend.BridgeServiceFileStore
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.yaml.YamlSupport
import scala.concurrent.ExecutionContext.Implicits.global

object MatchDuplicateToYaml extends MainNoArgs {

  def execute(): Int = {
    val dir = Directory("./store")
    val store = new BridgeServiceFileStore(dir)

    store.duplicates.syncStore.readAll() match {
      case Right(r) =>
        r.values.headOption match {
          case Some(md) =>
            import YamlSupport._
            val yaml = writeYaml(md)
            logger.info(s"YAML for match duplicate ${md.id}:\n${yaml}")
          case None =>
            logger.warning("Did not find any duplicate matches in store ${dir}")
        }
      case Left((statuscode, msg)) =>
        logger.warning("Did not find any duplicate matches in store ${dir}")
    }

    0
  }
}
