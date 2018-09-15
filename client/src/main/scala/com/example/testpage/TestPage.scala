package com.example.testpage

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.example.routes.AppRouter.AppPage
import com.example.pages.BaseStyles
import utils.logging.Logger
import com.example.react.AppButton
import com.example.routes.BridgeRouter
import com.example.rest2.RestClientDuplicateSummary
import com.example.rest2.AjaxResult
import scala.concurrent.ExecutionContext.Implicits.global
import com.example.logger.Alerter
import com.example.data.DuplicateSummary
import com.example.react.PopupOkCancel
import com.example.data.rest.JsonSupport
import play.api.libs.json._
import com.example.data.rest.JsonException

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * TestPage( TestPage.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object TestPage {
  import TestPageInternal._

  case class Props( returnpage: AppPage, router: BridgeRouter[AppPage] )

  def apply( returnpage: AppPage, router: BridgeRouter[AppPage] ) = component(Props(returnpage,router))

}

object TestPageInternal {
  import TestPage._

  val log = Logger("bridge.TestPage")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( errormsg: Option[String], summaryJsonString: Option[String], summary: Option[List[DuplicateSummary]] ) {
    def this() = this( None, None, None )
    def clear = new State()
  }

  object State {
    def apply() = new State()
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    val popupOk = scope.modState{ s=> s.copy( errormsg = None ) }

    def showError( msg: String ) = scope.modState{ s=> s.copy( errormsg = Some(msg) ) }

    def render( props: Props, state: State ) = {
      import BaseStyles._
      <.div(
        baseStyles.testPage,
        PopupOkCancel( state.errormsg.map(msg => <.div(<.pre(<.code(msg)))), Some(popupOk), None),
        <.h1("Test Page"),
        <.p("Using play-json"),

        state.summaryJsonString.whenDefined { json =>
          <.div(
            <.h2( "Duplicate Summary JSON" ),
            <.p( json )
          )
        },
        state.summary.whenDefined { summary =>
          <.div(
            <.h2( "Duplicate Summary" ),
            <.ol(
              summary.map{ s =>
                <.li(s.toString())
              }.toTagMod
            )
          )
        },

        <.div(
          baseStyles.divFooter,
          <.div(
            baseStyles.divFooterLeft,
            AppButton( "Home", "Home", props.router.setOnClick(props.returnpage) ),
            AppButton( "Clear", "Clear", ^.onClick --> clear )
          ),
          <.div(
            baseStyles.divFooterCenter,
            AppButton( "SummaryJson", "Summary Json", ^.onClick --> clickSummaryJson ),
            AppButton( "Convert", "Convert", ^.onClick --> clickConvert )
          ),
          <.div(
            baseStyles.divFooterRight,
            " "
          )
        )
      )
    }

    val clear = scope.modState( s => s.clear )

    val clickSummaryJson = Callback {
      val url = RestClientDuplicateSummary.getURL()

      AjaxResult.get(url).recordFailure().foreach{ summary =>
        scope.withEffectsImpure.modState{ s =>
          Alerter.tryitWithDefault(s) {
            val txt = summary.responseText
            log.info(s"""TestPage: received the following JSON for the duplicate summary:\n${txt}""")
            s.copy( summaryJsonString = Some(txt) )
          }
        }
      }
    }

    val clickConvert = scope.modState { s =>
      import JsonSupport._

      if (s.summaryJsonString.isDefined) {
        try {
//          val r = read[List[DuplicateSummary]]( s.summaryJsonString.get )
//          s.copy(summary = Some(r))
          val json = Json.parse(s.summaryJsonString.get)
          try {
            Json.fromJson[List[DuplicateSummary]](json) match {
              case JsSuccess(r, path: JsPath) =>
                s.copy(summary = Some(r))
              case x: JsError =>
                val e = JsError.toJson(x).toString()
                Alerter.alert(s"""TestPage: JsError converting duplicate summary to scala objects, ${e}""")
                log.warning(s"""TestPage: JsError converting duplicate summary to scala objects, ${e}""")
                s.copy(errormsg = Some(e) )
            }
          } catch {
            case x: Throwable =>
              val e = Alerter.exceptionToString(x)
              Alerter.alert(s"""TestPage: error converting duplicate summary to scala objects, ${e}""")
              log.warning(s"""TestPage: error converting duplicate summary to scala objects, ${e}""")
              s.copy(errormsg = Some(e) )
          }
        } catch {
          case x: Throwable =>
            val e = Alerter.exceptionToString(x)
            Alerter.alert(s"""TestPage: error converting duplicate summary json to JsValue, ${e}""")
            log.warning(s"""TestPage: error converting duplicate summary json to JsValue, ${e}""")
            s.copy(errormsg = Some(e) )
        }
      } else {
        s.copy(errormsg = Some("Must get the JSON string first"))
      }
    }

    private var mounted: Boolean = false

    import scala.concurrent.ExecutionContext.Implicits.global
    val didMount = CallbackTo {
      mounted = true
      log.finer("TestPage.didMount")
      // make AJAX rest call here

    }

    val willUnmount = CallbackTo {
      mounted = false
      log.finer("TestPage.willUnmount")
    }

  }

  val component = ScalaComponent.builder[Props]("TestPage")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount(scope => scope.backend.willUnmount)
                            .build
}

