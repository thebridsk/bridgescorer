package com.github.thebridsk.bridge.data.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateScore
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveComplete
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveDirector
import com.github.thebridsk.bridge.data.bridge.individual.IndividualDuplicateViewPerspective.PerspectiveTable

class TestIndividualScore extends AnyFlatSpec with Matchers {

  behavior of "the IndividualDuplicate class"

  it should "Serialize an IndividualDuplicate and deserialize it" in {
    val m = getMatch
    val s = writeJson(m)
    val mr = readJson[IndividualDuplicate](s)

    mr.equalsIgnoreModifyTime(m, true)
  }

  it should "get the sorted names" in {
    val m = getMatch.updatePlayer(2,"Kevin")
    val s = m.sortedPlayers
    val n = s.map(m.getPlayer(_))

    s mustBe List(1,3,4,5,6,7,8,2)

    def checkSort(cur: String, rest: List[String]): Boolean = {
      if (rest.isEmpty) true
      else if (cur < rest.head) {
        checkSort(rest.head, rest.tail)
      } else {
        false
      }
    }

    withClue(s"String s must be sorted: ${s}")(
      checkSort(n.head, n.tail) mustBe true
    )

  }

  behavior of "the IndividualDuplicateScore class"

  it should "perspective complete check scores on boards and totals for all players" in {
    val s = new IndividualDuplicateScore(getMatch, PerspectiveComplete)

    s.getBoardScore(1) match {
      case Some(b) =>
        (1 to 8).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 1, player ${p} got ${result}") {
                result.player mustBe p
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 1
                  result.nsIMP mustBe 0
                } else {
                  result.ewMP mustBe 1
                  result.ewIMP mustBe 0
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 1")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(2) match {
      case Some(b) =>
        List(8,2,5,4).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 2
                  result.nsIMP mustBe 1
                } else {
                  result.ewMP mustBe 2
                  result.ewIMP mustBe 1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
        List(1,3,6,7).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 0
                  result.nsIMP mustBe -1
                } else {
                  result.ewMP mustBe 0
                  result.ewIMP mustBe -1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(3) match {
      case Some(b) =>
        List(8,3,7,2).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe true
                result.hand.played.isDefined mustBe true
                result.hand.played.get.contract mustBe "4S"
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 3")
          }
        }
        List(4,1,6,5).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe true
                result.hand.played.isDefined mustBe false
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 3")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(4) match {
      case Some(b) =>
        List(8,3,7,2).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.player mustBe p
                result.hide mustBe true
                result.hand.played.isDefined mustBe false
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 4")
          }
        }
        List(4,1,6,5).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.player mustBe p
                result.hide mustBe true
                result.hand.played.isDefined mustBe false
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 4")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    List(8,2,5,4).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 3
            score.imp mustBe 1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

    List(1,3,6,7).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 1
            score.imp mustBe -1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

  }

  it should "perspective director check scores on boards and totals for all players" in {
    val s = new IndividualDuplicateScore(getMatch, PerspectiveDirector)

    s.getBoardScore(1) match {
      case Some(b) =>
        (1 to 8).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 1, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 1
                  result.nsIMP mustBe 0
                } else {
                  result.ewMP mustBe 1
                  result.ewIMP mustBe 0
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 1")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(2) match {
      case Some(b) =>
        List(8,2,5,4).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 2
                  result.nsIMP mustBe 1
                } else {
                  result.ewMP mustBe 2
                  result.ewIMP mustBe 1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
        List(1,3,6,7).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 0
                  result.nsIMP mustBe -1
                } else {
                  result.ewMP mustBe 0
                  result.ewIMP mustBe -1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(3) match {
      case Some(b) =>
        List(8,3,7,2).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                result.hand.played.isDefined mustBe true
                result.hand.played.get.contract mustBe "4S"
                result.nsMP mustBe 0
                result.ewMP mustBe 0
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
        List(4,1,6,5).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                result.hand.played.isDefined mustBe false
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    List(8,2,5,4).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 3
            score.imp mustBe 1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

    List(1,3,6,7).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 1
            score.imp mustBe -1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

  }

  it should "perspective table check scores on boards and totals for all players" in {
    val s = new IndividualDuplicateScore(getMatch, PerspectiveTable(Table.id(1), 3))

    s.getBoardScore(1) match {
      case Some(b) =>
        (1 to 8).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 1, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 1
                  result.nsIMP mustBe 0
                } else {
                  result.ewMP mustBe 1
                  result.ewIMP mustBe 0
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 1")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(2) match {
      case Some(b) =>
        List(8,2,5,4).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 2
                  result.nsIMP mustBe 1
                } else {
                  result.ewMP mustBe 2
                  result.ewIMP mustBe 1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
        List(1,3,6,7).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                if (result.isNS) {
                  result.nsMP mustBe 0
                  result.nsIMP mustBe -1
                } else {
                  result.ewMP mustBe 0
                  result.ewIMP mustBe -1
                }
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    s.getBoardScore(3) match {
      case Some(b) =>
        List(8,3,7,2).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                result.hand.played.isDefined mustBe true
                result.hand.played.get.contract mustBe "4S"
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
        List(4,1,6,5).foreach { p =>
          b.getResult(p) match {
            case Some(result) =>
              withClue(s"board 2, player ${p} got ${result}") {
                result.hide mustBe false
                result.hand.played.isDefined mustBe false
              }
            case None =>
              fail(s"Did not find score for player ${p} for board 2")
          }
        }
      case None =>
        fail("Did not find score for board 1")
    }

    List(8,2,5,4).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 3
            score.imp mustBe 1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

    List(1,3,6,7).foreach { p =>
      s.scores.get(p) match {
        case Some(score) =>
          withClue(s"player ${p} got ${score}") {
            score.mp mustBe 1
            score.imp mustBe -1
          }
        case None =>
          fail(s"Did not find score for player ${p}")
      }
    }

  }

  def getMatch: IndividualDuplicate = {
    IndividualDuplicate.create(
      id = IndividualDuplicate.id(1),
      players = List(
        "Adam",
        "Betty",
        "Cathy",
        "Don",
        "Elizabeth",
        "Fran",
        "George",
        "Harry"
      ),
      boards = List[IndividualBoard](
        IndividualBoard.apply(
          id = IndividualBoard.id(1),
          nsVul = false,
          ewVul = false,
          dealer = "N",
          // All players get 1 mp and 0 imp
          hands = List[IndividualDuplicateHand](
            IndividualDuplicateHand.apply(
              played = Some(
                Hand.create("p8", 3, "N", "N", "N", false, false, true, 3)
              ),
              table = Table.id(1),
              round = 1,
              board = IndividualBoard.id(1),
              north = 8,
              south = 1,
              east = 5,
              west = 7,
            ),
            IndividualDuplicateHand.apply(
              played = Some(
                Hand.create("p2", 3, "N", "N", "N", false, false, true, 3)
              ),
              table = Table.id(2),
              round = 1,
              board = IndividualBoard.id(1),
              north = 2,
              south = 6,
              east = 4,
              west = 3,
            )
          ),
        ),
        IndividualBoard.apply(
          id = IndividualBoard.id(2),
          nsVul = false,
          ewVul = false,
          dealer = "N",
          // players 8,2,5,4 get 2 mp, players 6,1,3,7 get 0
          hands = List[IndividualDuplicateHand](
            IndividualDuplicateHand.apply(
              played = Some(
                Hand.create("p8", 4, "S", "N", "N", false, false, true, 4)
              ),
              table = Table.id(1),
              round = 2,
              board = IndividualBoard.id(2),
              north = 8,
              south = 2,
              east = 6,
              west = 1,
            ),
            IndividualDuplicateHand.apply(
              played = Some(
                Hand.create("p3", 3, "N", "N", "N", false, false, true, 3)
              ),
              table = Table.id(2),
              round = 2,
              board = IndividualBoard.id(2),
              north = 3,
              south = 7,
              east = 5,
              west = 4,
            )
          ),
        ),
        IndividualBoard.apply(
          id = IndividualBoard.id(3),
          nsVul = false,
          ewVul = false,
          dealer = "N",
          hands = List[IndividualDuplicateHand](
            IndividualDuplicateHand.apply(
              played = Some(
                Hand.create("p8", 4, "S", "N", "N", false, false, true, 4)
              ),
              table = Table.id(1),
              round = 3,
              board = IndividualBoard.id(3),
              north = 8,
              south = 3,
              east = 7,
              west = 2,
            ),
            IndividualDuplicateHand.apply(
              played = None,
              table = Table.id(2),
              round = 3,
              board = IndividualBoard.id(3),
              north = 4,
              south = 1,
              east = 6,
              west = 5,
            )
          ),
        ),
        IndividualBoard.apply(
          id = IndividualBoard.id(4),
          nsVul = false,
          ewVul = false,
          dealer = "N",
          hands = List[IndividualDuplicateHand](
            IndividualDuplicateHand.apply(
              played = None,
              table = Table.id(1),
              round = 3,
              board = IndividualBoard.id(3),
              north = 8,
              south = 3,
              east = 7,
              west = 2,
            ),
            IndividualDuplicateHand.apply(
              played = None,
              table = Table.id(2),
              round = 3,
              board = IndividualBoard.id(3),
              north = 4,
              south = 1,
              east = 6,
              west = 5,
            )
          ),
        )
      ),
      boardset = BoardSet.idNul,
      movement = IndividualMovement.idNul,
    )
  }

}
