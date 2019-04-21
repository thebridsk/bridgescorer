


object XTimestamp {
  private var gottime = false

  private var time = 0L
  private var stime = timestamp()

  def millis( isSnap: Boolean ) = {
    if (gottime || isSnap) {
      time
    } else {
      time = System.currentTimeMillis()
      stime = timestamp()
      gottime = true
      time
    }
  }

  def string( isSnap: Boolean ) = {
    millis(isSnap)
    stime
  }

  private def timestamp() = {
    val now = time
    val dtf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    dtf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    val nowStr = dtf.format(new java.util.Date(now))
    nowStr
  }

}
