package com.github.thebridsk.bridge.server.service.graphql

import sangria.schema._

import com.github.thebridsk.utilities.logging.Logger

import com.github.thebridsk.bridge.server.service.graphql.schema.SchemaQuery.{log => _, _}
import com.github.thebridsk.bridge.server.service.graphql.schema.SchemaMutation.{log => _, _}

object SchemaDefinition {

  val log = Logger(SchemaDefinition.getClass.getName)

  val BridgeScorerSchema = Schema(QueryType, Some(MutationType))
}
