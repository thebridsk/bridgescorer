package com.github.thebridsk.bridge.data
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  title = "DuplicatePicture - Information about hand pictures for boards",
  description = "Information about hand pictures for boards"
)
case class DuplicatePicture(
  @Schema(description = "The board ID", required = true)
  boardId: Id.DuplicateBoard,
  @Schema(description = "The hand ID", required = true)
  handId: Id.DuplicateHand,
  @Schema(description = "The URL relative to this servers root URL", required = true)
  url: String
) {
  def key = (boardId,handId)
}
