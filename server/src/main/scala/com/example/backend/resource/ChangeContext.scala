package com.example.backend.resource

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

  private var mchanges: List[ChangeContextData] = Nil

  def changes = mchanges

  def getSpecificChange(): Option[ChangeContextData] = {
    mchanges.lastOption
  }

  def prepend(ch: ChangeContextData) = {
    mchanges = ch :: mchanges
    this
  }

  def create(newValue: Any) = prepend(ChangeContext.create(newValue))

  def update(newValue: Any) = prepend(ChangeContext.update(newValue))

  def delete(oldValue: Any) = prepend(ChangeContext.delete(oldValue))

  def create(newValue: Any, parentField: String) =
    prepend(ChangeContext.create(newValue, parentField))

  def update(newValue: Any, parentField: String) =
    prepend(ChangeContext.update(newValue, parentField))

  def delete(oldValue: Any, parentField: String) =
    prepend(ChangeContext.delete(oldValue, parentField))

  override def toString() = {
    "{ChangeContext " + changes + "}"
  }
}

object ChangeContext {

  def apply() = new ChangeContext

  def create(newValue: Any) = CreateChangeContext(newValue, None)

  def update(newValue: Any) = UpdateChangeContext(newValue, None)

  def delete(oldValue: Any) = DeleteChangeContext(oldValue, None)

  def create(newValue: Any, parentField: String) =
    CreateChangeContext(newValue, Option(parentField))

  def update(newValue: Any, parentField: String) =
    UpdateChangeContext(newValue, Option(parentField))

  def delete(oldValue: Any, parentField: String) =
    DeleteChangeContext(oldValue, Option(parentField))
}
