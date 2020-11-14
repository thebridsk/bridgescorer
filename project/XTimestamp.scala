import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.ZonedDateTime



object XTimestamp {
  private var gottime = false

  val dtfDateSnap = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone( ZoneId.systemDefault())
  val dtfDateTimeSnap = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone( ZoneId.systemDefault())
  val dtfDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone( ZoneId.of("UTC") )

  private var time = 0L
  private var stime = timestamp()

  def millis( isSnap: Boolean ) = synchronized {
    if (gottime) {
      time
    } else {
      if (isSnap) {
        val t = System.currentTimeMillis()
        val ts = dtfDateSnap.format( Instant.ofEpochMilli(t))
        val zdt  = ZonedDateTime.parse( s"$ts 00:00:00.000", dtfDateTimeSnap);
        time = zdt.toInstant().toEpochMilli()
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
    val nowStr = dtfDate.format(Instant.ofEpochMilli(now))
    nowStr
  }

}
