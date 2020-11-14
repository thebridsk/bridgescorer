package graphqlvoyager

import scala.scalajs.js
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSImport
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLBaseClient
import com.github.thebridsk.bridge.clientcommon.graphql.Query
import com.github.thebridsk.bridge.clientcommon.rest2.AjaxResult
import com.github.thebridsk.bridge.data.graphql.GraphQLProtocol.GraphQLResponse
import scala.scalajs.js.JSON

@js.native
trait VoyagerComponentProperty extends js.Object {
  val introspection: js.Function1[String, Promise[js.Object]] = js.native
}

object VoyagerComponentProperty {

  def intro(graphqlUrl: String)(query: String): Promise[js.Object] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val gql = new GraphQLBaseClient(graphqlUrl)
    val q = new Query[String](query, gql)
    val r: AjaxResult[GraphQLResponse] = q.execute(None)
    import js.JSConverters._
    val x = r
      .recordFailure()
      .map { resp =>
        val pr = js.Dynamic.literal()
        resp.data.foreach { d =>
          val dd = JSON.parse(d.toString(), (k, v) => v)
          pr.updateDynamic("data")(dd)
        }
        pr
      }
      .toJSPromise
    x
  }

  def apply(graphqlUrl: String): VoyagerComponentProperty = {
    val p = js.Dynamic.literal()

    val i = intro(graphqlUrl) _

    p.updateDynamic("introspection")(i)
    p.updateDynamic("workerURI")("graphql-voyager/dist/voyager.worker.js")

    p.asInstanceOf[VoyagerComponentProperty]
  }
}

@js.native
trait Voyager extends js.Object

object Voyager {
  val logger: Logger = Logger("bridge.Voyager")

//  @JSGlobal("ReactWidgets.Voyager")
//  @js.native
//  object ReactWidgetsVoyager extends js.Object

  val component = JsComponent[VoyagerComponentProperty, Children.None, Null](
    JSVoyager
  ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def apply(graphqlUrl: String) = { // scalafix:ok ExplicitResultTypes; ReactComponent

//    logger.info("Voyager: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = VoyagerComponentProperty(graphqlUrl)

    component(props)
  }

  @JSImport("graphql-voyager", "Voyager")
  @js.native
  object JSVoyager extends Voyager

}
