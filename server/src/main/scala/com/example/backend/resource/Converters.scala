package com.example.backend.resource

import play.api.libs.json._
import com.example.yaml.YamlSupport

trait BaseConverter {

  /**
   * Quick check if this string could be converted.
   * @return true if this string looks like it could be parsed.
   */
  def isPossible( s: String ): Boolean

  /**
   * Parse the string
   * @param s the string to parse
   * @return the object.
   * @throws JsonException
   */
  def read[T]( s: String )( implicit reads: Reads[T] ): T

  /**
   * Write the object
   * @param t the object to write
   * @return the string representation of the object
   */
  def write[T]( t: T )( implicit writes: Writes[T] ): String

  /**
   * get the file extension to use when reading/writing files.
   */
  def getExtension(): String
}

/**
 * Converter for JSON
 * This class assumes that the string does NOT start with a "-"
 */
class JsonConverter extends BaseConverter {

  def isPossible( s: String ): Boolean = s.find(c => c!=' ').filter(c => c!='-').isDefined

  def read[T]( s: String )( implicit reads: Reads[T] ): T = {
    YamlSupport.readJson(s)
  }

  def write[T]( t: T )( implicit writes: Writes[T] ): String = {
    YamlSupport.writePrettyJson(t)
  }

  def getExtension(): String = ".json"
}

/**
 * Converter for YAML.
 * This class assumes that the string being parsed starts with "---"
 */
class YamlConverter extends BaseConverter {

  def isPossible( s: String ): Boolean = s.find(c => c!=' ').filter(c => c=='-').isDefined

  def read[T]( s: String )( implicit reads: Reads[T] ): T = {
    YamlSupport.fromYaml(s)
  }

  def write[T]( t: T )( implicit writes: Writes[T] ): String = {
    YamlSupport.writeYaml(t)
  }

  def getExtension(): String = ".yaml"

}

/**
 * Converter class that can read either JSON or YAML, and write out only the primary one (JSON or YAML)
 */
class JsonYamlConverter( primary: BaseConverter, secondary: BaseConverter ) {

  /**
   * @return a tuple, First is a boolean when true indicates input string was in primary format.
   *                   Second is the T object.
   */
  def read[T]( s: String )( implicit reads: Reads[T] ): (Boolean,T) = {
    if (primary.isPossible(s)) {
      (true,primary.read(s))
    } else {
      (false,secondary.read(s))
    }
  }

  def write[T]( t: T )( implicit writes: Writes[T] ): String = {
    primary.write(t)
  }

  /**
   * get the file extensions to use when reading files.  The order is the order they should be tried.
   */
  def getReadExtensions(): List[String] = List( primary.getExtension(), secondary.getExtension() )

  /**
   * get the file extension to use when writing files.
   */
  def getWriteExtension(): String = primary.getExtension()

}
