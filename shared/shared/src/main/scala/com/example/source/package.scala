package com.github.thebridsk.bridge

import org.scalactic.source.Position

package object source {

  implicit class SourcePosition(val pos: Position) extends AnyVal {
    def line = s"${pos.fileName}:${pos.lineNumber}"

    def lineForFilename = s"${pos.fileName}_${pos.lineNumber}"
  }

  object SourcePosition {
    implicit def here(implicit pos: Position) = new SourcePosition(pos)
  }

}
