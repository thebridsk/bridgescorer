package com.github.thebridsk.bridge.client.pages.hand

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.data.bridge.ContractTricks
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.MyEnumeration
import com.github.thebridsk.bridge.client.pages.hand.ComponentInputStyleButton.InputMethod
import com.github.thebridsk.utilities.logging.Level
import com.github.thebridsk.bridge.data.maneuvers.TableManeuvers
import com.github.thebridsk.bridge.clientcommon.react.AppButton
import com.github.thebridsk.bridge.clientcommon.react.Button
import com.github.thebridsk.bridge.client.pages.Pixels
import com.github.thebridsk.bridge.clientcommon.react.HelpButton
import com.github.thebridsk.materialui.icons.Camera
import org.scalajs.dom.FileList
import com.github.thebridsk.bridge.clientcommon.react.Utils._
import com.github.thebridsk.materialui.icons.Photo
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import com.github.thebridsk.bridge.clientcommon.react.PopupOkCancel
import org.scalajs.dom.File
import com.github.thebridsk.materialui.icons.DeleteForever
import com.github.thebridsk.bridge.data.Team

/**
  * A skeleton component.
  *
  * To use, just code the following:
  *
  * <pre><code>
  * Component( Component.Props( ... ) )
  * </code></pre>
  *
  * @author werewolf
  */
object PageHand {
  import PageHandInternal._

  //                          picture      removePicture
  type CallbackOk = (Contract, Option[File], Boolean) => Callback
  //                                  picture      removePicture  honorPoints  honorPosition
  type CallbackWithHonors =
    (Contract, Option[File], Boolean, Int, Option[PlayerPosition]) => Callback
  type CallbackCancel = Callback

  /**
    * Constructor for showing and allowing modification of a contract.
    * @param contract the contract to view and possibly change
    * @param callbackOk A callback of type CallbackOk.  The callback takes
    * one argument, a Contract object, and returns a Callback object.
    * @param callbackCancel A callback called when cancel is hit.
    * @param teamNS
    * @param teamEW
    * @param newhand true if this is a new hand to score,
    *                false if contract shows old hand
    * @param allowPassedOut
    * @param callbackWithHonors if specified, this will be called instead of callbackOk
    */
  def apply(
      contract: Contract,
      callbackOk: CallbackOk,
      callbackCancel: CallbackCancel,
      teamNS: Option[Team.Id] = None,
      teamEW: Option[Team.Id] = None,
      newhand: Boolean = false,
      allowPassedOut: Boolean = true,
      callbackWithHonors: Option[CallbackWithHonors] = None,
      honors: Option[Int] = None,
      honorsPlayer: Option[PlayerPosition] = None,
      helppage: Option[String] = None,
      picture: Option[String] = None,
      supportPicture: Boolean = false,
      playingDuplicate: Boolean = false
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    logger.fine(s"PageHand.apply(): contract=$contract")
    component(
      Props(
        contract.withScoring,
        callbackOk,
        callbackCancel,
        teamNS,
        teamEW,
        newhand,
        allowPassedOut,
        callbackWithHonors,
        honors,
        honorsPlayer,
        helppage,
        picture,
        supportPicture,
        playingDuplicate
      )
    )
  }

