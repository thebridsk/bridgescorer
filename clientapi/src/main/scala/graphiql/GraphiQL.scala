package graphiql

import scala.scalajs.js
import japgolly.scalajs.react._
import com.github.thebridsk.utilities.logging.Logger
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSImport
import com.github.thebridsk.bridge.clientcommon.graphql.GraphQLBaseClient
import scala.scalajs.js.JSON

@js.native
trait GraphiQLComponentProperty extends js.Object {
  val fetcher: js.Function1[String, Promise[js.Object]] = js.native
}

object GraphiQLComponentProperty {

  def intro(graphqlUrl: String)(query: js.Object): Promise[js.Object] = {
    import com.github.thebridsk.bridge.clientcommon.BridgeExecutionContext.global
    val gql = new GraphQLBaseClient(graphqlUrl)
    val r = gql.requestWithBody(query)
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

  def apply(graphqlUrl: String): GraphiQLComponentProperty = {
    val p = js.Dynamic.literal()

    val i = intro(graphqlUrl) _

    p.updateDynamic("fetcher")(i)

    p.asInstanceOf[GraphiQLComponentProperty]
  }
}

object GraphiQL {
  val logger: Logger = Logger("bridge.GraphiQL")

  val component = JsComponent[GraphiQLComponentProperty, Children.None, Null](
    RawGraphiQL
  ) // scalafix:ok ExplicitResultTypes; ReactComponent

  def apply(graphqlUrl: String) = { // scalafix:ok ExplicitResultTypes; ReactComponent

//    logger.info("GraphiQL: msgEmptyList="+msgEmptyList+", msgEmptyFilter="+msgEmptyFilter)

    val props = GraphiQLComponentProperty(graphqlUrl)

    component(props)
  }

  @js.native
  @JSImport("graphiql", JSImport.Default) // "GraphiQL")
  object RawGraphiQL extends js.Any

  def showAny(c: js.Any, msg: String): Unit = {
    js.typeOf(c) match {
      case "object"   => showObject(c.asInstanceOf[js.Object], msg)
      case "function" => logger.fine(s"${msg} is a function")
      case _          => logger.fine(s"${msg} is ${c.toString()}")
    }
  }

  def showObject(c: js.Object, msg: String): Unit = {
    if (logger.isFineLoggable()) {
      logger.fine(s"Dumping ${msg}, ${c}")
      val o = c.asInstanceOf[js.Dynamic]
      js.Object.keys(o.asInstanceOf[js.Object]).foreach { k =>
        val v = o.selectDynamic(k)
        val s = js.typeOf(v) match {
          case "function" => "function"
          case _          => v.toString
        }
        logger.fine(s"  $k: $s")
      }
    }

  }
}
