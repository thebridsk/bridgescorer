package com.example.yaml

import play.api.libs.json.jackson.PlayJsonModule
import play.api.libs.json._
import com.example.data.rest.JsonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.Reader
import java.io.Writer
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import com.example.data.rest.JsonSupport

trait YamlSupport extends JsonSupport {

  private lazy val mapper = {
    new ObjectMapper(new YAMLFactory())
      .registerModule(new PlayJsonModule(JsonParserSettings()))
  }

  def fromYaml[T](config: String)(implicit reads: Reads[T]): T = {
    val jsvalue = try {
      mapper.readValue(config, classOf[JsValue])
    } catch {
      // IOException, JsonParseException, JsonMappingException
      case x: Exception =>
        throw new JsonException("Error reading data", x)
    }
    convertJson[T](jsvalue)
  }

  def readYaml[T](configFile: File)(implicit reads: Reads[T]): T = {
    readYaml[T](new FileInputStream(configFile))
  }

  def readYaml[T](is: InputStream)(implicit reads: Reads[T]): T = {
    readYaml[T](new BufferedReader(new InputStreamReader(is, "UTF8")))
  }

  def readYaml[T](reader: Reader)(implicit reads: Reads[T]): T = {
    val jsvalue = try {
      mapper.readValue(reader, classOf[JsValue])
    } catch {
      // IOException, JsonParseException, JsonMappingException
      case x: Exception =>
        throw new JsonException("Error reading data", x)
    }
    convertJson[T](jsvalue)
  }

  def writeYaml[T](t: T)(implicit writes: Writes[T]): String = {
    val jsvalue = Json.toJson(t)
    mapper.writeValueAsString(jsvalue)
  }

  def writeYaml[T](os: OutputStream, t: T)(implicit writes: Writes[T]): Unit = {
    val w = new BufferedWriter(new OutputStreamWriter(os, "UTF8"))
    writeYaml(w, t)
    w.flush()
  }

  def writeYaml[T](writer: Writer, t: T)(implicit writes: Writes[T]): Unit = {
    val jsvalue = Json.toJson(t)
    mapper.writeValue(writer, jsvalue)
  }

  def writeYaml[T](outfile: File, t: T)(implicit writes: Writes[T]): Unit = {
    import resource._
    for (os <- managed(new FileOutputStream(outfile))) {
      writeYaml(os, t)
    }
  }
}

object YamlSupport extends YamlSupport
