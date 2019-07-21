package com.github.thebridsk.browserpages

import org.scalatest.exceptions.ModifiableMessage
import org.scalatest.exceptions.TestFailedException
import org.scalactic.source.Position
import com.github.thebridsk.source.SourcePosition
import org.scalatest.Assertions._

trait PagesAssertions {

  def exceptionToFail[T]( fun: => T )(implicit pos: Position) = {
    try {
      fun
    } catch {
      case x: ModifiableMessage[_] =>
        throw x
      case x: Exception =>
        throw new TestFailedException( s"${pos.line} ${x}", x, 1 )
    }
  }

  def withClueEx[T]( clue: Any )( fun: => T )(implicit pos: Position) = {
    withClue(clue) {
      exceptionToFail( fun )
    }
  }
}

object PagesAssertions extends PagesAssertions
