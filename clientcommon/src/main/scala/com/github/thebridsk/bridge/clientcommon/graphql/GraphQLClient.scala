package com.github.thebridsk.bridge.clientcommon.graphql

import scala.concurrent.ExecutionContext.Implicits.global

object GraphQLClient extends GraphQLBaseClient("/v1/graphql") {}
