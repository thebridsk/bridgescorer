package graphiql

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
import graphqlvoyager.VoyagerComponentProperty


@js.native
trait GraphiQLComponentProperty extends js.Object {
  val fetcher: js.Function1[String, Promise[js.Object]] = js.native
}

case class GraphQLQuery( query: String)

object GraphiQLComponentProperty {

  def apply( graphqlUrl: String ): GraphiQLComponentProperty = {
    val p = js.Dynamic.literal()

    val i = VoyagerComponentProperty.intro(graphqlUrl) _

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

  @JSImport("graphiql", "GraphiQL")
  @js.native
  object GraphiQL extends GraphiQL // GraphiQL

}
