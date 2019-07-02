package com.example.data.duplicate.stats

class Statistic(name: String) {
  private var number: Int = 0
  private var total: Double = 0
  private var vmax: Double = Double.MinValue
  private var vmin: Double = Double.MaxValue

  override def toString() = {
    f"""Stat(${name}: ${total}%.2f/${number}, ave=${ave}%.2f, min=${vmin}%.2f, max=${vmax}%.2f)"""
  }

  def add(v: Double, n: Int): Unit = {
    if (n != 0) {
      number += n
      total += v * n
      vmax = Math.max(vmax, v)
      vmin = Math.min(vmin, v)
    }
  }

  def max = vmax
  def min = vmin
  def ave = if (number == 0) 0.0 else total / number
  def n = number

  /**
    * Returns the specified value v in the range [min,max]
    * into the range [sizemin,sizemax].
    *
    */
  def scale(v: Double, sizemin: Int, sizemax: Int): Int = {
    if (max == min) sizemax
    else ((v - min) * (sizemax - sizemin) / (max - min) + sizemin).toInt
  }

  /**
    * determines the distance from ave.
    * @param v
    * @param sizemin must be greater than 0
    * @param sizemax must be greater than sizemin
    * @return tuple2.  the first entry is a boolean, true indicates above average.
    * The second is the distance from average, scaled to range [smin,smax].  zero indicates average.
    * If v is min or max, then sizemax is return, and the boolean will
    * indicate the whether the result is min (false), max (true).
    */
  def scaleAve(v: Double, sizemin: Int, sizemax: Int): (Boolean, Int) = {
    if (min == max) (true, 0)
    else if (v == ave) (true, 0)
    else if (v < ave) {
      (false, ((v - min) * (sizemax - sizemin) / (ave - min) + sizemin).toInt)
    } else {
      (true, ((v - ave) * (sizemax - sizemin) / (max - ave) + sizemin).toInt)
    }

  }

  /**
    * determines the distance from ave.
    * @param v
    * @param sizemin must be greater than 0
    * @param sizemax must be greater than sizemin
    * @return tuple2.  the first entry is a boolean, true indicates above average.
    * The second is the distance from average, [0,1].
    * zero indicates average. a 1 indicates min or max
    */
  def scaleAveAsFraction(v: Double): (Boolean, Double) = {
    val a = ave
    if (min == max) (true, 0)
    else if (v == a) (true, 0)
    else if (v < a) {
      (false, ((a - v) / (a - min)))
    } else {
      (true, ((v - a) / (max - a)))
    }
  }

}
