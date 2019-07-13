package com.github.thebridsk.bridge.data

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema

/**
  * Contains a set of boards.  For each board there is the following information:
  * <dl>
  * <dt>id
  * <dd>the board number as a string
  * <dt>NSVul
  * <dd>true if NS is vulnerable
  * <dt>EWVul
  * <dd>true if EW is vulnerable
  * <dt>Dealear
  * <dd>The dealer, valid values are: "N", "E", "S", "W"
  * </dl>
  * <pre><code>
  * {
  *   "name": "ArmonkBoards",
  *   "short": "Armonk Boards",
  *   "description": "The boards used by Armonk bridge group",
  *   "boards": [
  *     {
  *       "id": "1",
  *       "NSVul": "false",
  *       "EWVul": "false",
  *       "Dealer": "N"
  *     },
  *     ...
  *   ]
  * }
  * </code></pre>
  *
  */
@Schema(
  name = "BoardSet",
  title = "BoardSet - A set of boards for duplicate bridge.",
  description = "A set of boards for duplicate bridge."
)
case class BoardSetV1(
    @Schema(description = "The name of the boardset", required = true)
    name: String,
    @Schema(
      description = "A short description of the boardset",
      required = true
    )
    short: String,
    @Schema(description = "A long description of the boardset", required = true)
    description: String,
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(implementation = classOf[BoardInSet]),
      arraySchema = new Schema(
        description = "All the boards in the boardset",
        required = true
      )
    )
    boards: List[BoardInSet]
) extends VersionedInstance[BoardSetV1, BoardSetV1, String] {

  def id = name

  def setId(
      newId: String,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ) = {
    copy(name = newId)
  }

  def convertToCurrentVersion() = (true, this)

  def readyForWrite() = this

}

@Schema(
  name = "BoardInSet",
  title = "BoardInSet - The vulnerabilities and dealer of a board",
  description =
    "Shows the vulnerabilities NS and EW and dealer of a board when a hand is played."
)
case class BoardInSet(
    @Schema(
      description = "The board number, 1, 2, ...",
      required = true,
      minimum = "1"
    )
    id: Int,
    @Schema(description = "true if NS is vulnerable", required = true)
    nsVul: Boolean,
    @Schema(description = "true if EW is vulnerable", required = true)
    ewVul: Boolean,
    @Schema(
      description = "the dealer",
      required = true,
      allowableValues = Array("N", "S", "E", "W"),
      `type` = "enum"
    )
    dealer: String
)
