package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.server.backend.BridgeService

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.bridge.PlayerPosition

import SchemaBase.{log => _, _}

object SchemaHand {

  val log: Logger = Logger(SchemaHand.getClass.getName)

  val HandType: ObjectType[BridgeService, Hand] = ObjectType(
    "Hand",
    "Result of a bridge hand",
    fields[BridgeService, Hand](
      Field("id", StringType, Some("The id of the hand"), resolve = _.value.id),
      Field(
        "contractTricks",
        IntType,
        Some("The number of tricks in the contract."),
        resolve = _.value.contractTricks
      ),
      Field(
        "contractSuit",
        StringType,
        Some("The suit in the contract."),
        resolve = _.value.contractSuit
      ),
      Field(
        "contractDoubled",
        StringType,
        Some("The doubling of the contract."),
        resolve = _.value.contractDoubled
      ),
      Field(
        "declarer",
        PositionEnum,
        Some("The declarer of the contract."),
        resolve = ctx => PlayerPosition(ctx.value.declarer)
      ),
      Field(
        "nsVul",
        BooleanType,
        Some("The vulnerability of NS for the contract."),
        resolve = _.value.nsVul
      ),
      Field(
        "ewVul",
        BooleanType,
        Some("The vulnerability of EW for the contract."),
        resolve = _.value.ewVul
      ),
      Field(
        "madeContract",
        BooleanType,
        Some("Whether the contract was made or not."),
        resolve = _.value.madeContract
      ),
      Field(
        "tricks",
        IntType,
        Some("The number of tricks made or down in the contract."),
        resolve = _.value.tricks
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.updated
      )
    )
  )

}
