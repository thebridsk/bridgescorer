import java.io.File
import java.security.MessageDigest
import java.security.DigestInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedInputStream


object Sha256 {

  /**
   * Generates the SHA-256 of a file, and writes out a file with the sha256 in the same format as sha256sum requires.
   * @param f the file to get the SHA-256 sum of.
   * @return the SHA-256 in the format required by sha256sum
   */
  def generate( f: File ) = {
    val name = f.getName

    val shaFile = new File( f.toString()+".sha256" )

    val md = MessageDigest.getInstance("SHA-256")

    val in = new DigestInputStream( new BufferedInputStream( new FileInputStream(f), 1024*1024), md )
    try {
      val b = new Array[Byte]( 1024*1024 )

      var rlen = 0
      while ( { rlen = in.read(b); rlen > 0 }) {
      }

      val sha = toHexString( md.digest() )

      val out = new OutputStreamWriter( new FileOutputStream( shaFile ), "UTF8" )
      try {
        val s =  sha+" *"+name
        out.write( s+"\n" )
        out.flush()
        s
      } finally {
        out.close()
      }

    } finally {
      in.close()
    }

  }

  def toHexString( hash: Array[Byte] ) = {
    hash.map( b => f"${b}%02x" ).mkString
  }

}
