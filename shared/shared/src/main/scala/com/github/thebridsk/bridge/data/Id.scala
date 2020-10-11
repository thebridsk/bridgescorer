package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import scala.reflect.ClassTag
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.KeyWrites
import play.api.libs.json.KeyReads

/**
  * Represents an ID.
  *
  * @param id the ID, Should follow the pattern "[a-zA-Z]*\d+", case sensitive if useName is false.
  * @param useName use a name instead of the Id pattern.
  * @param classTag a ClassTag for the class which instances of are identified by this ID
  *
  * @param A the ID type
  */
@Schema(description = "The ID", `type` = "string") //, format = "id")
case class Id[A] private[data] (
    @Schema(hidden = true)
    val id: String,
    @Schema(hidden = true)
    useName: Boolean = false
)(
    @Schema(hidden = true)
    implicit private[Id] val classTag: ClassTag[A]
) extends Ordered[Id[A]] {
  override def equals(other: Any): Boolean = {
    // println(s"comparing $this and $other")
    other match {
      case o: Id[A] =>
        o.id == id && (o.classTag.runtimeClass eq classTag.runtimeClass)
      case _ => false
    }
  }
  override def hashCode(): Int =
    id.hashCode() + classTag.runtimeClass.hashCode()

  override def toString(): String =
    s"Id[${classTag.runtimeClass.getSimpleName}]($id)"

  def compare(that: Id[A]): Int = {
    if (useName) {
      id.compareTo(that.id)
    } else {
      val Id.idpattern(p1, s1) = id
      val Id.idpattern(p2, s2) = that.id

      val r = p1.compareTo(p2)
      if (r == 0) {
        s1.toInt.compareTo(s2.toInt)
      } else {
        r
      }
    }
  }

  /**
    * Get just the number in the Id
    *
    * @return the number as a string.
    * If the id does not match idpattern, then id is returned.
    * @throws IllegalStateException is useName is true
    */
  def toNumber: String = {
    if (useName) throw new IllegalStateException
    id match {
      case Id.idpattern(_, bn) => bn
      case _                   => id
    }
  }

  def toInt = toNumber.toInt

  @Schema(hidden = true)
  def isNul: Boolean = id == ""

  def toBase[B >: A]: Id[B] = this.asInstanceOf[Id[B]]
  def toSubclass[S <: A](implicit sTag: ClassTag[S]): Option[Id[S]] = {
    val cls = classTag.runtimeClass
    if (sTag.runtimeClass.isAssignableFrom(cls)) Some(this.asInstanceOf[Id[S]])
    else None
  }
}

class HasId[IdType: ClassTag](
    val prefix: String,
    val useName: Boolean = false
) {

  import com.github.thebridsk.bridge.data
  type ItemType = IdType
  type Id = data.Id[ItemType]

  /**
    * Generate an Id from a string.
    * @param s the complete id,
    * if useName is false then must have a syntax of "[a-zA-Z]*\d+",
    * where the leading characters are the prefix, that is s"$prefix$n",
    * where $prefix is the prefix and $n is a non-negative integer
    * if useName is true, then the string can be anything.
    * @return the Id object
    * @throws IllegalArgumentException if s is not a valid Id for this type of item.
    */
  def id(s: String): Id = {
    if (!useName && !s.isEmpty()) {
      val p = Id.parseId(s)
      if (p.isEmpty || p.get._1 != prefix)
        throw new IllegalArgumentException(
          s"String not valid for Ids for class ${getClass.getSimpleName}: $s"
        )
    }
    Id(s, useName)
  }

  /**
    * @param i the Id number
    * @return the Id object with and id of s"$prefix$i"
    */
  def id(i: Int): Id = id(s"$prefix$i")

  def idNul: data.Id[IdType] = Id("", useName)

  val idKeyReads: KeyReads[data.Id[IdType]] = new KeyReads[data.Id[IdType]] {
    def readKey(key: String): JsResult[data.Id[IdType]] = JsSuccess(id(key))
  }

  val idKeyWrites: KeyWrites[data.Id[IdType]] = new KeyWrites[data.Id[IdType]] {
    def writeKey(key: data.Id[IdType]): String = key.id
  }

  val jsonFormat: jsonFormat = new jsonFormat
  class jsonFormat extends Format[data.Id[IdType]] {
    val classTag: ClassTag[IdType] = implicitly[ClassTag[IdType]]

    def reads(json: JsValue): JsResult[data.Id[IdType]] = {
      json match {
        case s: JsString =>
          JsSuccess(id(s.value))
        case e =>
          JsError(Seq())
      }
    }

    def writes(o: data.Id[IdType]): JsValue = {
      JsString(o.id)
    }

  }

}

object Id {

  def apply[A](id: String)(implicit classTag: ClassTag[A]) = new Id[A](id)

  private val idpattern = "([a-zA-Z]*)(\\d+)".r

  def parseId(s: String): Option[(String, String)] = {
    s match {
      case idpattern(p, i) => Some((p, i))
      case _               => None
    }
  }

  def isValidIdPattern(s: String): Boolean =
    s match {
      case idpattern(p, n) => true
      case _               => false
    }

  import scala.language.implicitConversions
  implicit def toBase[A, B >: A](id: Id[A]): Id[B] = id.toBase

}

object IdOrdering {

  trait IdOrderingBase[T] extends Ordering[T] {

    def compare(x: T, y: T): Int = {
      // Id.idComparer(x.toString(), y.toString())
      0
    }

  }

  implicit object IDMatchDuplicateOrdering
      extends IdOrderingBase[MatchDuplicate.Id]

  implicit object IDMatchDuplicateResultOrdering
      extends IdOrderingBase[MatchDuplicateResult.Id]

  implicit object IDMatchChicagoOrdering extends IdOrderingBase[MatchChicago.Id]

}
