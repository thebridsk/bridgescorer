package com.github.thebridsk.bridge.manualtest

import com.github.thebridsk.utilities.main.Main
import com.github.thebridsk.bridge.backend.BridgeServiceFileStore
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.yaml.YamlSupport
import scala.concurrent.ExecutionContext.Implicits.global

object MatchDuplicateToYaml extends Main {

  def execute() = {
    val dir = Directory("./store")
    val store = new BridgeServiceFileStore( dir )

    store.duplicates.syncStore.readAll() match {
      case Right( r ) =>
        r.values.headOption match {
          case Some(md) =>
            import YamlSupport._
            val yaml = writeYaml(md)
            logger.info(s"YAML for match duplicate ${md.id}:\n${yaml}")
          case None =>
            logger.warning("Did not find any duplicate matches in store ${dir}")
        }
      case Left( (statuscode,msg) ) =>
        logger.warning("Did not find any duplicate matches in store ${dir}")
    }

    0
  }
}
