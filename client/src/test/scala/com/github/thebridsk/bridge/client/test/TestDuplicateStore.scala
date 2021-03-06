package com.github.thebridsk.bridge.client.test

import com.github.thebridsk.bridge.client.bridge.store.DuplicateStore
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.sample.TestMatchDuplicate
import com.github.thebridsk.bridge.client.bridge.action.ActionStartDuplicateMatch
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateHand
import com.github.thebridsk.bridge.client.bridge.store.ChangeListenable
import com.github.thebridsk.bridge.data.js.SystemTimeJs
import com.github.thebridsk.bridge.data.bridge.MatchDuplicateScore
import com.github.thebridsk.bridge.data.bridge.PerspectiveTable
import com.github.thebridsk.bridge.client.bridge.action.ActionUpdateDuplicateMatch
import japgolly.scalajs.react.Callback
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHandV2

class TestDuplicateStore extends AnyFlatSpec with Matchers {

  SystemTimeJs()

  abstract class Tester {
    def changeListener(): Unit
    def doAction(): Unit

    private var changeCalled = false

    private def internalChangeListener =
      Callback {
        changeCalled = true;
        changeListener()
      }

    def run(): Unit = {
      try {
        DuplicateStore.addChangeListener(internalChangeListener)
        doAction()
        assert(changeCalled, "change must be called")
      } finally {
        DuplicateStore.removeChangeListener(internalChangeListener)
        DuplicateStore.removeAllListener(ChangeListenable.event)
      }
    }
  }

  behavior of "TestDuplicateStore in bridgescorer-client"

  val dupid: MatchDuplicate.Id = MatchDuplicate.id(1)

  it should "CreateMatchDuplicate" in {
    new Tester {
      def changeListener(): Unit = {
        DuplicateStore.getId() match {
          case Some(id) => id mustBe dupid
          case _        => fail("Did not get an ID from DuplicateStore")
        }
        DuplicateStore.getMatch() match {
          case Some(dm) => fail("should not see a matchduplicate")
          case _        =>
        }
      }
      def doAction(): Unit = {
        DuplicateStore.dispatch(ActionStartDuplicateMatch(dupid))
      }
    }.run()
  }

  it should "UpdateMatchDuplicate" in {
    new Tester {
      val startMatch = TestMatchDuplicate.create(dupid)
      def changeListener(): Unit = {
        DuplicateStore.getMatch() match {
          case Some(dm) => dm mustBe startMatch
          case _        => fail("Did not update store")
        }
      }
      def doAction(): Unit = {
        DuplicateStore.dispatch(ActionUpdateDuplicateMatch(startMatch))
      }
    }.run()
  }

  def getHand(
      boardid: Board.Id,
      handid: Team.Id,
      contractTricks: Int,
      contractSuit: String,
      contractDoubled: String,
      declarer: String,
      madeContract: Boolean,
      tricks: Int
  ): DuplicateHandV2 = {
    val b = DuplicateStore.getMatch().get.getBoard(Board.id(1)).get
    val dh = b.getHand(Team.id(1)).get
    dh.updateHand(
      Hand.create(
        dh.id.id,
        contractTricks,
        contractSuit,
        contractDoubled,
        declarer,
        b.nsVul,
        b.ewVul,
        madeContract,
        tricks
      )
    )
  }

  it should "PlayHands" in {
    val hands = TestMatchDuplicate.getHands(DuplicateStore.getMatch().get)
    for (hand <- hands) {
      new Tester {
        def changeListener(): Unit = {
          DuplicateStore.getMatch() match {
            case Some(dm) =>
              if (
                !dm
                  .getBoard(hand.board)
                  .get
                  .getHand(hand.id)
                  .get
                  .equalsIgnoreModifyTime(hand)
              ) fail("hands must be equal")
            case _ => fail("Did not update store")
          }
        }
        def doAction(): Unit = {
          DuplicateStore.dispatch(ActionUpdateDuplicateHand(dupid, hand))
        }
      }.run()
    }
  }

  it should "Score" in {
    val score = MatchDuplicateScore(
      DuplicateStore.getMatch().get,
      PerspectiveTable(Team.id(1), Team.id(2))
    )
    score.teamScores mustBe TestMatchDuplicate.getTeamScore()
  }
}
