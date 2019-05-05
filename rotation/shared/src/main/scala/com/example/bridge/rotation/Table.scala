package com.example.bridge.rotation

case class Table( north: String,
                  south: String,
                  east: String,
                  west: String,
                  sittingOut: String ) {
  import Table._

  override
  def toString() = {
    s"Table( $north-$south, $east-$west, $sittingOut )"
  }

  /**
   * Returns the location of the specified player
   */
  def find( p: String ) = {
    p match {
      case `north` => Some(North)
      case `south` => Some(South)
      case `east` => Some(East)
      case `west` => Some(West)
      case `sittingOut` => Some(SittingOut)
      case _ => None
    }
  }

  /**
   * Returns the player at specified location
   */
  def find( l: Location ) = {
    l.player(this)
  }

  def findNotSittingOut( p: String ) = {
    p match {
      case `north` => Some(North)
      case `south` => Some(South)
      case `east` => Some(East)
      case `west` => Some(West)
      case `sittingOut` => None
      case _ => None
    }
  }

  def partnerLocOf( l: Location ): Option[Location] = l match {
    case North => Some(South)
    case South => Some(North)
    case East => Some(West)
    case West => Some(East)
    case SittingOut => None
  }

  def leftLocOf( l: Location ): Option[Location] = l match {
    case North => Some(East)
    case South => Some(West)
    case East => Some(South)
    case West => Some(North)
    case SittingOut => None
  }

  def rightLocOf( l: Location ): Option[Location] = l match {
    case North => Some(West)
    case South => Some(East)
    case East => Some(North)
    case West => Some(South)
    case SittingOut => None
  }

  def partnerOf( p: String ): Option[String] = {
    find(p) match {
      case Some(l) => partnerOf(l)
      case None => None
    }
  }

  def partnerOf( l: Location ): Option[String] = partnerLocOf(l) match {
    case Some(p) => Some(p.player(this))
    case None => None
  }

  def leftOf( p: String ): Option[String] = {
    find(p) match {
      case Some(l) => leftOf(l)
      case None => None
    }
  }

  def leftOf( l: Location ): Option[String] = leftLocOf(l) match {
    case Some(p) => Some(p.player(this))
    case None => None
  }

  def rightOf( p: String ): Option[String] = {
    find(p) match {
      case Some(l) => rightOf(l)
      case None => None
    }
  }

  def rightOf( l: Location ): Option[String] = rightLocOf(l) match {
    case Some(p) => Some(p.player(this))
    case None => None
  }

  def setPlayer( l: Location, p: String ) = {
    l match {
      case North => copy( north = p )
      case South => copy( south = p )
      case East => copy( east = p )
      case West => copy( west = p )
      case SittingOut => copy( sittingOut = p )
    }
  }

  private def error[T]( msg: String ): T = {
    throw new IllegalArgumentException( msg )
  }

  def swap( l1: Location, l2: Location ): Table = {
    val p1 = find(l1)
    val p2 = find(l2)
    setPlayer(l1,p2).setPlayer(l2, p1)
  }

  def swapLeftPartnerOf( l: Location ): Table = {
    val partner = partnerLocOf(l).getOrElse(error("Did not find partner of "+l))
    val left = leftLocOf(l).getOrElse(error("Did not find left of "+l))
    swap(partner,left)
  }

  def swapRightPartnerOf( l: Location ): Table = {
    val partner = partnerLocOf(l).getOrElse(error("Did not find partner of "+l))
    val right = rightLocOf(l).getOrElse(error("Did not find right of "+l))
    swap(partner,right)
  }

  def swapLeftRight( l: Location ): Table = {
    val left = leftLocOf(l).getOrElse(error("Did not find left of "+l))
    val right = rightLocOf(l).getOrElse(error("Did not find right of "+l))
    swap(left,right)
  }

  def swapSittingOutAnd( l: Location ): Table = {
    swap(SittingOut,l)
  }

  def swapLeftAndNew( l: Location ): Table = {
    val left = leftLocOf(l).getOrElse(error("Did not find left of "+l))
    swap(left,l)
  }

  def swapRightAndNew( l: Location ): Table = {
    val left = rightLocOf(l).getOrElse(error("Did not find right of "+l))
    swap(left,l)
  }

  def rotateSwapLeftPartnerOf( l: Location ): Table = {
    swapLeftPartnerOf(l).swapSittingOutAnd(l)
  }

  def rotateSwapRightPartnerOf( l: Location ): Table = {
    swapRightPartnerOf(l).swapSittingOutAnd(l)
  }

  def getSwappingPlayersInRotateSwapRightPartnerOf( l: Location ): List[(String,String)] = {
    val partner = partnerOf(l).get
    val right = rightOf(l).get
    val me = find(l)
    val out = sittingOut
    (me,out)::(partner,right)::Nil
  }

  def rotateCounterClockwise( l: Location ): Table = {
    val t = swapSittingOutAnd(l)  // .swapRightAndNew(l)
    val left = t.leftLocOf(l).get
    val partner = t.partnerLocOf(l).get
    val right = t.rightLocOf(l).get
    val pl = t.find(left)
    val pp = t.find(partner)
    val pr = t.find(right)
    t.setPlayer(partner, pr).setPlayer(left, pp).setPlayer(right, pl)
  }

  def rotateClockwise( l: Location ): Table = {
    val t = swapSittingOutAnd(l) // .swapLeftAndNew(l)
    val left = t.leftLocOf(l).get
    val partner = t.partnerLocOf(l).get
    val right = t.rightLocOf(l).get
    val pl = t.find(left)
    val pp = t.find(partner)
    val pr = t.find(right)
    t.setPlayer(partner, pl).setPlayer(left, pr).setPlayer(right, pp)
  }

  def rotateCounterClockwiseAlt( l: Location ): Table = {
    val t = swapSittingOutAnd(l)
    val left = t.leftLocOf(l).get
    t.swap(left, l)
  }

  def rotateClockwiseAlt( l: Location ): Table = {
    val t = swapSittingOutAnd(l)
    val right = t.rightLocOf(l).get
    t.swap(right, l)
  }

  def rotateSwapLeftRight( l: Location ): Table = {
    swapSittingOutAnd(l).swapLeftRight(l)
  }

  def hasPartnership( p1: String, p2: String ) = {
    (north == p1 && south == p2) || (north == p2 && south == p1) || (east == p1 && west == p2) || (east == p2 && west == p1)
  }

  def players() = {
    north::south::east::west::sittingOut::Nil
  }
}

