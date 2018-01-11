package com.example.data

import com.example.data.SystemTime.Timestamp

import io.swagger.annotations._
import scala.annotation.meta._

@ApiModel(description = "A table for a duplicate match")
case class Table(
    @(ApiModelProperty @field)(value="The ID of the table", required=true)
    id: String,
    @(ApiModelProperty @field)(value="The name of the table", required=true)
    name: String,
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {

  def equalsIgnoreModifyTime( other: Table ) = this == other.copy( created=created, updated=updated )

  def setId( newId: String, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, created=if (forCreate) time; else created, updated=time)
    }

}

object Table {
  def create(id: Id.Table, name: String ) = new Table(id,name,0,0)
}
