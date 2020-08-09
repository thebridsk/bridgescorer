package com.github.thebridsk.bridge.server.logging

import com.github.thebridsk.bridge.data.LoggerConfig
import java.io.Reader
import java.io.File
import com.github.thebridsk.bridge.server.yaml.YamlSupport
import java.io.InputStream

/**
  */
case class RemoteLoggingConfig(
    configs: Map[String, Map[String, LoggerConfig]]
) {

  def browserNames() = configs.keySet

  def browserConfigs(browser: String): Set[String] = {
    configs.get(browser).map(bc => bc.keySet).getOrElse(Set[String]())
  }

  def browserConfig(browser: String, config: String): Option[LoggerConfig] = {
    configs.get(browser).flatMap(bc => bc.get(config))
  }

}

/**
  * Handling of LoggerConfig configuration
  *
  * This object supports reading in configurations from a yaml file.
  *
  * The syntax is:
  *
  * configs:
  *   <browsername>:
  *       <configname>:
  *          loggers:
  *             - "[root]=ALL"
  *          appenders:
  *             - "console=ALL"
  *             - "server=ALL,bridge"
  *          clientid: "123"
  *          useRestToServer: true
  *          useSSEFromServer: true
  *       <configname2>:
  *          ...
  *   <browsername2>:
  *      ...
  *
  * // LoggerConfig( "[root]=ALL"::Nil, "console=ALL"::"server=ALL,bridge"::Nil, "123", true, true)
  *
  * The clientid field can be omitted from the yaml file, it will be set and overridden by the server
  * giving each client a unique id
  * The useRestToServer is optional, default is true.  if true, use REST calls to update server, otherwise use WebSockets
  * The useSSEFromServer is optional, default is true.  if true, use SSE to receive updates from server, otherwise use WebSockets
  */
object RemoteLoggingConfig {
  import play.api.libs.json._

  implicit val loggerConfigFormat: OFormat[LoggerConfig] =
    Json.format[LoggerConfig]
  implicit val remoteLoggerConfigFormat: OFormat[RemoteLoggingConfig] =
    Json.format[RemoteLoggingConfig]

  def getDefaultRemoteLoggerConfig(): Option[RemoteLoggingConfig] = {
    readFromResource("com/github/thebridsk/bridge/server/remoteLogging.yaml")
  }

  def read(config: String): RemoteLoggingConfig = {
    YamlSupport.fromYaml[RemoteLoggingConfig](config)
  }

  def readConfig(configFile: File): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](configFile)
  }

  def readConfig(reader: InputStream): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](reader)
  }

  def readConfig(reader: Reader): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](reader)
  }

  def readFromResource(resource: String): Option[RemoteLoggingConfig] = {
    val in = getClass.getClassLoader.getResourceAsStream(resource)
    if (in != null) {
      try {
        Some(readConfig(in))
      } finally {
        in.close
      }
    } else {
      None
    }
  }

  def apply() = new RemoteLoggingConfig(Map())
}
