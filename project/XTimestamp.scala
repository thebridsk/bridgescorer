


object XTimestamp {
  private var gottime = false

  private var time = 0L
  private var stime = timestamp()

  def millis( isSnap: Boolean ) = {
    if (gottime) {
      time
    } else {
      if (isSnap) {
        val t = System.currentTimeMillis()
        val dtfDate = new java.text.SimpleDateFormat("yyyy-MM-dd")
        dtfDate.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
        val ts = dtfDate.format(new java.util.Date(t))
        val ti = dtfDate.parse(ts)
        time = ti.getTime()
        stime = ts
      } else {
        time = System.currentTimeMillis()
        stime = timestamp()
      }
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