  /**
    * Constructor for entering a new hand
    * @param scoringSystem The scoring system that should be used, Values are: Duplicate, Chicago, Rubber
    * @param table the table in duplicate.  0 should be used when not duplicate
    * @param board the board in duplicate.  0 should be used when not duplicate
    * @param north the north player's name
    * @param south the south player's name
    * @param east the east player's name
    * @param west the west player's name
    * @param callbackOk A callback of type CallbackOk.  The callback takes
    * one argument, a Contract object, and returns a Callback object.
    * @param callbackCancel A callback called when cancel is hit.
    * @param teamNS
    * @param teamEW
    * @param newhand true if this is a new hand to score,
    *                false if contract shows old hand
    * @param allowPassedOut
    * @param callbackWithHonors if specified, this will be called instead of callbackOk
    */
  def create(
      h: BridgeHand,
      scoringSystem: ScoringSystem,
      table: Int = 0,
      board: Int = 0,
      north: String = "nsPlayer1",
      south: String = "nsPlayer2",
      east: String = "ewPlayer1",
      west: String = "ewPlayer2",
      dealer: PlayerPosition = North,
      callbackOk: CallbackOk,
      callbackCancel: CallbackCancel,
      teamNS: Option[Team.Id] = None,
      teamEW: Option[Team.Id] = None,
      newhand: Boolean = false,
      allowPassedOut: Boolean = true,
      callbackWithHonors: Option[CallbackWithHonors] = None,
      honors: Option[Int] = None,
      honorsPlayer: Option[PlayerPosition] = None,
      helppage: Option[String] = None,
      picture: Option[String] = None,
      supportPicture: Boolean = false,
      playingDuplicate: Boolean = false
  ) = // scalafix:ok ExplicitResultTypes; ReactComponent
    apply(
      Contract(
        h.id,
        h.contractTricks,
        h.contractSuit,
        h.contractDoubled,
        h.declarer,
        h.nsVul,
        h.ewVul,
        h.madeOrDown,
        h.tricks,
        honors,
        honorsPlayer,
        scoringSystem,
        None,
        table,
        board,
        north,
        south,
        east,
        west,
        dealer
      ),
      callbackOk,
      callbackCancel,
      teamNS,
      teamEW,
      newhand = newhand,
      allowPassedOut = allowPassedOut,
      callbackWithHonors = callbackWithHonors,
      honors = honors,
      honorsPlayer = honorsPlayer,
      helppage = helppage,
      picture = picture,
      supportPicture = supportPicture,
      playingDuplicate = playingDuplicate
    )

  var scorekeeper: PlayerPosition = North

}

object PageHandInternal {
  import PageHand._
  import HandStyles._

  val logger: Logger = Logger("bridge.PageHand")

  implicit val loggerForReactComponents: Logger = Logger("bridge.PageHand")
  implicit val defaultTraceLevelForReactComponents: Level = Level.FINER

