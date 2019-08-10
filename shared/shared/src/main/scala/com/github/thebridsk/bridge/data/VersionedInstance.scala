package com.github.thebridsk.bridge.data

/**
  * @tparam CurrentVersion the current version type
  * @tparam ThisVersion the type of this instance
  */
trait VersionedInstance[CurrentVersion, ThisVersion, VID] {

  def created: SystemTime.Timestamp

  def id: VID

  def setId(
      newId: VID,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): ThisVersion

  /**
    * @return a tuple, First is a boolean when true indicates input string was current version.
    *                   Second is the CurrentVersion object.
    */
  def convertToCurrentVersion(): (Boolean, CurrentVersion)

  def readyForWrite(): ThisVersion
}
