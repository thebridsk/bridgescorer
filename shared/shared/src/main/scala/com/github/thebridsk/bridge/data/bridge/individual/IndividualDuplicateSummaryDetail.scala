package com.github.thebridsk.bridge.data.bridge.individual

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  title = "IndividualDuplicateSummaryDetails - Player stats in a match",
  description = "Details about a player in a match"
)
case class IndividualDuplicateSummaryDetails(
    @Schema(description = "The name of the player", required = true)
    player: String,
    @Schema(
      description = "The number of times the player was declarer",
      required = true,
      minimum = "0"
    )
    declarer: Int = 0,
    @Schema(
      description =
        "The number of times the player made the contract as declarer",
      required = true,
      minimum = "0"
    )
    made: Int = 0,
    @Schema(
      description = "The number of times the player went down as declarer",
      required = true,
      minimum = "0"
    )
    down: Int = 0,
    @Schema(
      description = "The number of times the player defended the contract",
      required = true,
      minimum = "0"
    )
    defended: Int = 0,
    @Schema(
      description =
        "The number of times the player took down the contract as defenders",
      required = true,
      minimum = "0"
    )
    tookDown: Int = 0,
    @Schema(
      description =
        "The number of times the player allowed the contract to be made as defenders",
      required = true,
      minimum = "0"
    )
    allowedMade: Int = 0,
    @Schema(
      description = "The number of times the player passed out a game",
      required = true,
      minimum = "0"
    )
    passed: Int = 0
) {

  def add(v: IndividualDuplicateSummaryDetails): IndividualDuplicateSummaryDetails = {
    copy(
      declarer = declarer + v.declarer,
      made = made + v.made,
      down = down + v.down,
      defended = defended + v.defended,
      tookDown = tookDown + v.tookDown,
      allowedMade = allowedMade + v.allowedMade,
      passed = passed + v.passed
    )
  }

  def percentMade: Double = if (declarer == 0) 0.0 else made * 100.0 / declarer
  def percentDown: Double = if (declarer == 0) 0.0 else down * 100.0 / declarer
  def percentAllowedMade: Double =
    if (defended == 0) 0.0 else allowedMade * 100.0 / defended
  def percentTookDown: Double =
    if (defended == 0) 0.0 else tookDown * 100.0 / defended

  def percentDeclared: Double =
    if (total == 0) 0.0 else declarer * 100.0 / total
  def percentDefended: Double =
    if (total == 0) 0.0 else defended * 100.0 / total
  def percentPassed: Double = if (total == 0) 0.0 else passed * 100.0 / total

  /**
    * Returns the total number of hands played by the player.
    */
  def total: Int = declarer + defended + passed
}

object IndividualDuplicateSummaryDetails {
  def zero(player: String) = new IndividualDuplicateSummaryDetails(player)
  def passed(player: String) = new IndividualDuplicateSummaryDetails(player, passed = 1)
  def made(player: String) =
    new IndividualDuplicateSummaryDetails(player, declarer = 1, made = 1)
  def down(player: String) =
    new IndividualDuplicateSummaryDetails(player, declarer = 1, down = 1)
  def allowedMade(player: String) =
    new IndividualDuplicateSummaryDetails(player, defended = 1, allowedMade = 1)
  def tookDown(player: String) =
    new IndividualDuplicateSummaryDetails(player, defended = 1, tookDown = 1)
}
