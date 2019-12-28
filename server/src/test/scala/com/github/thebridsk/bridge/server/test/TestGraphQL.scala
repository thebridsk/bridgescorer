package com.github.thebridsk.bridge.server.test

import org.scalatest.Finders
import org.scalatest.matchers.must.Matchers
import com.github.thebridsk.bridge.server.test.backend.BridgeServiceTesting
import com.github.thebridsk.bridge.server.service.MyService
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
import com.github.thebridsk.utilities.logging.Config
import com.github.thebridsk.utilities.classpath.ClassPath
import java.util.logging.LogManager
import java.util.logging.Logger
import com.github.thebridsk.utilities.logging.FileHandler
import com.github.thebridsk.utilities.logging.FileFormatter
import java.util.logging.Level
import com.github.thebridsk.utilities.logging.RedirectOutput
import sangria.macros._
import play.api.libs.json._
import com.github.thebridsk.bridge.server.service.graphql.Query
import akka.http.scaladsl.model.StatusCodes
import com.github.thebridsk.bridge.server.backend.FileImportStore
import scala.reflect.io.Directory
import com.github.thebridsk.bridge.server.service.graphql.GraphQLRoute
import com.github.thebridsk.bridge.data.rest.JsonSupport._
import com.github.thebridsk.bridge.server.rest.UtilsPlayJson._
import com.github.thebridsk.bridge.server.test.backend.ImportStoreTesting
import java.io.FileOutputStream
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Using
import scala.concurrent.Future
import org.scalatest.flatspec.AsyncFlatSpec
import scala.concurrent.ExecutionContext

object TestGraphQL {

  val testlog = com.github.thebridsk.utilities.logging.Logger[TestGraphQL]

  val graphQL = new Query

  val dirTemp = Directory.makeTemp()
  val dirImport = dirTemp / "store"

  def store( implicit ec: ExecutionContext ) = new BridgeServiceTesting {

    override
    val importStore = Some(new FileImportStore( dirImport.toDirectory ))

  }

  def route( implicit ec: ExecutionContext ) = new GraphQLRoute {
    val restService = store
  }

  def queryToJson( query: String, variables: Option[Map[String,JsValue]]= None, operation: Option[String] = None ) = {
    JsObject(
        operation.map( v => "operationName" -> JsString(v) ).toList :::
        variables.map( v => "variables" -> JsObject( v.toSeq ) ).toList :::
        "query" -> JsString(query) :: Nil
    )

  }

  def processError( resp: JsValue, comment: String = "Errors" ) = {
    resp match {
      case obj: JsObject =>
        obj \ "errors" match {
          case JsDefined( JsArray( messages ) ) =>
            println( messages.map { v =>
                       (v \ "message") match {
                         case JsDefined(JsString(msg)) => msg
                         case x => x.toString()
                       }
                     }.mkString(s"${comment}:\n","\n","") )
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
class TestGraphQL extends AsyncFlatSpec with ScalatestRouteTest with Matchers {
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
      processError(respjson, "Error response, this is not a test failure, this is expected")
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

  it should "export and import the store" in {

    val importfile = (dirTemp / "import.zip")
    val r = Using.resource( new FileOutputStream( importfile.toString() ) ) { file =>
      val fut = store.export(file)
      Await.result(fut, 60 seconds)
    }

    r mustBe Right( List( "M1" ) )

    importfile.isFile mustBe true

    store.importStore.map { is =>
      is.create("import.zip", importfile.toFile).map { rbs =>
        rbs match {
          case Right(bs) =>
            (dirImport / "import.zip").isFile mustBe true
          case Left((statusCode,msg)) =>
            fail(s"Error importing store: ${statusCode} ${msg.msg}" )
        }
      }
    }.getOrElse( fail("Did not find import store" ))
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
      respjson \ "data" \ "import" \ "duplicate" \ "teams" \ 0 \ "player1" mustBe JsDefined(JsString("Nancy"))
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

    Post("/v1/graphql", request) ~> addHeader(remoteAddress) ~> route.graphQLRoute ~> check {
      status mustBe OK
      val respjson = responseAs[JsObject]
      respjson \ "data" \ "import" \ "duplicate" \ "teams" \ 0 \ "player1" mustBe JsDefined(JsString("Nancy"))
    }
  }

}
