package graphqlvoyager

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import japgolly.scalajs.react._
import utils.logging.Logger
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSImport
import com.example.graphql.GraphQLBaseClient
import com.example.graphql.Query
import com.example.rest2.AjaxResult
import com.example.data.graphql.GraphQLProtocol.GraphQLResponse
import scala.scalajs.js.JSON
import play.api.libs.json.JsValue
import play.api.libs.json.Json


@js.native
trait VoyagerComponentProperty extends js.Object {
  val introspection: js.Function1[String, Promise[js.Object]] = js.native
}

object VoyagerComponentProperty {

  def intro( graphqlUrl: String)( query: String): Promise[js.Object] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val gql = new GraphQLBaseClient(graphqlUrl)
    val q = new Query[String](query,gql)
    val r: AjaxResult[GraphQLResponse] = q.execute( None )
    import js.JSConverters._
    val x = r.recordFailure().map { resp =>

      val pr = js.Dynamic.literal()
      resp.data.foreach{ d =>
        val dd = JSON.parse(d.toString(), (k,v) => v )
        pr.updateDynamic("data")( dd )
      }
      pr
    }.toJSPromise
    x
  }

  def apply( graphqlUrl: String ): VoyagerComponentProperty = {
    val p = js.Dynamic.literal()

    val i = intro(graphqlUrl) _

    p.updateDynamic("introspection")( i)
    p.updateDynamic("workerURI")("graphql-voyager/dist/voyager.worker.js")

    p.asInstanceOf[VoyagerComponentProperty]
  }
}

@js.native
trait Voyager extends js.Object

object Voyager {
  val logger = Logger("bridge.Voyager")

//  @JSGlobal("ReactWidgets.Voyager")
//  @js.native
//  object ReactWidgetsVoyager extends js.Object

  val component = JsComponent[VoyagerComponentProperty, Children.None, Null](JSVoyager)

  def apply( graphqlUrl: String ) = {

//    logger.info("Voyager: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = VoyagerComponentProperty(graphqlUrl)

    component(props)
  }

  @JSImport("graphql-voyager", "Voyager")
  @js.native
  object JSVoyager extends Voyager

}
