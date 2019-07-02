package com.example.backend.resource

import play.api.libs.json._
import com.example.yaml.YamlSupport
import utils.logging.Logger
import com.example.data.rest.JsonException

object Converter {
  private[resource] val log = Logger[Converter]

  /**
    * Returns a converter for yaml or json.
    * @param yaml true if yaml is primary, false if json is primary
    * @return the converter
    */
  def getConverter(yaml: Boolean): Converter = {
    if (yaml) {
      new JsonYamlConverter(YamlConverter, JsonConverter)
    } else {
      new JsonYamlConverter(JsonConverter, YamlConverter)
    }
  }
}

trait Converter {

  /**
    * Quick check if this string could be converted.
    * @return true if this string looks like it could be parsed.
    */
  def isPossible(s: String): Boolean

  /**
    * Parse the string
    * @param s the string to parse
    * @return a tuple, First is a boolean when true indicates input string was in primary format.
    *                   Second is the T object.
    * @throws JsonException
    */
  def read[T](s: String)(implicit reads: Reads[T]): (Boolean, T)

  /**
    * Write the object
    * @param t the object to write
    * @return the string representation of the object
    */
  def write[T](t: T)(implicit writes: Writes[T]): String

  /**
    * get the file extensions to use when reading files.  The order is the order they should be tried.
    */
  def getReadExtensions(): List[String] = List(getWriteExtension)

  /**
    * get the file extension to use when writing files.
    */
  def getWriteExtension(): String
}

/**
  * Converter for JSON
  * This class assumes that the string does NOT start with a "-"
  */
object JsonConverter extends Converter {

  def isPossible(s: String): Boolean =
    s.find(c => c != ' ').filter(c => c != '-').isDefined

  def read[T](s: String)(implicit reads: Reads[T]): (Boolean, T) = {
    (true, YamlSupport.readJson(s))
  }

  def write[T](t: T)(implicit writes: Writes[T]): String = {
    YamlSupport.writePrettyJson(t)
  }

  def getWriteExtension(): String = ".json"
}

/**
  * Converter for YAML.
  * This class assumes that the string being parsed starts with "---"
  */
object YamlConverter extends Converter {

  def isPossible(s: String): Boolean =
    s.find(c => c != ' ').filter(c => c == '-').isDefined

  def read[T](s: String)(implicit reads: Reads[T]): (Boolean, T) = {
    (true, YamlSupport.fromYaml(s))
  }

  def write[T](t: T)(implicit writes: Writes[T]): String = {
    YamlSupport.writeYaml(t)
  }

  def getWriteExtension(): String = ".yaml"

}

/**
  * Converter class that can read either JSON or YAML, and write out only the primary one (JSON or YAML)
  */
class JsonYamlConverter(primary: Converter, secondary: Converter*)
    extends Converter {

  def isPossible(s: String): Boolean =
    primary.isPossible(s) || secondary.find(sec => sec.isPossible(s)).isDefined

  /**
    * @return a tuple, First is a boolean when true indicates input string was in primary format.
    *                   Second is the T object.
    */
  def read[T](s: String)(implicit reads: Reads[T]): (Boolean, T) = {
    if (primary.isPossible(s)) {
      primary.read(s)
    } else {
      var lastx: Option[Exception] = None
      for (sec <- secondary) {
        try {
          if (sec.isPossible(s)) {
            val (p, t) = sec.read(s)
            return (false, t)
          }
        } catch {
          case x: Exception =>
            Converter.log
              .fine(s"""Error parsing with ${sec.getClass.getName}: ${s}""", x)
            lastx = Some(x)
        }
      }
      throw lastx.getOrElse(new JsonException("Did not find parser for ${s}"))
    }
  }

  def write[T](t: T)(implicit writes: Writes[T]): String = {
    primary.write(t)
  }

  /**
    * get the file extensions to use when reading files.  The order is the order they should be tried.
    */
  override def getReadExtensions(): List[String] =
    secondary.foldLeft(primary.getReadExtensions()) { (ac, s) =>
      ac ::: s.getReadExtensions()
    }

  /**
    * get the file extension to use when writing files.
    */
  def getWriteExtension(): String = primary.getWriteExtension()

}