  /**
    * The properties for rendering the component.
    */
  case class Props(
      contract: Contract,
      callbackOk: CallbackOk,
      callbackCancel: CallbackCancel,
      teamNS: Option[Team.Id],
      teamEW: Option[Team.Id],
      newhand: Boolean,
      allowPassedOut: Boolean,
      callbackWithHonors: Option[CallbackWithHonors] = None,
      honors: Option[Int] = None,
      honorsPlayer: Option[PlayerPosition] = None,
      helppage: Option[String] = None,
      picture: Option[String] = None,
      supportPicture: Boolean = false,
      playingDuplicate: Boolean = false
  )

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(
      currentcontract: Contract,
      contractTricks: Option[ContractTricks],
      contractSuit: Option[ContractSuit],
      contractDoubled: Option[ContractDoubled],
      declarer: Option[PlayerPosition],
      madeOrDown: Option[MadeOrDown],
      tricks: Option[Int],
      allowPassedOut: Boolean,
      initialDoubled: Option[ContractDoubled],
      allowHonors: Boolean,
      honors: Option[Int],
      honorsPlayer: Option[PlayerPosition],
      changeScorekeeper: Boolean = false,
      picture: Option[File] = None,
      showPicture: Boolean = false,
      removePicture: Boolean = false
  ) {

    def withRemovePicture(f: Boolean): State = copy(removePicture = f)
    def withShowPicture(f: Boolean): State = copy(showPicture = f)
    def pictureToShow(
        default: Option[String]
    ): (Option[File], Option[String]) = {
      if (removePicture) (None, None)
      else {
        (picture, default)
      }
    }

    def withScoring(): State = {
      val rc =
        if (
          contractTricks match {
            case Some(ct) if (ct.tricks == 0) && (allowPassedOut) =>
              true
            case _ =>
              contractTricks.isDefined &&
                contractTricks.get.tricks != 0 &&
                contractSuit.isDefined &&
                contractDoubled.isDefined &&
                declarer.isDefined &&
                madeOrDown.isDefined &&
                tricks.isDefined &&
                (!allowHonors || honors.isDefined && (honors.getOrElse(
                  100
                ) == 0 || honorsPlayer.isDefined) &&
                  (honors.getOrElse(0) != 100 || contractSuit != NoTrump))

          }
        ) {
          val cc = if (contractTricks.get.tricks == 0) {
            currentcontract.copy(
              contractTricks = 0,
              contractSuit = NoTrump,
              contractDoubled = NotDoubled,
              declarer = North,
              madeOrDown = Made,
              tricks = 0,
              honor = honors,
              honorPlayer = honorsPlayer
            )
          } else {
            currentcontract.copy(
              contractTricks = contractTricks.get,
              contractSuit = contractSuit.getOrElse(NoTrump),
              contractDoubled = contractDoubled.getOrElse(NotDoubled),
              declarer = declarer.getOrElse(North),
              madeOrDown = madeOrDown.getOrElse(Made),
              tricks = tricks.getOrElse(0),
              honor = honors,
              honorPlayer = honorsPlayer
            )
          }
          copy(currentcontract = cc.withScoring)
        } else {
          copy(currentcontract = currentcontract.copy(scorer = None))
        }
//      logger.warning("WithScoring new State "+rc+"\n"+
//                  "old State "+this)
      rc
    }

    def setContractTricks(cTricks: ContractTricks): State = {
      copy(contractTricks = Some(cTricks)).withScoring()
    }

    def setContractSuit(cSuit: ContractSuit): State = {
      copy(contractSuit = Some(cSuit)).withScoring()
    }

    def setContractDoubled(cDoubled: ContractDoubled): State = {
      copy(contractDoubled = Some(cDoubled)).withScoring()
    }

    def setNSVul(vul: Vulnerability): State = {
      copy(currentcontract = currentcontract.copy(nsVul = vul)).withScoring()
    }

    def setEWVul(vul: Vulnerability): State = {
      copy(currentcontract = currentcontract.copy(ewVul = vul)).withScoring()
    }

    def setDeclarer(decl: PlayerPosition): State = {
      copy(declarer = Some(decl)).withScoring()
    }

    def setMadeOrDown(mord: MadeOrDown): State = {
      copy(madeOrDown = Some(mord)).withScoring()
    }

    def setTricks(cTricks: Int): State = {
      copy(tricks = Some(cTricks)).withScoring()
    }

    def setHonors(chonors: Int): State = {
      val s =
        if (chonors != 0) copy(honors = Some(chonors))
        else copy(honors = Some(chonors), honorsPlayer = None)
      s.withScoring()
    }

    def setHonorsPlayer(chonorsPlayer: Option[PlayerPosition]): State = {
      copy(honorsPlayer = chonorsPlayer).withScoring()
    }

    def clear(): State =
      copy(
        contractTricks = None,
        contractSuit = None,
        contractDoubled = initialDoubled,
        declarer = None,
        madeOrDown = None,
        tricks = None,
        honors = None,
        honorsPlayer = None,
        picture = None,
        removePicture = false
      ).withScoring()

    def nextInput(): PageHandNextInput.Value = {
      def gotContractTricks =
        contractTricks.isDefined && contractTricks.get.tricks > 0
      import PageHandNextInput._
      PageHandNextInput.values.find { x =>
        x match {
          case InputContractTricks => contractTricks.isEmpty
          case InputContractSuit   => contractSuit.isEmpty && gotContractTricks
          case InputContractDoubled =>
            contractDoubled.isEmpty && gotContractTricks
          case InputContractBy => declarer.isEmpty && gotContractTricks
          case InputHonors     => allowHonors && honors.isEmpty
          case InputHonorsPlayer =>
            allowHonors && honors.getOrElse(0) != 0 && honorsPlayer.isEmpty
          case InputResultMadeOrDown => gotContractTricks && madeOrDown.isEmpty
          case InputResultTricks =>
            gotContractTricks && (tricks.isEmpty || madeOrDown.isEmpty ||
              !BridgeHand
                .getTricksRange(madeOrDown.get, contractTricks.get)
                .contains(tricks.get))
          case InputAll => true
        }
      }.get
    }

    def allowVulChange(): Boolean =
      currentcontract.scoringSystem.isInstanceOf[Test]
  }

