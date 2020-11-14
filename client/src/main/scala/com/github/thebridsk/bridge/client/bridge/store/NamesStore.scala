package com.github.thebridsk.bridge.client.bridge.store

import japgolly.scalajs.react.Callback
import com.github.thebridsk.bridge.clientcommon.rest2.RestClientNames
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.logger.Alerter
import com.github.thebridsk.bridge.clientcommon.demo.BridgeDemo
import flux.dispatcher.DispatchToken
import com.github.thebridsk.bridge.client.bridge.action.BridgeDispatcher
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateMatch
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateTeam
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.client.controller.ChicagoController
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicago
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicago5
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateChicagoNames
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateRubber
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateRubberNames

object NamesStore extends ChangeListenable {
  val logger: Logger = Logger("bridge.ViewPlayers")

  /**
    * Required to instantiate the store.
    */
  def init(): Unit = {
//    refreshNames()
  }

  private var names: List[String] = Nil

  def getNames = names

  private var dispatchToken: Option[DispatchToken] = {
    if (BridgeDemo.isDemo) {
      initNames
      Some(BridgeDispatcher.register(dispatch _))
    } else {
      None
    }
  }

  def updateNames(addnames: String*): Unit = {
    val an = onlyValidNames(addnames.toList)
    names = (names.filter(n => !an.contains(n)).toList ++ an).sorted
  }

  def namesFromTeam(team: Team): List[String] = {
    onlyValidNames(Option(team.player1).toList ::: Option(team.player2).toList)
  }

  def namesFromChicago(mc: MatchChicago): List[String] = {
    onlyValidNames(mc.players)
  }

  def onlyValidNames(names: List[String]): List[String] =
    names.filter(p => p != "")

  /**
    * Flux dispatcher call
    *
    * Only called when in demo mode
    *
    * @param msg
    */
  def dispatch(msg: Any): Unit =
    Alerter.tryitWithUnit {
      msg match {
        case ActionUpdateDuplicateMatch(duplicate) =>
          val p = duplicate.teams.flatMap(namesFromTeam(_))
          if (!p.isEmpty) updateNames(p: _*)
        case ActionUpdateTeam(dupid, team) =>
          val p = namesFromTeam(team)
          if (!p.isEmpty) updateNames(p: _*)
        case ActionUpdateChicago(chi, _) =>
          updateNames(namesFromChicago(chi).distinct: _*)
        case ActionUpdateChicago5(chiid, extra, _) =>
          updateNames(extra)
        case ActionUpdateChicagoNames(chiid, p1, p2, p3, p4, extra, _, _, _) =>
          val n = (p1 :: p2 :: p3 :: p4 :: extra.toList).distinct
          updateNames(n: _*)
        case ActionUpdateRubber(rub, _) =>
          updateNames(rub.north, rub.south, rub.east, rub.west)
        case ActionUpdateRubberNames(rubid, north, south, east, west, _, _) =>
          updateNames(north, south, east, west)
        case action =>
        // There are multiple stores, all the actions get sent to all stores
//      logger.fine("Ignoring unknown action: "+action)
      }
    }

  def initNames: Unit = {
    if (BridgeDemo.isDemo) {
      val mcs = ChicagoController.getSummaryFromLocalStorage
      if (mcs.isEmpty) {
        names = List(
          "Barry",
          "Bill",
          "Cathy",
          "Irene",
          "June",
          "Kelly",
          "Larry",
          "Norman",
          "Victor"
        )
      } else {
        val n = mcs.flatMap(mc => namesFromChicago(mc)).toList.distinct
        updateNames(n: _*)
      }
    }
  }

  /**
    * Refresh the names in the list.
    * @param cb - callback that should be called when the names have been updated
    */
  def refreshNames(cb: Option[Callback] = None): Any = {
    if (BridgeDemo.isDemo) {
      import scala.scalajs.js.timers._

      setTimeout(1) { // note the absence of () =>
        cb.foreach { _.runNow() }
      }
    } else {
      import scala.concurrent.ExecutionContext.Implicits.global
      RestClientNames
        .list()
        .recordFailure()
        .foreach(list =>
          Alerter.tryitWithUnit {
            names = list.toList
            cb.foreach { _.runNow() }
          }
        )
    }
  }

  /**
    * Ensure names are cached, get names from server if they are not.
    * @param cb - callback that should be called when the names have been updated.  Will call the callback immediately if the names are cached.
    */
  def ensureNamesAreCached(cb: Option[Callback] = None): Unit =
    Alerter.tryitWithUnit {
      refreshNames(cb)
      // if (names.isEmpty) refreshNames(cb)
      // else cb.foreach(_.runNow())
    }
}
