package com.github.thebridsk.bridge.data

import scala.reflect.ClassTag


trait MovementBase extends Ordered[MovementBase] {

  def short: String
  def description: String
  def tables: Int
  def getBoards: List[Int]
  def nameAsString: String
  def isDisabled: Boolean
  def isDeletable: Boolean
  def isResetToDefault: Boolean

  def getId: MovementBase.Id
  def getIndividualId: Option[IndividualMovement.Id] = None
  def getMovementId: Option[Movement.Id] = None

  /**
    * get the key for sorting movements
    *
    * @return Tuple, (ntables, name)
    */
  def sortKey: (Int, String) = (tables, short)

  /** Result of comparing `this` with operand `that`.
   *
   * Implement this method to determine how instances of A will be sorted.
   *
   * Returns `x` where:
   *
   *   - `x < 0` when `this < that`
   *
   *   - `x == 0` when `this == that`
   *
   *   - `x > 0` when  `this > that`
   *
   */
  def compare(that: MovementBase): Int = {
    val o = that.sortKey
    val t = this.sortKey

    val r = t._1.compare(o._1)
    if (r == 0) {
      t._2.compare(o._2)
    } else {
      r
    }
  }

  def withId[T](
    ifn: IndividualMovement.Id => T,
    mfn: Movement.Id => T,
    default: => T
  ): T = {
    getIndividualId.map(ifn(_))
      .getOrElse(getMovementId.map(mfn(_)).getOrElse(default))
  }
}

trait IdMovementBase

object MovementBase extends HasId[IdMovementBase]("", true) {
  override def id(i: Int): Id = {
    throw new IllegalArgumentException(
      "MovementBase Ids can not be generated, must use Movement.Id or IndividualMovement.Id"
    )
  }

  def useId[T](
      id: MovementBase.Id,
      fmd: Movement.Id => T,
      fmdr: IndividualMovement.Id => T,
      default: => T
  ): T = {
    id.toSubclass[Movement.ItemType].map(fmd).getOrElse {
      id.toSubclass[IndividualMovement.ItemType].map(fmdr).getOrElse(default)
    }
  }

  def use[T](
      mov: MovementBase,
      fmov: Movement => T,
      fimov: IndividualMovement => T,
      default: => T
  ): T = {
    mov match {
      case m: Movement => fmov(m)
      case m: IndividualMovement => fimov(m)
      case _ => default
    }
  }

  import com.github.thebridsk.bridge.data.{Id => DId}
  def runIf[T <: IdMovementBase: ClassTag, R](
      id: MovementBase.Id,
      default: => R
  )(f: DId[T] => R): R = {
    id.toSubclass[T].map(sid => f(sid)).getOrElse(default)
  }
}
