package com.example.logging

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.util.{List => JList, Map => JMap}
import scala.collection.JavaConverters._
import com.example.data.LoggerConfig
import java.io.Reader
import java.io.File
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import com.example.data.rest.JsonException
import com.example.yaml.YamlSupport
import java.io.InputStream

/**
 *
 */
case class RemoteLoggingConfig(
    configs: Map[ String, Map[ String, LoggerConfig ]]
    ) {

  def browserNames() = configs.keySet

  def browserConfigs( browser: String ) = {
    configs.get(browser).map(bc => bc.keySet).getOrElse(Set[String]())
  }

  def browserConfig( browser: String, config: String ): Option[LoggerConfig] = {
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
 *
 */
object RemoteLoggingConfig {
  import play.api.libs.json._

  implicit val loggerConfigFormat = Json.format[LoggerConfig]
  implicit val remoteLoggerConfigFormat = Json.format[RemoteLoggingConfig]

  def getDefaultRemoteLoggerConfig() = {
    readFromResource("com/example/remoteLogging.yaml")
  }

  def read( config: String ): RemoteLoggingConfig = {
    YamlSupport.fromYaml[RemoteLoggingConfig](config)
  }

  def readConfig( configFile: File ): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](configFile)
  }

  def readConfig( reader: InputStream ): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](reader)
  }

  def readConfig( reader: Reader ): RemoteLoggingConfig = {
    YamlSupport.readYaml[RemoteLoggingConfig](reader)
  }

  def readFromResource( resource: String ): Option[RemoteLoggingConfig] = {
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
