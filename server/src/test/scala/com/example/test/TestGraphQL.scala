package com.example.test

import org.scalatest.Finders
import org.scalatest.MustMatchers
import com.example.test.backend.BridgeServiceTesting
import com.example.service.MyService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.RemoteAddress.IP
import java.net.InetAddress
import _root_.utils.logging.Config
import _root_.utils.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import _root_.utils.logging.FileHandler
import _root_.utils.logging.FileFormatter
import java.util.logging.Level
import _root_.utils.logging.RedirectOutput
import sangria.macros._
import play.api.libs.json._
import com.example.service.graphql.Query
import akka.http.scaladsl.model.StatusCodes
import com.example.backend.FileImportStore
import scala.reflect.io.Directory
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.service.graphql.GraphQLRoute
import com.example.data.rest.JsonSupport._
import com.example.rest.UtilsPlayJson._

object TestGraphQL {

  val testlog = utils.logging.Logger[TestGraphQL]

  val graphQL = new Query

  val store = new BridgeServiceTesting {

    override
    val importStore = Some(new FileImportStore( Directory("""/tmp/store/import""" )))
  }

  val route = new GraphQLRoute {
    val restService = store
  }

  def queryToJson( query: String, variables: Option[Map[String,JsValue]]= None, operation: Option[String] = None ) = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> JsObject( v.toSeq ) ).toList :::
        "query" -> JsString(query) :: Nil
    )

  }

  def processError( resp: JsValue ) = {
    resp match {
      case _: JsObject =>
        resp \ "errors" match {
          case JsDefined( JsArray( messages ) ) =>
            println( messages.map { v =>
                       (v \ "message") match {
                         case JsDefined(JsString(msg)) => msg
                         case x => x.toString()
                       }
                     }.mkString("Errors:\n","\n","") )
          case JsDefined( _ ) =>
            testlog.warning(s"Expecting a messages, got ${resp}")
          case _: JsUndefined =>
            // no error
        }

      case _ =>
        testlog.warning(s"Expecting a JsObject, got ${resp}")
    }
  }
}

/**
 * Test class to start the logging system
 */
class TestGraphQL extends AsyncFlatSpec with ScalatestRouteTest with MustMatchers {
  import TestGraphQL._

  TestStartLogging.startLogging()

  behavior of "GraphQL Query"

  it should "return all the duplicate IDs in the store" in {

    val request = queryToJson(
       """
         query DuplicateIds {
           duplicateIds
         }
       """
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      statusCode mustBe StatusCodes.OK
      respjson mustBe JsObject( Seq( "data" -> JsObject( Seq( "duplicateIds" -> JsArray( Seq(JsString( "M1" )) )) )) )
    }

  }

  it should "return all the duplicate IDs in the store again" in {

    val request = queryToJson(
       """
         query DuplicateIdsAgain {
           duplicateIds
         }
       """
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      statusCode mustBe StatusCodes.OK
      respjson mustBe JsObject( Seq( "data" -> JsObject( Seq( "duplicateIds" -> JsArray( Seq(JsString( "M1" )) )) )) )
    }

  }

  it should "return all the teams from duplicate match M1" in {

    val vars = Map("mdid" -> JsString("M1"))
    val request = queryToJson(
       """
         query DuplicateTeams($mdid: DuplicateId!) {
           duplicate(id: $mdid) {
             id,
             teams {
               id,
               player1,
               player2
             }
           }
         }
       """,
       Some(vars),
//       Some("SecondQuery")
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      testlog.warning( s"Response was ${statusCode} ${respjson}" )
      statusCode mustBe StatusCodes.OK
      respjson \ "data" \ "duplicate" \ "teams" \ 0 \ "player1" mustBe JsDefined(JsString("Nancy"))
    }

  }

  it should "return an error" in {

    val vars = Map("mdid" -> JsString("M1"))
    val request = queryToJson(
       """
         query Error($mdid: TeamId!) {
           duplicate(id: $mdid) {
             id,
             teams {
               id,
               player1,
               player2
             }
           }
         }
       """,
       Some(vars),
//       Some("SecondQuery")
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      testlog.warning( s"Response was ${statusCode} ${respjson}" )
      statusCode mustBe StatusCodes.BadRequest
      respjson \ "errors" \ 0 \ "message" match {
        case JsDefined( JsString(v) ) =>
          v must include regex "'TeamId!'"
        case x: JsDefined =>
          fail( s"Expecting a JsDefined( JsString( msg )), got $x" )
        case x: JsUndefined =>
          fail( s"Expecting error, got ${x.error}" )
      }
    }

  }

  it should "return all the ids of import stores" in {

    val request = queryToJson(
       """
         query ImportIds {
           importIds
         }
       """,
       None,
//       Some("SecondQuery")
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      testlog.warning( s"Response was ${statusCode} ${respjson}" )
      statusCode mustBe StatusCodes.OK
      respjson \ "data" \ "importIds" mustBe JsDefined(JsArray(Seq(JsString("import.zip"))))
    }

  }

  it should "return all the teams from duplicate match M1 from the import store" in {

    val vars = Map("mdid" -> JsString("M1"), "iid" -> JsString("import.zip"))
    val request = queryToJson(
       """
         query ImputDuplicateTeams($mdid: DuplicateId!, $iid: ImportId!) {
           import(id: $iid) {
             duplicate(id: $mdid) {
               id,
               teams {
                 id,
                 player1,
                 player2
               }
             }
           }
         }
       """,
       Some(vars),
//       Some("SecondQuery")
    )

    val response = graphQL.query(request, store)

    response.map { resp =>
      val (statusCode, respjson) = resp
      processError(respjson)
      testlog.warning( s"Response was ${statusCode} ${respjson}" )
      statusCode mustBe StatusCodes.OK
      respjson \ "data" \ "import" \ "duplicate" \ "teams" \ 0 \ "player1" mustBe JsDefined(JsString("Bill"))
    }

  }


  val remoteAddress = `Remote-Address`( IP( InetAddress.getLocalHost, Some(12345) ))

  it should "make a graphql query" in {
    val vars = Map("mdid" -> JsString("M1"), "iid" -> JsString("import.zip"))
    val request = queryToJson(
       """
         query ImputDuplicateTeams($mdid: DuplicateId!, $iid: ImportId!) {
           import(id: $iid) {
             duplicate(id: $mdid) {
               id,
               teams {
                 id,
                 player1,
                 player2
               }
             }
           }
         }
       """,
       Some(vars),
//       Some("SecondQuery")
    )

    Post("/graphql", request) ~> addHeader(remoteAddress) ~> route.graphQLRoute ~> check {
      status mustBe OK
      val respjson = responseAs[JsObject]
      respjson \ "data" \ "import" \ "duplicate" \ "teams" \ 0 \ "player1" mustBe JsDefined(JsString("Bill"))
    }
  }

}
