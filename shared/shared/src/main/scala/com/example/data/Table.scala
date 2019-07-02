package com.example.data

import com.example.data.SystemTime.Timestamp

import scala.annotation.meta._
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A table for a duplicate match")
case class Table(
    @Schema(description = "The ID of the table", required = true)
    id: String,
    @Schema(description = "The name of the table", required = true)
    name: String,
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

  def equalsIgnoreModifyTime(other: Table) =
    this == other.copy(created = created, updated = updated)

  def setId(newId: String, forCreate: Boolean) = {
    val time = SystemTime.currentTimeMillis()
    copy(
      id = newId,
      created = if (forCreate) time; else created,
      updated = time
    )
  }

}

object Table {
  def create(id: Id.Table, name: String) = new Table(id, name, 0, 0)
}
