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
import com.github.thebridsk.bridge.clientcommon.react.reactwidgets.DateTimePicker
import japgolly.scalajs.react.component.Js.Unmounted
import japgolly.scalajs.react.raw.React.Element


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

  // val component = componentFn

  def apply( graphqlUrl: String ) = {

//    logger.info("GraphiQL: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = GraphiQLComponentProperty(graphqlUrl)

    // val c = component

    // showObject( c.asInstanceOf[js.Object], "GraphiQL.component" )

    // c(props)

    // val ce = React.raw.createElement[GraphiQLComponentProperty]( Raw.GraphiQL.asInstanceOf[japgolly.scalajs.react.raw.React.ComponentType[GraphiQLComponentProperty]], props )

    // logger.fine(s"GraphiQL.apply: ce = $ce")

    // ce.asInstanceOf[Unmounted[Null,Null]]

    val x = Raw.GraphiQL

    // see https://github.com/japgolly/scalajs-react/blob/master/test/src/test/scala/japgolly/scalajs/react/core/JsComponentEs6Test.scala
    val RawComp = js.eval("window.GraphiQL")
    val component = JsComponent[GraphiQLComponentProperty, Children.None, Null](RawComp)
    component(props)
  }

  object Raw {
    @JSImport("graphiql", JSImport.Namespace ) // "GraphiQL")
    @js.native
    object GraphiQL extends js.Object // GraphiQL
  }

//   def componentFn() = {
//     logger.fine(s"RawGraphiQL = ${Raw.GraphiQL}")
//     showObject( Raw.GraphiQL, "RawGraphiQL")
//     logger.fine(s"DateTimePicker = ${DateTimePicker}")

// //    JsFnComponent[GraphiQLComponentProperty, Children.None](RawGraphiQL)
//     JsComponent[GraphiQLComponentProperty, Children.None, Null](Raw.GraphiQL)
//   }

  def showObject( c: js.Object, msg: String ) = {
    if (logger.isFineLoggable()) {
      logger.fine( s"Dumping ${msg}, ${c}")
      val o = c.asInstanceOf[js.Dynamic]
      js.Object.keys(o.asInstanceOf[js.Object]).foreach { k =>
        val v = o.selectDynamic(k)
        val s = js.typeOf(v) match {
          case "function" => "function"
          case _ => v.toString
        }
        logger.fine(s"  $k: $s")
      }
    }

  }
}
