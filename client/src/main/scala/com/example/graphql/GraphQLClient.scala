package com.example.graphql

import scala.concurrent.ExecutionContext.Implicits.global

object GraphQLClient extends GraphQLRequest("/v1/graphql") {

}
