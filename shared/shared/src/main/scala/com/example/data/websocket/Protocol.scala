package com.example.data.websocket

import com.example.data.MatchDuplicate
import com.example.data.Id
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.Team
import play.api.libs.json._
import com.example.data.rest.JsonSupport._
import com.example.data.MatchChicago
import com.example.data.MatchRubber

object Protocol {

  /** The websocket protocol name */
  val DuplicateBridge = "DuplicateBridge"
  val Logging = "Logging"

  // When adding another subclass of Message,
  // be sure to add the serialization and deserialization
  // code in the vals message2Writer and message2Reader
  sealed trait ToServerMessage
  sealed trait ToBrowserMessage

  // Unsolicited data from server to browser

  /**
   * A new browser is now watching the same game.
   * This is typically another table in the duplicate match.
   */
  case class MonitorJoined(id: String, members: Seq[String]) extends ToBrowserMessage
  /**
   * A browser is no longer watching the game.
   */
  case class MonitorLeft(id: String, members: Seq[String]) extends ToBrowserMessage

  // Requests to server from browser

  /**
   * Start monitoring duplicate match summary
   */
  case class StartMonitorSummary( summary: String = "") extends ToServerMessage
  /**
   * Stop monitoring duplicate match summary
   */
  case class StopMonitorSummary( summary: String = "") extends ToServerMessage
  /**
   * Start monitoring a duplicate match
   * @param dupid - the id of the duplicate match to start/stop monitoring
   */
  case class StartMonitorDuplicate( dupid: Id.MatchDuplicate ) extends ToServerMessage
  /**
   * Stop monitoring a duplicate match
   * @param dupid - the id of the duplicate match to start/stop monitoring
   */
  case class StopMonitorDuplicate( dupid: Id.MatchDuplicate ) extends ToServerMessage
  /**
   * Start monitoring a chicago match
   * @param chiid - the id of the chicago match to start/stop monitoring
   */
  case class StartMonitorChicago( chiid: Id.MatchChicago ) extends ToServerMessage
  /**
   * Stop monitoring a chicago match
   * @param chiid - the id of the chicago match to start/stop monitoring
   */
  case class StopMonitorChicago( chiid: Id.MatchChicago ) extends ToServerMessage
  /**
   * Start monitoring a rubber match
   * @param rubid - the id of the rubber match to start/stop monitoring
   */
  case class StartMonitorRubber( rubid: String ) extends ToServerMessage
  /**
   * Stop monitoring a rubber match
   * @param rubid - the id of the rubber match to start/stop monitoring
   */
  case class StopMonitorRubber( rubid: String ) extends ToServerMessage

  // Data that can flow either way

  /**
   * Update the MatchDuplicate.
   */
  case class UpdateDuplicate(matchDuplicate: MatchDuplicate) extends ToServerMessage with ToBrowserMessage
  /**
   * Update a board in a duplicate match.
   */
  case class UpdateDuplicateHand(dupid: Id.MatchDuplicate, hand: DuplicateHand) extends ToServerMessage with ToBrowserMessage
  /**
   * Update a board in a duplicate match.
   */
  case class UpdateDuplicateTeam(dupid: Id.MatchDuplicate, team: Team) extends ToServerMessage with ToBrowserMessage

  /**
   * Update the MatchChicago.
   */
  case class UpdateChicago(matchChicago: MatchChicago) extends ToServerMessage with ToBrowserMessage

  /**
   * Update the MatchRubber.
   */
  case class UpdateRubber(matchRubber: MatchRubber) extends ToServerMessage with ToBrowserMessage

  case class NoData( data: String = "") extends ToServerMessage with ToBrowserMessage

  import com.example.data.rest.JsonSupport._

  def toStringToBrowserMessage(msg: ToBrowserMessage) = {
    import com.example.data.rest.ToBrowserProtocolJsonSupport._
    msg.toJson
  }

  def fromStringToBrowserMessage(str: String) = {
    import com.example.data.rest.ToBrowserProtocolJsonSupport._
    str.parseJson[ToBrowserMessage]
  }

  def toStringToServerMessage(msg: ToServerMessage) = {
    import com.example.data.rest.ToServerProtocolJsonSupport._
    msg.toJson
  }

  def fromStringToServerMessage(str: String) = {
    import com.example.data.rest.ToServerProtocolJsonSupport._
    str.parseJson[ToServerMessage]
  }
}
