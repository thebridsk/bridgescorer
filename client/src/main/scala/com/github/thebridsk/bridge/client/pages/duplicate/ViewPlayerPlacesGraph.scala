package com.github.thebridsk.bridge.client.pages.duplicate

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlace
import com.github.thebridsk.bridge.client.pages.duplicate.DuplicateStyles.dupStyles
import com.github.thebridsk.bridge.clientcommon.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil


/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * ViewPlayerPlacesGraph( ViewPlayerPlacesGraph.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ViewPlayerPlacesGraph {
  import ViewPlayerPlacesGraphInternal._

  case class Props( stats: Option[PlayerPlaces] )

  def apply( stats: Option[PlayerPlaces] ) = component(Props(stats))

}

object ViewPlayerPlacesGraphInternal {
  import ViewPlayerPlacesGraph._

  val log = Logger("bridge.ViewPlayerPlacesGraph")

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
    (maxLightness-minLightness)*other/(maxOther-1) + minLightness
  }

  val sPlaces = "First"::"Second"::"Third"::"Fourth"::Nil
  /**
   * @param place the place, zero based.  0 = first
   */
  def getPlaceString( place: Int ) = sPlaces.drop(place).headOption.getOrElse(s"${place+1} place")

  //                                                        cols                         getSize
  val Row = ScalaComponent.builder[(Props,List[PlayerPlace],Int,IntLegendUtil[(Int,Int)],Int=>Int)]("ComponentBoard.TeamRow")
              .render_P( cprops => {
                val (props,playerlist,cols,legendUtil,getSize) = cprops

                val emptyCols = (playerlist.length until cols).map( i => <.td() ).toTagMod

                <.tr(
                  playerlist.map { p =>

                    val histogram = p.place.zipWithIndex.flatMap { entry =>
                      val (list,place) = entry

                      val others = list.zipWithIndex.flatMap { entry2 =>
                        val (v, other) = entry2
                        if (v == 0) Nil
                        else ((place,other), v)::Nil
                      }
                      if (others.isEmpty) Nil
                      else {
                        (getPlaceString(place),others)::Nil
                      }
                    }

                    val size = getSize(p.total)

                    <.td(
                      <.div(
                        <.p(p.name),
                        PieChartWithTooltip(
                          histogram = histogram,
                          title=Some(p.name),
                          legendtitle=Left(true),
                          util=legendUtil,
                          size=size,
                          sizeInLegend = tooltipPieChartSize,
                          minSize = divMinSize
                        )
                      )
                    )
                  }.toTagMod,
                  emptyCols
                )
              }).build

  //
  val Legend = ScalaComponent.builder[(Props,IntLegendUtil[(Int,Int)],Int,Int)]("ComponentBoard.TeamRow")
              .render_P( cprops => {
                val (props,legendUtil,maxTeams,maxOthers) = cprops

                log.fine(s"maxTeams=${maxTeams}, maxOthers=${maxOthers}")

                <.table(
                  <.thead(
                    <.tr(
                      <.th( ^.rowSpan:=2, "Place" ),
                      <.th( ^.colSpan:=(maxOthers), "Tied With")
                    ),
                    <.tr(
                      ((0 until maxOthers).map { i =>
                        <.th( i.toString )
                      }).toTagMod
                    )
                  ),
                  <.tbody(
                    ((0 until maxTeams).map { p =>
                      val maxO = Math.min( maxTeams-p, maxOthers)
                      <.tr(
                        <.th(getPlaceString(p)),
                        ((0 until maxOthers).map { o =>
                          <.td(
                            if (o < maxO) {
                              PieChart(
                                20,
                                1::Nil,
                                legendUtil.colorMap((p,o))::Nil
                              )
                            } else {
                              ""
                            }
                          )
                        }).toTagMod
                      )
                    }).toTagMod
                  )
                )

              }).build

  val pieChartMaxSize = 200
  val pieChartMinSize = 5
  val tooltipPieChartSize = 300
  val divMinSize = 20

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

          def calcSize( played: Int ) = {
            ((played.toDouble-minPlayed)/(maxPlayed-minPlayed)*(pieChartMaxSize-pieChartMinSize)).toInt +
                pieChartMinSize
          }

          val legendUtil = new IntLegendUtil[(Int,Int)] {
            def nameToTitle( name: (Int,Int) ): String = {
              val (place,other) = name
              getPlaceString(place) + (if (other == 0) "" else s" with ${other} other")
            }
            def colorMap( name: (Int,Int) ): Color = {
              val (place,other) = name
              val c = getColorForPlace(place,maxTeams)
              val l = getLightness(other,maxOther)
              Color.hsl(c,100,l)
            }
          }

          val rowLength = 5

          val footerRightLength: Int = rowLength/2
          val footerLeftLength = rowLength - footerRightLength

          val players = pps.players.grouped(rowLength)

          <.div(
            dupStyles.viewPlayerPlaces,

            <.table(
              <.caption("Player places"),
              <.tfoot(
                <.tr(
                  <.td(
                    ^.colSpan := footerLeftLength,
                    "Shows how many times a player came in first, second, ...",
                    <.br,
                    "Also shows if they were tied with other teams.",
                    <.br,
                    " The size of the pie chart is relative to the number of matches the player played.",
                    <.br,
                    " The slice size is the percentage of times the player came in that place."
                  ),
                  <.td(
                    ^.colSpan := footerRightLength,
                    <.div(
                      "Legend",
                      Legend((props,legendUtil,maxTeams,maxOther))
                    )
                  )
                )
              ),
              <.tbody(
                players.map { p =>
                  Row((props,p,rowLength,legendUtil,calcSize))
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

  val component = ScalaComponent.builder[Props]("ViewPlayerPlacesGraph")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .componentDidMount( scope => scope.backend.didMount)
                            .componentWillUnmount( scope => scope.backend.willUnmount )
                            .build
}

