package com.github.thebridsk.bridge.data
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  title = "IndividualDuplicatePicture - Information about hand pictures for boards",
  description = "Information about hand pictures for boards"
)
case class IndividualDuplicatePicture(
    @Schema(description = "The board ID", required = true)
    boardId: IndividualBoard.Id,
    @Schema(description = "The hand ID", required = true)
    handId: IndividualDuplicateHand.Id,
    @Schema(
      description = "The URL relative to this servers root URL",
      required = true
    )
    url: String
) {
  def key: (IndividualBoard.Id, IndividualDuplicateHand.Id) = (boardId, handId)
}
