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
  * @param id the ID, Should follow the pattern "[a-zA-Z]*\d+", case sensitive.
  * @param classTag a ClassTag for the class which instances of are identified by this ID

  * @param A the ID type
  *
  */
case class Id[A]( val id: String )(implicit val classTag: ClassTag[A]) extends Ordered[Id[A]] {
  override
  def equals( other: Any ) = {
    // println(s"comparing $this and $other")
    other match {
      case o: Id[A] => o.id == id && (o.classTag.runtimeClass eq classTag.runtimeClass)
      case _ => false
    }
  }
  override
  def hashCode() = id.hashCode() + classTag.runtimeClass.hashCode()

  override
  def toString() = s"Id[${classTag.runtimeClass.getSimpleName}]($id)"

  def compare(that: Id[A]): Int = {
    val Id.idpattern(s1) = id
    val Id.idpattern(s2) = that.id

    s1.toInt.compareTo(s2.toInt)
  }

  /**
    * Get just the number in the Id
    *
    * @return the number as a string.
    * If the id does not match idpattern, then id is returned.
    */
  def toNumber = id match {
    case Id.idpattern(bn) => bn
    case _                => id
  }

  def toInt = toNumber.toInt

  def isNul = id == ""
}

class HasId[IdType: ClassTag]( prefix: String ) {

  import com.github.thebridsk.bridge.data
  type Id = data.Id[IdType]
  type Type = IdType
  /**
    * @param i the complete id with a syntax of "[a-zA-Z]*\d+"
    * @return the Id object
    */
  def id( i: String ): Id = data.Id[IdType](i)
  /**
    *
    *
    * @param i the Id number
    * @return the Id object with and id of s"$prefix$i"
    */
  def id( i: Int ): Id = id( s"$prefix$i" )

  def idNul = id("")

  val idKeyReads = new KeyReads[data.Id[IdType]] {
    def readKey(key: String ): JsResult[data.Id[IdType]] = JsSuccess( id(key) )
  }

  val idKeyWrites = new KeyWrites[data.Id[IdType]] {
    def writeKey(key: data.Id[IdType]): String = key.id
  }

  val jsonFormat = new Format[data.Id[IdType]] {
    val classTag = implicitly[ClassTag[IdType]]

    def reads(json: JsValue): JsResult[data.Id[IdType]] = {
      json match {
        case s: JsString =>
          JsSuccess( id(s.value) )
        case e =>
          JsError( Seq() )
      }
    }

    def writes(o: data.Id[IdType]): JsValue = {
      JsString( o.id )
    }

  }

}

object Id {

  def apply[A]( id: String )(implicit classTag: ClassTag[A]) = new Id[A](id)

  import com.github.thebridsk.bridge.data

  @Schema(
    description =
      "The id of a duplicate match result, just has the points scored by a team"
  )
  type MatchDuplicateResult = String
  @Schema(description = "The id of a duplicate match")
  type MatchDuplicate = String
  @Schema(description = "The id of a Chicago match")
  type MatchChicago = String
  @Schema(description = "The id of a duplicate board, positive integer")
  type DuplicateBoard = data.Board.Id
  @Schema(description = "The id of a duplicate hand")
  type DuplicateHand = data.Team.Id
  @Schema(description = "The id of a team")
  type Team = data.Team.Id
  @Schema(description = "The id of a table")
  type Table = data.Table.Id

  private val idpattern = "[a-zA-Z]*(\\d+)".r
  def idComparer(id1: String, id2: String): Int = {
    val idpattern(s1) = id1
    val idpattern(s2) = id2

    s1.toInt.compareTo(s2.toInt)
  }

  def duplicateIdToNumber(id: Id.MatchDuplicate) = id match {
    case idpattern(bn) => bn
    case _             => id
  }

  def genericIdToNumber(id: String) = id match {
    case idpattern(bn) => bn
    case _             => id
  }

  def isValidIdPattern(s: String): Boolean = s match {
    case idpattern(n) => true
    case _            => false
  }
}

object IdOrdering {

  trait IdOrderingBase[T] extends Ordering[T] {

    def compare(x: T, y: T): Int = {
      Id.idComparer(x.toString(), y.toString())
    }

  }

  implicit object IDMatchDuplicateOrdering
      extends IdOrderingBase[Id.MatchDuplicate]

  implicit object IDMatchDuplicateResultOrdering
      extends IdOrderingBase[Id.MatchDuplicateResult]

  implicit object IDMatchChicagoOrdering extends IdOrderingBase[Id.MatchChicago]

}