  object PageHandNextInput extends MyEnumeration {
    type PageHandNextInput = Value
    val InputContractTricks, InputContractSuit, InputContractDoubled,
        InputContractBy, InputHonors, InputHonorsPlayer, InputResultMadeOrDown,
        InputResultTricks, InputAll = Value

  }

  /**
    * Internal backend object for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    import org.scalajs.dom.html
    private val canvasRef = Ref[html.Canvas]

    def modState(f: (State) => State, cb: Callback = Callback.empty): Callback =
      scope.modState(f, cb)

    def setContractTricks(tricks: ContractTricks): Callback =
      modState(s => s.setContractTricks(tricks))
    def setContractSuit(suit: ContractSuit): Callback =
      modState(s => s.setContractSuit(suit))
    def setContractDoubled(doubled: ContractDoubled): Callback =
      modState(s => s.setContractDoubled(doubled))
    def setDeclarer(declarer: PlayerPosition): Callback =
      modState(s => s.setDeclarer(declarer))
    def setNSVul(nsVul: Vulnerability): Callback =
      modState(s => s.setNSVul(nsVul))
    def setEWVul(ewVul: Vulnerability): Callback =
      modState(s => s.setEWVul(ewVul))
    def setMadeOrDown(madeOrDown: MadeOrDown): Callback =
      modState(s => s.setMadeOrDown(madeOrDown))
    def setTricks(tricks: Int): Callback = modState(s => s.setTricks(tricks))
    def setHonors(honors: Int): Callback = modState(s => s.setHonors(honors))
    def setHonorsPlayer(honorsPlayer: Option[PlayerPosition]): Callback =
      modState(s => s.setHonorsPlayer(honorsPlayer))

    val clear: Callback = modState(s => s.clear())

    val kickRefresh = scope.forceUpdate

    val ok: Callback = scope.stateProps { (state, props) =>
      val c = props.contract
      props.callbackWithHonors
        .map { cb =>
          cb(
            state.currentcontract.setPlayers(c.north, c.south, c.east, c.west),
            state.picture,
            state.removePicture,
            state.honors.getOrElse(0),
            state.honorsPlayer
          )
        }
        .getOrElse {
          props.callbackOk(
            state.currentcontract.setPlayers(c.north, c.south, c.east, c.west),
            state.picture,
            state.removePicture
          )
        }
    }

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      logger.fine(s"PageHand.render(): props.contract=${props.contract}")
      logger.fine(s"PageHand.render(): props.currentcontract=${state.currentcontract}")
      if (state.changeScorekeeper) renderChangeScorekeeper(props, state)
      else renderHand(props, state)
    }

    def setScorekeeper(pos: PlayerPosition): Callback =
      scope.modState(s => {
        scorekeeper = pos
        s.copy(changeScorekeeper = false)
      })

    val cancelSetScorekeeper: Callback = scope.modState(s => {
      s.copy(changeScorekeeper = false)
    })

    val changeScorekeeper: Callback = scope.modState(s => {
      s.copy(changeScorekeeper = true)
    })

    def renderChangeScorekeeper(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val maneuvers = TableManeuvers(
        props.contract.north,
        props.contract.south,
        props.contract.east,
        props.contract.west
      )

      val extraWidth = Properties.defaultHandButtonBorderRadius +
        Properties.defaultHandButtonPaddingBorder
      val width = s"${Pixels.maxLength(maneuvers.players: _*) + extraWidth}px"

      <.div(
        <.h1("Select scorekeeper"),
        maneuvers.sortedPlayers
          .map(p => {
            val (pos, posname) = maneuvers.find(p) match {
              case Some(l) => (l, l.name)
              case None    => (North, "Oops")
            }
            <.p(
              AppButton(
                posname,
                p,
                ^.width := width,
                ^.onClick --> setScorekeeper(pos),
                HandStyles.highlight(selected = scorekeeper == pos)
              )
            )
          })
          .toTagMod
      )
    }

    def getFile(filelist: FileList): Option[File] = {
      if (filelist.length == 1) {
        val file = filelist(0)
        Some(file)
      } else {
        None
      }
    }

    def doPictureInput(e: ReactEventFromInput): Callback =
      e.preventDefaultAction.inputFiles { filelist =>
        scope.modState { s =>
          getFile(filelist)
            .map { file =>
              // val formData = new FormData
              // formData.append("picture", file)
              logger.fine(
                s"Picture for hand taken: ${file.name}, length=${file.size}"
              )
              s.copy(picture = Some(file), removePicture = false)
            }
            .getOrElse({
              logger.fine("Picture for hand cleared")
              s.copy(picture = None)
            })
        }
      }

    def removePicture: Callback =
      scope.modState { s =>
        s.copy(removePicture = true)
      }

    val showPicture: Callback = scope.modState(s => s.copy(showPicture = true))

    def renderHand(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      val contract = state.currentcontract
      val valid = contract.scorer.isDefined

      val (asetNSVul, asetEWVul) = contract.scoringSystem match {
        case sc: Test =>
          (Some(setNSVul _), Some(setEWVul _))
        case _ =>
          (None, None)
      }

      val nextInput = state.nextInput()

      def sectionHeader = {
        val (cbHonors, cbHonorsP, curHonors, curHonorsP) =
          if (state.allowHonors) {
            (
              Some(setHonors _),
              Some(setHonorsPlayer _),
              state.honors,
              state.honorsPlayer
            )
          } else {
            (None, None, None, None)
          }
        SectionHeader(
          contract.scoringSystem,
          state.declarer,
          props.contract.north,
          props.contract.south,
          props.contract.east,
          props.contract.west,
          contract.nsVul,
          contract.ewVul,
          setDeclarer,
          asetNSVul,
          asetEWVul,
          contract.dealer,
          contract.table,
          contract.board,
          props.teamNS,
          props.teamEW,
          state.contractTricks,
          state.contractSuit,
          nextInput,
          cbHonors,
          cbHonorsP,
          curHonors,
          curHonorsP
        )
      }

      def sectionResults = {
        val (cbHonors, cbHonorsP, curHonors, curHonorsP) =
          if (state.allowHonors) {
            (
              Some(setHonors _),
              Some(setHonorsPlayer _),
              state.honors,
              state.honorsPlayer
            )
          } else {
            (None, None, None, None)
          }
        SectionResult(
          state.madeOrDown,
          state.tricks,
          state.contractTricks,
          state.contractSuit,
          state.contractDoubled,
          state.declarer,
          curHonors,
          curHonorsP,
//                      contract.north,
//                      contract.south,
//                      contract.east,
//                      contract.west,
          setMadeOrDown,
          setTricks,
          cbHonors,
          cbHonorsP,
          nextInput,
          contract
        )
      }

      def sectionContract(showContractHeader: Boolean) =
        SectionContract(
          state.contractTricks,
          state.contractSuit,
          state.contractDoubled,
          props.allowPassedOut,
          setContractTricks,
          setContractSuit,
          setContractDoubled,
          nextInput,
          showContractHeader
        )

      val pictureShowing = {
        val (file, default) = state.pictureToShow(props.picture)
        file.isDefined || default.isDefined
      }

      <.div(
        handStyles.pageHand,
        contract.scoringSystem match {
          case _: Duplicate => handStyles.playDuplicate
          case _: Chicago   => handStyles.playChicago
          case _: Rubber    => handStyles.playRubber
          case _            => TagMod()
        },
        <.div(
//          props.teamNS.map( team => TagMod() ).getOrElse( <.span( ^.id:="VerifySectionHeader","Bridge Scorer:") ),
          if (ComponentInputStyleButton.inputMethod == InputMethod.Original) {
            Seq(sectionHeader, sectionContract(true)).toTagMod
          } else {
            Seq(sectionContract(true), sectionHeader).toTagMod
          },
          sectionResults,
          SectionScore(contract, nextInput),
          <.div(
            baseStyles.divFooter,
            <.div(
              baseStyles.divFooterLeft,
              Button(
                handStyles.footerButton,
                "Ok",
                "OK",
                ^.disabled := !valid,
                HandStyles.highlight(required = valid),
                ^.onClick --> ok
              ),
              props.supportPicture ?= TagMod(
                <.div(
                  ^.id := "CameraInput",
                  <.label(
                    Camera(),
                    <.input(
                      ^.`type` := "file",
                      ^.name := "picture",
                      ^.accept := "image/*",
                      ^.capture := "environment", // use camera in back
                      ^.value := "",
                      ^.onChange ==> doPictureInput _
                    )
                  )
                ),
                <.button(
                  ^.`type` := "button",
                  handStyles.footerButton,
                  ^.onClick --> showPicture,
                  ^.id := "ShowPicture",
                  Photo(),
                  !pictureShowing ?= ^.display.none
                ),
                <.button(
                  ^.`type` := "button",
                  handStyles.footerButton,
                  ^.onClick --> removePicture,
                  ^.id := "RemovePicture",
                  DeleteForever(),
                  !pictureShowing ?= ^.display.none
                )
              )
            ),
            <.div(
              baseStyles.divFooterCenter,
              Button(
                handStyles.footerButton,
                "Cancel",
                "Cancel",
                ^.onClick --> props.callbackCancel
              ),
              Button(
                handStyles.footerButton,
                "ChangeSK",
                "Change Scorekeeper",
                ^.onClick --> changeScorekeeper
              )
            ),
            <.div(
              baseStyles.divFooterRight,
              ComponentInputStyleButton(kickRefresh, true),
              Button(
                handStyles.footerButton,
                "Clear",
                "Clear",
                ^.onClick --> this.clear
              ),
              props.helppage.whenDefined(p => HelpButton(p))
            )
          )
        ),
        PopupOkCancel(
          content = if (state.showPicture) {
            Some(Picture(state.picture, props.picture))
          } else {
            None
          },
          ok = Some(popupOk)
        )
      )
    }

    val popupOk: Callback = scope.modState { s =>
      s.copy(showPicture = false)
    }

    val didMount: Callback = scope.stateProps { (s, p) =>
      Callback {
        logger.fine(s"PageHand.didMount")
      }
    }

    val willUnmount: Callback = Callback {
      logger.info("PageHand.willUnmount")
    }

    def didUpdate(
        cdu: ComponentDidUpdate[Props, State, Backend, Unit]
    ): Callback =
      Callback {
        logger.fine(s"PageHand.didUpdate")
      }
  }

  val initialDoubled: Option[ContractDoubled] = None // Some(NotDoubled)
  private[hand] val component = ScalaComponent
    .builder[Props]("PageHand")
    .initialStateFromProps { props =>
      if (props.newhand)
        State(
          props.contract,
          None,
          None,
          initialDoubled,
          None,
          None,
          None,
          props.allowPassedOut,
          initialDoubled,
          props.callbackWithHonors.isDefined,
          None,
          None
        ).withScoring()
      else
        State(
          props.contract,
          Some(props.contract.contractTricks),
          Some(props.contract.contractSuit),
          Some(props.contract.contractDoubled),
          Some(props.contract.declarer),
          Some(props.contract.madeOrDown),
          Some(props.contract.tricks),
          props.allowPassedOut,
          Some(NotDoubled),
          props.callbackWithHonors.isDefined,
          props.honors,
          props.honorsPlayer
        ).withScoring()
    }
    .backend(new Backend(_))
    .renderBackend
    .componentDidMount(scope => scope.backend.didMount)
    .componentWillUnmount(scope => scope.backend.willUnmount)
    .componentDidUpdate(cdu => cdu.backend.didUpdate(cdu))
//                            .configure(LogLifecycleToServer.verbose)     // logs lifecycle events
    .build

}