object Table {
  sealed trait Location {
    def player( t: Table ): String
  }
  object North extends Location {
    def player( t: Table ) = t.north
    override def toString() = "North"
  }
  object South extends Location {
    def player( t: Table ) = t.south
    override def toString() = "South"
  }
  object East extends Location {
    def player( t: Table ) = t.east
    override def toString() = "East"
  }
  object West extends Location {
    def player( t: Table ) = t.west
    override def toString() = "West"
  }
  object SittingOut extends Location {
    def player( t: Table ) = t.sittingOut
    override def toString() = "SittingOut"
  }
}

object Chicago5Rotation {
  import Table._

  /**
   * @param order must be length 4.  The order that the players sit out.  If Nil, then north,south,west,east
   * @param list the already played hands
   */
  def playRight(order: List[String] = Nil, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    var s = ts.last
    var players = if (order.isEmpty) s.north::s.south::s.west::s.east::Nil else order
    for ( playerGoingOut <- players ) {
      val t = ts.last
      val l = getLocationNotSittingOut(playerGoingOut,ts)
      ts = ts:::t.rotateSwapRightPartnerOf(l)::Nil
    }
    ts
  }

  /**
   * @param order must be length 1.  The order that the players sit out.  If Nil, then north,south,west,east
   * @param list the already played hands
   */
  def playOneRight(order: List[String], list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    var t = ts.last
    val l = getLocationNotSittingOut(order.head,ts)
    ts = ts:::t.rotateSwapRightPartnerOf(l)::Nil
    ts
  }

  /**
   * @param order must be length 1.  The order that the players sit out.  If Nil, then north,south,west,east
   * @param list the already played hands
   */
  def playOneLeft(order: List[String], list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    var t = ts.last
    val l = getLocationNotSittingOut(order.head,ts)
    ts = ts:::t.rotateSwapLeftPartnerOf(l)::Nil
    ts
  }

  /**
   * @param order must be length 4.  The order that the players sit out.  If Nil, then north,south,west,east
   * @param list the already played hands
   */
  def playLeft(order: List[String] = Nil, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    var s = ts.last
    var players = if (order.isEmpty) s.north::s.south::s.west::s.east::Nil else order
    for ( playerGoingOut <- players ) {
      val t = ts.last
      val l = getLocationNotSittingOut(playerGoingOut,ts)
      ts = ts:::t.rotateSwapLeftPartnerOf(l)::Nil
    }
    ts
  }


  /**
   * @param order must be length 5.  The order that the players sit out.  If Nil, then sittingOut,north,south,west,east
   *              The first entry must be 'sittingOut' in the last entry in list
   * @param list the already played hands
   */
  def playBoth(order: List[String] = Nil, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    val s = ts.last
    val players = if (order.isEmpty) s.sittingOut::s.west::s.east::s.south::s.north::Nil else order
    println("order: "+order)
    println("players: "+players)
    if (s.sittingOut != players.head) throw new IllegalArgumentException("First player in order must be sitting out in last entry")
    ts = playRight(players.drop(1),ts)
    ts = playLeft(players.take(4),ts)
    ts = playOneRight(players.drop(4),ts)
    println( "playBoth: "+ts )
    ts
  }

  def playCounterClockwise( playerGoingOut: String, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    val s = ts.last
    val loc = getLocationNotSittingOut(playerGoingOut,list)
    ts = ts:::s.rotateCounterClockwise(loc)::Nil
    ts

  }

  def playClockwise( playerGoingOut: String, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    val s = ts.last
    val loc = getLocationNotSittingOut(playerGoingOut,list)
    ts = ts:::s.rotateClockwise(loc)::Nil
    ts

  }

