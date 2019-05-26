package com.example.backend.resource

import com.example.data.VersionedInstance
import scala.reflect.ClassTag
import utils.logging.Logger
import scala.language.reflectiveCalls
import play.api.libs.json._
import com.example.data.rest.JsonException

object VersionedInstanceJson {
  val log = Logger[VersionedInstanceJson[_,_]]

/**
 * Provides to/from JSON conversion of a resource object.
 * Can read multiple versions of the resource are supported and converts them to the latest version.
 * Writes the latest version only.
 *
 * @param TId the type of the ID for this resource
 * @param T the type of the latest version of the resource.  This must be a subclass of VersionedInstance.
 *
 * @constructor
 * @param converter to convert and object to/from JSON/YAML
 * @param reader a Reads object to convert a JSON/YAML representation to an object
 * @param writer a Writes object to convert an object to JSON/YAML representation.
 * @param classtag ClassTag object of the type of the latest version.
 */
  def apply[TId, T <: VersionedInstance[T,T, TId]]( implicit converter: Converter,
                                                        reader: Reads[T],
                                                        writer: Writes[T],
                                                        classtag: ClassTag[T] ) = new VersionedInstanceJson[TId, T]
}

/**
 * @param TId the type of the ID for this resource
 * @param T the type of the latest version of the resource.  This must be a subclass of VersionedInstance.
 * @param R the type of an older version of the resource.  This must be a subclass of VersionedInstance, and
 * convertable to the latest version, see [[VersionedInstance]]
 *
 * @constructor
 * @param converter to convert and object to/from JSON/YAML
 * @param reader a Reads object to convert a JSON/YAML representation to an object, an old version
 * @param writer a Writes object to convert an object to JSON/YAML representation, old version.
 * @param classtag ClassTag object of the type of the old version.
 * @param classtagCurrent ClassTag object of the type of the latest version.
 */
private class ReaderAndConvert[TId,
                               T <: VersionedInstance[T,T, TId],
                               R <: VersionedInstance[T,R,TId]]
                              ( implicit converter: Converter,
                                         reader: Reads[R],
                                         writer: Writes[R],
                                         classtag: ClassTag[R],
                                         classtagCurrent: ClassTag[T] ) {

  def parse(s: String): ( Boolean, T) = {
    val (primary, r) = parseOld(s)
    val cur = r.convertToCurrentVersion()
    (primary && cur._1, cur._2)
  }

  /**
   * @param s the json string of the object
   * @return a tuple, First is a boolean when true indicates input string was current version.
   *                   Second is the T object.
   */
  def parseOld( s: String ): ( Boolean, R ) = converter.read[R](s)

  def toJsonOld( r: R ) = converter.write(r)

  def getOldClass = classtag.runtimeClass.asInstanceOf[Class[R]]
  def getCurrentClass = classtagCurrent.runtimeClass.asInstanceOf[Class[T]]

  def nameOld = getOldClass.getName
  def nameCurrent = getCurrentClass.getName

  override
  def toString() = s"Convert(${nameOld} -> ${nameCurrent})"

}

/**
 * Provides to/from JSON conversion of a resource object.
 * Can read multiple versions of the resource are supported and converts them to the latest version.
 * Writes the latest version only.
 *
 * @param TId the type of the ID for this resource
 * @param T the type of the latest version of the resource.  This must be a subclass of VersionedInstance.
 *
 * @constructor
 * @param converter to convert and object to/from JSON/YAML
 * @param reader a Reads object to convert a JSON/YAML representation to an object
 * @param writer a Writes object to convert an object to JSON/YAML representation.
 * @param classtagCurrent ClassTag object of the type of the latest version.
 */
class VersionedInstanceJson[TId, T <: VersionedInstance[T,T, TId]](
                    implicit
                      converter: Converter,
                      reader: Reads[T],
                      writer: Writes[T],
                      classtagCurrent: ClassTag[T]
                    ) {

  import VersionedInstanceJson._

  private val currentReaderAndConvert =  new ReaderAndConvert[TId, T, T]

  /** ReadAndConvert objects for older versions. */
  private val converters = scala.collection.mutable.ListBuffer[ReaderAndConvert[TId,T,_]]()

  /**
   * Add support for an old version of resource to this object.
   *
   * @param R the type of an older version of the resource.  This must be a subclass of VersionedInstance, and
   * convertable to the latest version, see [[VersionedInstance]]
   *
   * @param reader a Reads object to convert a JSON/YAML representation to an object
   * @param writer a Writes object to convert an object to JSON/YAML representation.
   * @param classtag ClassTag object of the type of the older version.
   * @return this.  allowing chaining all the old versions in one statement.
   */
  def add[R <: VersionedInstance[T,R,TId]]( implicit reader: Reads[R],
                                                     writer: Writes[R],
                                                     classtag: ClassTag[R]): VersionedInstanceJson[TId, T] = {
    converters += new ReaderAndConvert[TId, T, R]
    this
  }

  /**
   * Parse the string and return the latest version.  If the string represents an older version,
   * then the older version object is converted to the latest version.
   * @param the string representation.  This can be in any of the supported versions.
   * @return a tuple, First is a boolean when true indicates input string was current version.
   *                   Second is the T object.
   */
  def parse( s: String ): (Boolean, T) = {
    try {
      currentReaderAndConvert.parse(s)
    } catch {
      case e: JsonException =>
        var lastE: Option[Exception] = None
        for ( conv <- converters ) {
          try {
//              logger.warning("Trying another converter")
            val rc = conv.parse(s)
            log.warning(s"Converted ${rc._2.id} with ${conv}" )
            return (false,rc._2)      // this is an old version
          } catch {
            case e: Exception =>
              lastE = Some(e)
              log.warning(s"Oops with ${conv}: ${e}")
          }
        }
        throw e
    }
  }

  /**
   * Returns the string representation of the object.
   * @param t the object to convert.  Only the latest version can be written.
   * @return the string representation.
   */
  def toJson( t: T ) = {
    converter.write(t)
  }

  /**
   * get the file extensions to use when reading files.  The order is the order they should be tried.
   */
  def getReadExtensions(): List[String] = converter.getReadExtensions()

  /**
   * get the file extension to use when writing files.
   */
  def getWriteExtension(): String = converter.getWriteExtension()

}
