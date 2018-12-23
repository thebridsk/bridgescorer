package com.example.data

/**
 * @tparam CurrentVersion the current version type
 * @tparam ThisVersion the type of this instance
 */
trait VersionedInstance[CurrentVersion,ThisVersion,VID] {

  def id: VID

  def setId( newId: VID, forCreate: Boolean, dontUpdateTime: Boolean = false ): ThisVersion

  def convertToCurrentVersion(): CurrentVersion

  def readyForWrite(): ThisVersion
}
