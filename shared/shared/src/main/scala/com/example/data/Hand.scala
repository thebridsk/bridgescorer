package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._
import com.example.data.SystemTime.Timestamp

/**
 * @author werewolf
 */
@ApiModel(description = "The result of playing a hand")
case class Hand(
    @(ApiModelProperty @field)(value="The ID of a hand", required=true)
    id: String,
    @(ApiModelProperty @field)(value="The number of tricks in the bid", required=true)
    contractTricks: Int,
    @(ApiModelProperty @field)(value="The suit of the bid", required=true)
    contractSuit: String,
    @(ApiModelProperty @field)(value="The doubling of the contract", required=true)
    contractDoubled: String,
    @(ApiModelProperty @field)(value="The declarer", required=true)
    declarer: String,
    @(ApiModelProperty @field)(value="true if NS was vulnerable", required=true)
    nsVul: Boolean,
    @(ApiModelProperty @field)(value="true if EW was vulnerable", required=true)
    ewVul: Boolean,
    @(ApiModelProperty @field)(value="true if the contract was made", required=true)
    madeContract: Boolean,
    @(ApiModelProperty @field)(value="The number of tricks made or down", required=true)
    tricks: Int,
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {
  require(0<=contractTricks && contractTricks <= 7)
  require(validContract())
  require(contractSuit.length()==1 && 0 <= "NSHDC".indexOf(contractSuit.charAt(0)))
  require(contractDoubled.length()==1 && 0 <= "NDR".indexOf(contractDoubled.charAt(0)))
  require(declarer.length()==1 && 0 <= "NEWS".indexOf(declarer.charAt(0)))

  def equalsIgnoreModifyTime( other: Hand ) = this == other.copy( created=created, updated=updated )

  def setId( newId: String, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, created=if (forCreate) time; else created, updated=time)
    }

  private def validContract() = {
    if (contractTricks == 0) true
    else if (madeContract) {
      if (contractTricks <= tricks && tricks <= 7) true
      else false
    } else {
      if (1 <= tricks && tricks <= 6+contractTricks) true
      else false
    }

  }

  def copyForCreate(id: String) = {
    val time = SystemTime.currentTimeMillis()
    copy( id=id, created=time, updated=time )
  }

  def contract = {
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
  def create(id: String,
                contractTricks: Int,
                contractSuit: String,
                contractDoubled: String,
                declarer: String,
                nsVul: Boolean,
                ewVul: Boolean,
                madeContract: Boolean,
                tricks: Int,
                created: Timestamp = 0,
                updated: Timestamp = 0 ) = new Hand(id,contractTricks,contractSuit,contractDoubled,declarer,nsVul,ewVul,madeContract,tricks,created,updated)
}
