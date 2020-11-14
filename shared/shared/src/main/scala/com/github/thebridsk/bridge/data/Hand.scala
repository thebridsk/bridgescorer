package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import io.swagger.v3.oas.annotations.media.Schema

/**
  * @author werewolf
  */
@Schema(
  title = "Hand - The result of playing a hand",
  description = "The result of playing a hand"
)
case class Hand(
    @Schema(description = "The ID of a hand", required = true)
    id: String,
    @Schema(
      description = "The number of tricks in the bid",
      required = true,
      minimum = "0",
      maximum = "7"
    )
    contractTricks: Int,
    @Schema(
      description = "The suit of the bid",
      required = true,
      `type` = "enum",
      allowableValues = Array("N", "S", "H", "D", "C")
    )
    contractSuit: String,
    @Schema(
      description = "The doubling of the contract",
      required = true,
      `type` = "enum",
      allowableValues = Array("N", "D", "R")
    )
    contractDoubled: String,
    @Schema(
      description = "The declarer",
      required = true,
      `type` = "enum",
      allowableValues = Array("N", "S", "E", "W")
    )
    declarer: String,
    @Schema(description = "true if NS was vulnerable", required = true)
    nsVul: Boolean,
    @Schema(description = "true if EW was vulnerable", required = true)
    ewVul: Boolean,
    @Schema(description = "true if the contract was made", required = true)
    madeContract: Boolean,
    @Schema(
      description = "The number of tricks made or down",
      required = true,
      minimum = "0",
      maximum = "13"
    )
    tricks: Int,
    @Schema(
      description =
        "When the duplicate hand was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp
) {
  require(0 <= contractTricks && contractTricks <= 7)
  require(validContract())
  require(
    contractSuit.length() == 1 && 0 <= "NSHDC".indexOf(contractSuit.charAt(0))
  )
  require(
    contractDoubled.length() == 1 && 0 <= "NDR"
      .indexOf(contractDoubled.charAt(0))
  )
  require(declarer.length() == 1 && 0 <= "NEWS".indexOf(declarer.charAt(0)))

  def equalsIgnoreModifyTime(other: Hand): Boolean =
    this == other.copy(created = created, updated = updated)

  def setId(newId: String, forCreate: Boolean): Hand = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  private def validContract() = {
    if (contractTricks == 0) true
    else if (madeContract) {
      if (contractTricks <= tricks && tricks <= 7) true
      else false
    } else {
      if (1 <= tricks && tricks <= 6 + contractTricks) true
      else false
    }

  }

  def copyForCreate(id: String): Hand = {
    val time = SystemTime.currentTimeMillis()
    copy(id = id, created = time, updated = time)
  }

  def contract: String = {
    if (contractTricks == 0) {
      "PassedOut"
    } else {
      val d = contractDoubled match {
        case "N" => ""
        case "D" => "*"
        case "R" => "**"
      }
      s"${contractTricks}${contractSuit}${d}"
    }
  }
}

object Hand {
  def create(
      id: String,
      contractTricks: Int,
      contractSuit: String,
      contractDoubled: String,
      declarer: String,
      nsVul: Boolean,
      ewVul: Boolean,
      madeContract: Boolean,
      tricks: Int,
      created: Timestamp = 0,
      updated: Timestamp = 0
  ) =
    new Hand(
      id,
      contractTricks,
      contractSuit,
      contractDoubled,
      declarer,
      nsVul,
      ewVul,
      madeContract,
      tricks,
      created,
      updated
    )
}
