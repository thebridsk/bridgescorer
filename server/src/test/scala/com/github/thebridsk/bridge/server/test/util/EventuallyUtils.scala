package com.github.thebridsk.bridge.server.test.util

import org.scalatest.concurrent.Eventually
import org.scalactic.source.Position
import org.scalatest.exceptions.TestFailedException
import org.scalatest.exceptions.StackDepthException
import org.scalactic.exceptions.NullArgumentException
import com.github.thebridsk.utilities.logging.Logger

object EventuallyUtilsInternals {
  val log: Logger = Logger[EventuallyUtils]()

  /**
    * If message or message contents are null, throw a null exception, otherwise
    * create a function that returns the option.
    */
  def toExceptionFunction(
      message: Option[String]
  ): StackDepthException => Option[String] = {
    message match {
      case null => throw new NullArgumentException("message is null")
      case None => throw new NullArgumentException("message is None")
      case Some(null) =>
        throw new NullArgumentException("message is a Some(null)")
      case _ => { e => message }
    }
  }
}

/**
  * Used to indicate that there was no result.
  * This exception is used by some functions when
  * they are wrapped in an eventually { ... } clause.
  * @author werewolf
  */
class NoResultYet(
    message: String = "No result yet",
    cause: Option[Throwable] = None,
    payload: Option[Any] = None
)(implicit pos: Position)
    extends TestFailedException(
      EventuallyUtilsInternals.toExceptionFunction(Some(message)),
      cause,
      pos,
      payload
    )

trait EventuallyUtils {
  import Eventually.{patienceConfig => _, _}

  def tcpSleep(sec: Int = 30): Unit = {
    // MonitorTCP.waitForConnections(sec seconds)
  }

  /**
    * Invokes the passed by-name parameter repeatedly until it either returns true, or a configured maximum
    * amount of time has passed, sleeping a configured interval between attempts.
    *
    * <p>
    * The by-name parameter "succeeds" if it returns a true. It "fails" if it returns false or if it throws any exception that
    * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException"><code>TestPendingException</code></a> and
    * <code>Error</code>s listed in the
    * <a href="Suite.html#errorHandling">Treatment of <code>java.lang.Error</code>s</a> section of the
    * documentation of trait <code>Suite</code>.)
    * </p>
    *
    * <p>
    * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the <code>timeout</code> field of
    * the <code>PatienceConfig</code> passed implicitly as the last parameter.
    * The interval to sleep between attempts is configured by the <code>interval</code> field of
    * the <code>PatienceConfig</code> passed implicitly as the last parameter.
    * </p>
    *
    * @param fun the by-name parameter to repeatedly invoke
    * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
    *          <code>interval</code> parameters
    * @param pos the filename and line number of where it is called from.
    * @throws NoResultYet if <code>fun</code> always returned None
    */
  def eventuallyTrue(
      fun: => Boolean
  )(implicit config: PatienceConfig, pos: Position): Unit = {
    EventuallyUtilsInternals.log.fine("Implicit config = " + config)
    eventually({
      if (!fun) throw new NoResultYet("function did not return true")(pos)
    })

  }

  /**
    * Invokes the passed by-name parameter repeatedly until it either returns some object, or a configured maximum
    * amount of time has passed, sleeping a configured interval between attempts.
    *
    * <p>
    * The by-name parameter "succeeds" if it returns a Some(obj). It "fails" if it returns None or if it throws any exception that
    * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException"><code>TestPendingException</code></a> and
    * <code>Error</code>s listed in the
    * <a href="Suite.html#errorHandling">Treatment of <code>java.lang.Error</code>s</a> section of the
    * documentation of trait <code>Suite</code>.)
    * </p>
    *
    * <p>
    * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the <code>timeout</code> field of
    * the <code>PatienceConfig</code> passed implicitly as the last parameter.
    * The interval to sleep between attempts is configured by the <code>interval</code> field of
    * the <code>PatienceConfig</code> passed implicitly as the last parameter.
    * </p>
    *
    * @param fun the by-name parameter to repeatedly invoke
    * @param config the <code>PatienceConfig</code> object containing the <code>timeout</code> and
    *          <code>interval</code> parameters
    * @param pos the filename and line number of where it is called from.
    * @return the object returned by invoking the <code>fun</code> by-name parameter, the first time it succeeds
    * @throws NoResultYet if <code>fun</code> always returned None
    */
  def eventuallySome[T](
      fun: => Option[T]
  )(implicit config: PatienceConfig, pos: Position): T = {
    eventually({
      fun match {
        case Some(t) => Some(t)
        case None =>
          throw new NoResultYet("function did not return some object")(pos)
      }
    }).get
  }

}

object EventuallyUtils extends EventuallyUtils {}
