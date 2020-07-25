package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  title = "Team - A team from a duplicate match",
  description = "A team from a duplicate match"
)
case class Team(
    @Schema(description = "The ID of the team.", required = true)
    id: Team.Id,
    @Schema(description = "The name of player 1 on the team", required = true)
    player1: String,
    @Schema(description = "The name of player 2 on the team", required = true)
    player2: String,
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

  def equalsIgnoreModifyTime(other: Team) =
    this == other.copy(created = created, updated = updated)

  def setId(newId: Team.Id, forCreate: Boolean) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

  def copyForCreate(id: Team.Id) = {
    val time = SystemTime.currentTimeMillis()
    copy(id = id, created = time, updated = time)
  }

  def areBothPlayersSet() =
    player1 != null && player1.length() > 0 && player2 != null && player2
      .length() > 0

  def setPlayers(p1: String, p2: String) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      player1 = Option(p1).getOrElse(""),
      player2 = Option(p2).getOrElse(""),
      updated = time
    )
  }

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    * @return None if the names were not changed.  Some() with the modified object
    */
  def modifyPlayers(nameMap: Map[String, String]) = {

    def getName(n: String) = nameMap.get(n).getOrElse(n)

    val np1 = getName(player1)
    val np2 = getName(player2)

    if (np1.equals(player1) && np2.equals(player2)) {
      None
    } else {
      Some(copy(player1 = np1, player2 = np2))
    }
  }
}

trait IdTeam

object Team extends HasId[IdTeam]("T") {
  def create(id: Team.Id, player1: String, player2: String) = {
    val time = SystemTime.currentTimeMillis()
    new Team(id, player1, player2, time, time)
  }
  def create(id: Team.Id) = {
    val time = SystemTime.currentTimeMillis()
    new Team(id, "", "", time, time)
  }

}
