package graphiql

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSImport
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLBaseClient
import com.github.thebridsk.bridge.clientcommon.graphql.Query
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import scala.scalajs.js.JSON
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import graphqlvoyager.VoyagerComponentProperty


@js.native
trait GraphiQLComponentProperty extends js.Object {
  val fetcher: js.Function1[String, Promise[js.Object]] = js.native
}

object GraphiQLComponentProperty {

  def intro( graphqlUrl: String)( query: js.Object): Promise[js.Object] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val gql = new GraphQLBaseClient(graphqlUrl)
    val r = gql.requestWithBody(query)
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

  def apply( graphqlUrl: String ): GraphiQLComponentProperty = {
    val p = js.Dynamic.literal()

    val i = intro(graphqlUrl) _

    p.updateDynamic("fetcher")( i)

    p.asInstanceOf[GraphiQLComponentProperty]
  }
}

@js.native
trait GraphiQL extends js.Object

object GraphiQL {
  val logger = Logger("bridge.GraphiQL")

//  val component = ScalaComponent.builder[GraphiQLComponentProperty]("bridge.GraphiQL")
//  .stateless
//  .render_P( props => {
//    raw.React.createElement(GraphiQL, props )
//  }).build


//  @JSGlobal("ReactWidgets.GraphiQL")
//  @js.native
//  object ReactWidgetsGraphiQL extends js.Object

  val component = JsComponent[GraphiQLComponentProperty, Children.None, Null](GraphiQL)

  def apply( graphqlUrl: String ) = {

    val x = GraphiQL

//    logger.info("GraphiQL: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = GraphiQLComponentProperty(graphqlUrl)

    component(props)
  }

  @JSImport("graphiql", JSImport.Namespace ) // "GraphiQL")
  @js.native
  object GraphiQL extends GraphiQL // GraphiQL

}
