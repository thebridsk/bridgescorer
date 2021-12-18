package com.github.thebridsk.bridge.data.bridge.individual

object IMPScoring {

//  https://www.bridgehands.com/I/IMP.htm
//
//    Difference
//  in points    IMPs
//  20-40        1
//  50-80        2
//  90-120       3
//  130-160      4
//  170-210      5
//  220-260      6
//  270-310      7
//  320-360      8
//  370-420      9
//  430-490     10
//  500-590     11
//  600-740     12
//  750-890     13
//  900-1090    14
//  1100-1290   15
//  1300-1490   16
//  1500-1740   17
//  1750-1990   18
//  2000-2240   19
//  2250-2490   20
//  2500-2990   21
//  3000-3490   22
//  3500-3990   23
//  4000+       24

  case class IMPEntry(min: Int, max: Int, IMP: Int)

  val IMPTable: List[IMPEntry] = List(
    IMPEntry(0, 10, 0),
    IMPEntry(20, 40, 1),
    IMPEntry(50, 80, 2),
    IMPEntry(90, 120, 3),
    IMPEntry(130, 160, 4),
    IMPEntry(170, 210, 5),
    IMPEntry(220, 260, 6),
    IMPEntry(270, 310, 7),
    IMPEntry(320, 360, 8),
    IMPEntry(370, 420, 9),
    IMPEntry(430, 490, 10),
    IMPEntry(500, 590, 11),
    IMPEntry(600, 740, 12),
    IMPEntry(750, 890, 13),
    IMPEntry(900, 1090, 14),
    IMPEntry(1100, 1290, 15),
    IMPEntry(1300, 1490, 16),
    IMPEntry(1500, 1740, 17),
    IMPEntry(1750, 1990, 18),
    IMPEntry(2000, 2240, 19),
    IMPEntry(2250, 2490, 20),
    IMPEntry(2500, 2990, 21),
    IMPEntry(3000, 3490, 22),
    IMPEntry(3500, 3990, 23),
    IMPEntry(4000, 100000, 24)
  )

  def getIMPs(points: Int): Int = {
    IMPTable
      .find { entry =>
        entry.min <= points && entry.max >= points
      }
      .map { entry =>
        entry.IMP
      }
      .getOrElse(0)

  }

}
