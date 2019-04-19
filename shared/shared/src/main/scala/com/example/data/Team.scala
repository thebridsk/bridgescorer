package com.example.data

import com.example.data.SystemTime.Timestamp

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    title = "Team - A team from a duplicate match",
    description = "A team from a duplicate match"
)
case class Team(
    @Schema(description="The ID of the team.", required=true)
    id: Id.Team,
    @Schema(description="The name of player 1 on the team", required=true)
    player1: String,
    @Schema(description="The name of player 2 on the team", required=true)
    player2: String,
    @Schema(description="When the duplicate hand was created, in milliseconds since 1/1/1970 UTC", required=true)
    created: Timestamp,
    @Schema(description="When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC", required=true)
    updated: Timestamp ) {

  def equalsIgnoreModifyTime( other: Team ) = this == other.copy( created=created, updated=updated )

  def setId( newId: Id.Team, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, created=if (forCreate) time; else created, updated=time)
    }

  def copyForCreate(id: Id.Team) = {
    val time = SystemTime.currentTimeMillis()
    copy( id=id, created=time, updated=time )
  }

  def areBothPlayersSet() = player1!=null&&player1.length()>0&&player2!=null&&player2.length()>0

  def setPlayers( p1: String, p2: String ) = {
    val time = SystemTime.currentTimeMillis()
    copy( player1=if (p1==null) ""; else p1, player2=if (p2==null) ""; else p2, updated=time )
  }

  /**
   * Modify the player names according to the specified name map.
   * The timestamp is not changed.
   * @return None if the names were not changed.  Some() with the modified object
   */
  def modifyPlayers( nameMap: Map[String,String] ) = {

    def getName( n: String ) = nameMap.get(n).getOrElse(n)

    val np1 = getName(player1)
    val np2 = getName(player2)

    if (np1.equals(player1) && np2.equals(player2)) {
      None
    } else {
      Some( copy( player1=np1, player2=np2 ) )
    }
  }
}

object Team {
  def create( id: Id.Team, player1: String, player2: String ) = {
    val time = SystemTime.currentTimeMillis()
    new Team(id,player1,player2,time,time)
  }
  def create( id: Id.Team ) = {
    val time = SystemTime.currentTimeMillis()
    new Team(id,"","",time,time)
  }
}
