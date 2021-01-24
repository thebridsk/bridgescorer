package com.github.thebridsk.bridge.data


trait MovementBase extends Ordered[MovementBase] {

  def short: String
  def description: String
  def tables: Int
  def getBoards: List[Int]
  def nameAsString: String
  def isDisabled: Boolean

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
