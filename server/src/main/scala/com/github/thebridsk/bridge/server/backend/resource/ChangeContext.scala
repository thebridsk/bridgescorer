package com.github.thebridsk.bridge.server.backend.resource

sealed trait ChangeContextData
case class CreateChangeContext(
    newValue: Any,
    parentField: Option[String] = None
) extends ChangeContextData
case class UpdateChangeContext(
    newValue: Any,
    parentField: Option[String] = None
) extends ChangeContextData
case class DeleteChangeContext(
    oldValue: Any,
    parentField: Option[String] = None
) extends ChangeContextData

/**
  * @param changes the list of changes made.  The most specific one is the last one.
  */
class ChangeContext {
  private var seq: Int = 0
  private var mchanges: List[ChangeContextData] = Nil

  def changes = mchanges

  def setSeq(s: Int): ChangeContext = {
    seq = s
    this
  }

  def getSeq(): Int = {
    seq
  }

  def getSpecificChange(): Option[ChangeContextData] = {
    mchanges.lastOption
  }

  def prepend(ch: ChangeContextData): ChangeContext = {
    mchanges = ch :: mchanges
    this
  }

  def create(newValue: Any): ChangeContext =
    prepend(ChangeContext.create(newValue))

  def update(newValue: Any): ChangeContext =
    prepend(ChangeContext.update(newValue))

  def delete(oldValue: Any): ChangeContext =
    prepend(ChangeContext.delete(oldValue))

  def create(newValue: Any, parentField: String): ChangeContext =
    prepend(ChangeContext.create(newValue, parentField))

  def update(newValue: Any, parentField: String): ChangeContext =
    prepend(ChangeContext.update(newValue, parentField))

  def delete(oldValue: Any, parentField: String): ChangeContext =
    prepend(ChangeContext.delete(oldValue, parentField))

  override def toString(): String = {
    s"{ChangeContext seq=${seq} ${changes}}"
  }
}

object ChangeContext {

  def apply() = new ChangeContext

  def create(newValue: Any): CreateChangeContext =
    CreateChangeContext(newValue, None)

  def update(newValue: Any): UpdateChangeContext =
    UpdateChangeContext(newValue, None)

  def delete(oldValue: Any): DeleteChangeContext =
    DeleteChangeContext(oldValue, None)

  def create(newValue: Any, parentField: String): CreateChangeContext =
    CreateChangeContext(newValue, Option(parentField))

  def update(newValue: Any, parentField: String): UpdateChangeContext =
    UpdateChangeContext(newValue, Option(parentField))

  def delete(oldValue: Any, parentField: String): DeleteChangeContext =
    DeleteChangeContext(oldValue, Option(parentField))
}