  private def getLocationNotSittingOut( playerGoingOut: String, list: List[Table] ) = {
    val t = list.last
    val loc = t.find(playerGoingOut).get
    if (loc == SittingOut) {
      println("Location is SittingOut, which can't happen here")
      println(s"Looking for player $playerGoingOut")
      println( list.mkString("\n") )
      throw new IllegalArgumentException("Player can't be sitting out")
    }
    loc
  }

  def playSwapLeftRight( playerGoingOut: String, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    val s = ts.last
    val loc = getLocationNotSittingOut(playerGoingOut,ts)
    ts = ts:::s.rotateSwapLeftRight(loc)::Nil
    ts
  }


  /**
   * @param order must be length 5.  The order that the players sit out.  If Nil, then sittingOut,north,south,west,east
   *              The first entry must be 'sittingOut' in the last entry in list
   * @param list the already played hands
   */
  def allRotations(rightfirst: Boolean, order: List[String] = Nil, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {

// http://www.bridgeguys.com/pdf/FrenchPartyBridge04.pdf
//
//  Chukker   Movement
//  1         As per draw for partners and sit-out
//  2-5       Player to left of newcomer sits still, other two switch
//  6-9       Player to right sits still, other two switch
//  10        Player to left sits still, other two switch
//  11        Other three all move, rotating counterclockwise
//  12        Player opposite sits still, other two switch
//  13        Other three all move, rotating clockwise         // should be counterclockwise
//  14        Player opposite sits still, other two switch
//  15        Other three all move, rotating clockwise

// Chukker 2-5 done by playLeft
// Chukker 6-9 done by playRight
// Chukker 1-10 done by playBoth

    val s = list.last
    val players = if (order.isEmpty) s.sittingOut::s.west::s.east::s.south::s.north::Nil else order

    val p1 = players(0)
    val p2 = players(1)
    val p3 = players(2)
    val p4 = players(3)
    val p5 = players(4)

//    var ts = playBoth(players,list)
    var ts = list
    if (rightfirst) {
      ts = playRight(players.drop(1),ts)
      ts = playLeft(players.take(4),ts)
      ts = playOneRight(players.drop(4),ts)
      ts = playCounterClockwise(p1, ts)
      ts = playSwapLeftRight(p2, ts)
      ts = playCounterClockwise(p3, ts)
      ts = playSwapLeftRight(p4, ts)
      playClockwise(p5, ts)
    } else {
      ts = playLeft(players.drop(1),ts)
      ts = playRight(players.take(4),ts)
      ts = playOneLeft(players.drop(4),ts)
      ts = playClockwise(p1, ts)
      ts = playSwapLeftRight(p2, ts)
      ts = playCounterClockwise(p3, ts)
      ts = playSwapLeftRight(p4, ts)
      playCounterClockwise(p5, ts)
    }
  }

  def playComplete(order: List[String] = Nil, list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var nextPlayer = order.tail
    def next = {
      nextPlayer match {
        case playerGoingOut::tt =>
          nextPlayer = tt
          playerGoingOut
        case Nil =>
          nextPlayer = order.tail
          order.head
      }
    }

    play( maneuverLeftStillOthersSwap(next) _ ::
          maneuverLeftStillOthersSwap(next) _ ::
          maneuverLeftStillOthersSwap(next) _ ::
          maneuverLeftStillOthersSwap(next) _ ::
          maneuverRightStillOthersSwap(next) _ ::
          maneuverRightStillOthersSwap(next) _ ::
          maneuverRightStillOthersSwap(next) _ ::
          maneuverRightStillOthersSwap(next) _ ::
          maneuverLeftStillOthersSwap(next) _ ::
          maneuverRotateCounterClockwise(next) _ ::
          maneuverSwapLeftRight(next) _ ::
          maneuverRotateCounterClockwise(next) _ ::
          maneuverSwapLeftRight(next) _ ::
          maneuverRotateClockwise(next) _ ::
          Nil, list )
  }

  type Maneuver = (Table) => Table

  def maneuverRotateCounterClockwise( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateCounterClockwise(l)
  }

  def maneuverRotateClockwise( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateClockwise(l)
  }

  def maneuverRotateCounterClockwiseAlt( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateCounterClockwiseAlt(l)
  }

  def maneuverRotateClockwiseAlt( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateClockwiseAlt(l)
  }

  def maneuverLeftStillOthersSwap( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateSwapRightPartnerOf(l)
  }

  def maneuverRightStillOthersSwap( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateSwapLeftPartnerOf(l)
  }

  def maneuverSwapLeftRight( playerGoingOut: String )( t: Table ): Table = {
    val l = t.findNotSittingOut(playerGoingOut).getOrElse(throw new IllegalArgumentException(s"Unable to find player $playerGoingOut"))
    t.rotateSwapLeftRight(l)
  }

  def play( maneuvers: List[Maneuver], list: List[Table] = Table( north = "1", south = "2", east = "3", west = "4", sittingOut = "5" )::Nil) = {
    var ts = list
    var last = list.last
    for ( maneuver <- maneuvers ) {
      last = maneuver(last)
      ts = ts:::last::Nil
    }
    ts
  }
}
