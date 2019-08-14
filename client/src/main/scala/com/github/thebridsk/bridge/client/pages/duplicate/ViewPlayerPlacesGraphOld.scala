package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlace
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles.dupStyles
import com.github.thebridsk.bridge.clientcommon.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.utilities.logging.Logger

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPlayerPlacesGraphOld( ViewPlayerPlacesGraphOld.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerPlacesGraphOld {
  import ViewPlayerPlacesGraphOldInternal._

  case class Props( stats: Option[PlayerPlaces] )

  def apply( stats: Option[PlayerPlaces] ) = component(Props(stats))

}

object ViewPlayerPlacesGraphOldInternal {
  import ViewPlayerPlacesGraphOld._

  val log = Logger("bridge.ViewPlayerPlacesGraphOld")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State()

  val first = 120   // HSL green
  val last = -60      // HSL red

  /**
   *
   * @param place the place, zero based, 0 is first
   * @param teams the number of teams
   */
  def getColorForPlace( place: Int, teams: Int ) = {
    val h = (last-first)*place/(teams-1) + first
    if (h<0) h+360
    else h
  }

  val maxLightness = 75
  val minLightness = 25


  /**
   *
   * @param other the number of other teams tied with player
   * @param maxOther the max number of other teams tied with player
   */
  def getLightness( other: Int, maxOther: Int ) = {
    (maxLightness-minLightness)*other/maxOther + minLightness
  }

  //                                                        col  place,other
  val Row = ScalaComponent.builder[(Props,List[PlayerPlace],Int,(Int,Int)=>Color,Int,Int)]("ComponentBoard.TeamRow")
              .render_P( cprops => {
                val (props,playerlist,cols,getColor,minPlayed,maxPlayed) = cprops

                val emptyCols = (playerlist.length until cols).map( i => <.td() ).toTagMod

                <.tr(
                  playerlist.map { p =>

                  val (pslices,pcolors,psliceTitles) =
                    p.place.zipWithIndex.foldLeft((List[Double](),List[Color](),List[String]())) { (acc, en) =>
                      val (ll, place) = en

                      val (psl,pco,pst) = ll.zipWithIndex.foldLeft((List[Double](),List[Color](),List[String]())) { (ac, e) =>
                        val (n, oth) = e
                        val slice: Double = n
                        val color = getColor(place,oth)
                        val title = if (oth == 0) s"${n}: Place ${place+1}" else s"${n}: Place ${place+1} with ${oth} other teams"
                        (ac._1:::List(slice),ac._2:::List(color),ac._3:::List(title))
                      }

                      (acc._1:::psl,acc._2:::pco,acc._3:::pst)
                    }

                    val size: Double = p.total*200.0/maxPlayed
                    val slices: List[Double] = pslices.toList
                    val colors: List[Color] = pcolors.toList
                    val chartTitle: Option[String] = Some(p.name)
                    val sliceTitles: Option[List[String]] = Some(psliceTitles.toList)
                    val attrs: Option[TagMod] = None

                    log.fine(s"""----------------
                                |Name ${p.name}
                                |size ${size} ${p.total}
                                |slices ${slices.mkString(", ")}
                                |colors ${colors.mkString(", ")}
                                |sliceTitles ${sliceTitles.mkString(", ")}
                                |place ${p.place.map(l => l.mkString("[",",","]")).mkString("\n      ")}
                                |""".stripMargin)

                    <.td(
                      <.div(
                        <.p(p.name),
                        PieChart(
                          size,
                          slices,
                          colors,
                          chartTitle,
                          sliceTitles,
                          attrs
                        )
                      )
                    )
                  }.toTagMod,
                  emptyCols
                )
              }).build


  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {
    def render( props: Props, state: State ) = {
      props.stats match {
        case Some(pps) =>
          val maxTeams = pps.maxTeams
          val (maxOther,minPlayed,maxPlayed) = pps.players.foldLeft((0,99999,0)){ (acc,pp) =>
            val mo = pp.place.foldLeft(0){ (acc,o) =>
              Math.max(acc,o.length)
            }
            (Math.max(acc._1,mo), Math.min(acc._2,pp.total), Math.max(acc._3,pp.total))
          }

          def getColor( place: Int, other: Int ): Color = {
            val c = getColorForPlace(place,maxTeams)
            val l = getLightness(other,maxOther)
            Color.hsl(c,100,l)
          }

          val rowLength = 5

          val players = pps.players.grouped(rowLength)

          <.div(
            dupStyles.viewPlayerPlaces,

            <.table(
              <.tbody(
                players.map { p =>
                  Row(props,p,rowLength,getColor,minPlayed,maxPlayed)
                }.toTagMod
              )
            )
          )

        case None =>
          <.div(
            dupStyles.viewPlayerPlaces,
            <.h1("Loading...")
          )
      }
    }

    private var mounted = false

    val didMount = Callback {
      mounted = true

    }

    val willUnmount = Callback {
      mounted = false

    }
  }

  val component = ScalaComponent.builder[Props]("ViewPlayerPlacesGraphOld")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

