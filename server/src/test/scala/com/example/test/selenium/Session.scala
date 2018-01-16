package com.example.test.selenium

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.safari.SafariDriver
import java.util.concurrent.TimeUnit
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.Point
import org.openqa.selenium.Dimension
import org.openqa.selenium.firefox.internal.ProfilesIni
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.scalatest.selenium.Firefox
import org.scalatest.selenium.InternetExplorer
import org.openqa.selenium.ie.InternetExplorerDriver
import utils.logging.Logger
import java.util.logging.Level
import org.openqa.selenium.JavascriptExecutor
import com.example.data.Id
import org.openqa.selenium.chrome.ChromeDriverService
import java.util.concurrent.atomic.AtomicLong
import java.io.File
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.SessionNotCreatedException
import scala.annotation.tailrec
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxDriverLogLevel
import java.util.Base64
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.io.BufferedReader
import java.io.PrintWriter
import org.openqa.selenium.WebDriverException

class Session extends WebDriver {
  import Session._

  implicit var webDriver: WebDriver = null

  val debug = {
    val f = getPropOrEnv("WebDriverDebug").getOrElse("")
    f.equalsIgnoreCase("true") || f.equals("1")
  }

//  private def firefoxOld = {
//    val profile = new ProfilesIni();
//
//    val fp = profile.getProfile("seleniumTesting");
//    assert( fp != null )
////    val fp = new FirefoxProfile();
//
//    // the following line gets rid of firefox starting on page "about:blank&utm_content=firstrun"
//    fp.setPreference("browser.startup.homepage_override.mstone", "ignore")
//
//    fp.setPreference("browser.startup.homepage", "about:blank");
//    fp.setPreference("startup.homepage_welcome_url", "about:blank");
//    fp.setPreference("startup.homepage_welcome_url.additional", "about:blank");
//    new FirefoxDriver(fp)
//  }

  private def showFirefoxProfile( fp: FirefoxProfile ): Unit = {

    val jsonInBase64 = fp.toJson()
    testlog.info(s"seleniumTesting profile is\n${jsonInBase64}")

    val decoder = Base64.getDecoder

    val json = decoder.decode(jsonInBase64)

    val sw = new StringWriter
    val pw = new PrintWriter( sw )

    val l = json.length
    testlog.info(s"seleniumTesting profile size is ${l}")
  }

  private def firefox = {

    val profile = new ProfilesIni();

    val fp = profile.getProfile("seleniumTesting");
    assert( fp != null )
    showFirefoxProfile(fp)
//    val fp = new FirefoxProfile();

    // need to start firefox as if by the following:
    //   firefox -new-instance -no-remote -P "seleniumTesting"
    val options = new FirefoxOptions().
      setProfile(fp).
    // the following line gets rid of firefox starting on page "about:blank&utm_content=firstrun"
      addPreference("browser.startup.homepage_override.mstone", "ignore").

      addPreference("startup.homepage_welcome_url", "about:blank").
      addPreference("startup.homepage_welcome_url.additional", "about:blank").
      addPreference("browser.startup.homepage", "about:blank")

//      options.addArguments( "-new-instance")

      options.setLogLevel(FirefoxDriverLogLevel.TRACE)

    new FirefoxDriver(options)
  }

  private var chromeDriverService: Option[ChromeDriverService] = None

  private def chrome( headless: Boolean ) = chromeCurrent(headless)

  private def chromeExperiment( headless: Boolean ): RemoteWebDriver = {
    val logfile = new File(s"chromedriver.${Session.sessionCounter.incrementAndGet()}.log")

    val options = new ChromeOptions
    // http://peter.sh/experiments/chromium-command-line-switches/
    // and "chromedriver --help"
    if (!debug) options.addArguments("--silent" )
    else options.addArguments(s"""--log-path=${logfile.toString}""", "--verbose", "--silent")
//    options.addArguments("--enable-automation=false")
    options.addArguments("--disable-infobars")
    if (headless) options.addArguments("--headless")
//    val capabilities = DesiredCapabilities.chrome();
//    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
    val driver = new ChromeDriver(options);
    driver
  }

  private def chromeCurrent( headless: Boolean ) = {
    // does not work
//    val options = new ChromeOptions()
//    options.addArguments("--verbose", "--log-path=C:\\temp\\chrome_test.log")

    val logfile = new File(s"chromedriver.${Session.sessionCounter.incrementAndGet()}.log")

    val service = if (debug) {
      new ChromeDriverService.Builder()
                      .usingAnyFreePort()
                      .withSilent(false)
                      .withLogFile(logfile)
                      .withVerbose(true)
                      .build()
    } else {
      new ChromeDriverService.Builder()
                      .usingAnyFreePort()
                      .withSilent(true)
                      .build()
    }

    try {
      chromeDriverService = Some(service)
      service.start()
      val options = new ChromeOptions
      options.addArguments("--disable-infobars")
      if (headless) options.addArguments("--headless")
      val capabilities = DesiredCapabilities.chrome();
      capabilities.setCapability(ChromeOptions.CAPABILITY, options);
      new RemoteWebDriver(service.getUrl(), capabilities)
    } catch {
      case x: Throwable =>
        service.stop()
        chromeDriverService = None
        throw x
    }

  }

  private def edge = {
    new EdgeDriver
  }

  private def internetExplorer = {
    new InternetExplorerDriver
  }

  private def safari = {
    new SafariDriver
  }

  /**
   * Start a browser webdriver.  Will retry the default number of times, 2.
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If invalid browser name or null,
   *                then firefox is used.
   * @return this Session object
   */
  def sessionStart( browser: String ): Session = sessionStart( if (browser==null) None; else Some(browser) )

  /**
   * Start a browser webdriver
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If invalid browser name or null,
   *                then firefox is used.
   * @param retry the number of retries
   * @return this Session object
   */
  def sessionStart( browser: String, retry: Int ): Session = sessionStart( if (browser==null) None; else Some(browser), retry )

  /**
   * The default browser when a specific browser has not been specified.
   */
  def defaultBrowser = chrome(false)

  /**
   * Start a browser webdriver
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If omitted or invalid browser name or None,
   *                then chrome is used.
   * @param retry the number of retries
   * @return this Session object
   */
  def sessionStart( browser: Option[String] = None, retry: Int = 2): Session = {
    sessionStartInternal(browser,retry,retry)
  }

  /**
   * Start a browser webdriver
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If omitted or invalid browser name or None,
   *                then chrome is used.
   * @param retry the number of retries
   * @return this Session object
   */
  @tailrec
  private def sessionStartInternal( browser: Option[String] = None, original: Int = 2, retry: Int = 2): Session = {
    try {
      createSession(browser)
    } catch {
      case x: SessionNotCreatedException =>
        if (retry <= 0) throw x
        Thread.sleep( (original-retry)*3000L )
        sessionStartInternal( browser, original, retry-1 )
    }
  }

  private def createSession( browser: Option[String] = None): Session = synchronized {
    webDriver = browser.orElse(getPropOrEnv("DefaultWebDriver")) match {
      case None =>
        testlog.fine( "DefaultWebDriver is not set in system properties or environment, using default" )
        defaultBrowser // default
      case Some(wd) =>
        wd.toLowerCase() match {
          case "chrome" =>
            testlog.fine( "Using chrome" )
            chrome(false) // Chrome.webDriver
          case "chromeheadless" =>
            testlog.fine( "Using chrome headless" )
            chrome(true) // Chrome.webDriver
          case "safari" =>
            testlog.fine( "Using safari" )
            safari // Safari.webDriver
          case "firefox" =>
            testlog.fine( "Using firefox" )
            firefox // Firefox.webDriver
          case "edge" =>
            testlog.fine( "Using edge" )
            edge
          case "ie" =>
            testlog.fine( "Using internet explorer" )
            internetExplorer
          case _ =>
            testlog.fine( "Unknown browser specified, using default: "+wd )
            defaultBrowser // default
        }
    }
    sessionImplicitlyWait(2, TimeUnit.SECONDS)
    this
  }

  /**
   * Start a browser webdriver if not already running.  Will retry the default number of times, 2.
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If invalid browser name or null,
   *                then firefox is used.
   * @return this Session object
   */
  def sessionStartIfNotRunning( browser: String ): Session = sessionStartIfNotRunning( if (browser==null) None; else Some(browser) )

  /**
   * Start a browser webdriver if not already running.
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If invalid browser name or null,
   *                then firefox is used.
   * @param retry the number of retries
   * @return this Session object
   */
  def sessionStartIfNotRunning( browser: String, retry: Int ): Session = sessionStartIfNotRunning( if (browser==null) None; else Some(browser), retry )

  /**
   * Start a browser webdriver if not already running.
   * @param browser the name of the browser to start.
   *                Valid values: firefox, chrome, safari, edge, ie
   *                If omitted or invalid browser name or None,
   *                then chrome is used.
   * @param retry the number of retries
   * @return this Session object
   */
  def sessionStartIfNotRunning( browser: Option[String] = None, retry: Int = 2 ): Session = synchronized {
    if (webDriver==null) sessionStart(browser, retry)
    else this
  }

  def isSessionRunning = synchronized { webDriver!=null }

  def sessionImplicitlyWait(time: Long, unit: TimeUnit = TimeUnit.SECONDS) = webDriver.manage().timeouts().implicitlyWait(time,unit);

  /**
   * Stop the browser webdriver
   */
  def sessionStop(): Unit = synchronized {
    if (webDriver != null) {
      webDriver.close()
      try {
        webDriver.quit()
      } catch {
        case t: Throwable =>
          testlog.fine("Ignoring "+t.toString())
      }
      webDriver = null
    }

    chromeDriverService.map( cds => cds.stop() )
    chromeDriverService = None
  }

  /**
   * Set the quadrant for the window.
   * @param q the quadrant, values are 1,2,3,4.
   *          quadrant 1 is top left, goes around clockwise.
   */
  def setQuadrant( q: Int ) = {
    import Session._
    if (getScreenInfo) {
      val originx = origin.get.getX
      val originy = origin.get.getY
      val sizex = screenSize.get.getWidth-originx
      val sizey = screenSize.get.getHeight-originy
      val halfx = originx/2 + sizex/2
      val halfy = originy/2 + sizey/2
      q match {
        case 1 =>
          setPosition(originx,originy)
          setSize(halfx,halfy)
        case 2 =>
          setPosition(originx+halfx,originy)
          setSize(halfx,halfy)
        case 4 =>
          setPosition(originx,originy+halfy)
          setSize(halfx,halfy)
        case 3 =>
          setPosition(originx+halfx,originy+halfy)
          setSize(halfx,halfy)
        case _ =>
      }
    }
    this
  }

  /**
   * Set the quadrant for the window.  The size is set to the specified width and height
   * @param q the quadrant, values are 1,2,3,4.
   *          quadrant 1 is top left, goes around clockwise.
   * @param width
   * @param height
   */
  def setQuadrant( q: Int, width: Int, height: Int ) = {
    import Session._
    if (getScreenInfo) {
      val originx = origin.get.getX
      val originy = origin.get.getY
      val sizex = screenSize.get.getWidth-originx
      val sizey = screenSize.get.getHeight-originy
      val halfx = originx/2 + sizex/2
      val halfy = originy/2 + sizey/2
      q match {
        case 1 =>
          setPosition(originx,originy)
          setSize(width,height)
        case 2 =>
          setPosition(originx+halfx,originy)
          setSize(width,height)
        case 4 =>
          setPosition(originx,originy+halfy)
          setSize(width,height)
        case 3 =>
          setPosition(originx+halfx,originy+halfy)
          setSize(width,height)
        case _ =>
      }
    }
    this
  }

  /**
   * Set the position on the screen.  The coordinates are relative to the actual desktop.
   * The task bar is not included in the actual desktop.
   * @param x
   * @param y
   */
  def setPositionRelative( x: Int, y: Int ) = {
    import Session._
    if (getScreenInfo) {
      val originx = origin.get.getX
      val originy = origin.get.getY
      setPosition(originx+x,originy+y)
    }
    this
  }

  /**
   * Set the position on the screen using screen coordinates.
   * @param x
   * @param y
   */
  def setPosition( x: Int, y: Int ) = {
    webDriver.manage().window().setPosition(new Point(x,y))
    this
  }

  /**
   * Set the size of the window.
   * @param width
   * @param height
   */
  def setSize( width: Int, height: Int ) = {
    webDriver.manage().window().setSize(new Dimension(width,height))
    this
  }

  def saveDom( tofile: String ): Unit = {
    import com.example.pages.PageBrowser._
    try {
      scala.tools.nsc.io.File(tofile).writeAll( executeScript("return document.documentElement.outerHTML")(webDriver).toString() )
    } catch {
      case e: Exception =>
        testlog.warning("Exception trying to execute a script in browser", e)
    }
  }

  // from WebDriver
  def close(): Unit = webDriver.close()
  def findElement(by: org.openqa.selenium.By): org.openqa.selenium.WebElement = webDriver.findElement(by)
  def findElements(by: org.openqa.selenium.By): java.util.List[org.openqa.selenium.WebElement] = webDriver.findElements(by)
  def get(url: String): Unit = webDriver.get(url)
  def getCurrentUrl(): String = webDriver.getCurrentUrl
  def getPageSource(): String = webDriver.getPageSource
  def getTitle(): String = webDriver.getTitle
  def getWindowHandle(): String = webDriver.getWindowHandle
  def getWindowHandles(): java.util.Set[String] = webDriver.getWindowHandles
  def manage(): org.openqa.selenium.WebDriver.Options = webDriver.manage()
  def navigate(): org.openqa.selenium.WebDriver.Navigation = webDriver.navigate()
  def quit(): Unit = webDriver.quit()
  def switchTo(): org.openqa.selenium.WebDriver.TargetLocator = webDriver.switchTo()

}

object Session {

  val testlog = Logger[Session]

  private var screenSize: Option[Dimension] = None
  private var origin: Option[Point] = None

  private var screenInfoNotSupported = false

  /**
   * @return false if screenInfo is NOT supported, true if it is supported
   */
  def getScreenInfo( implicit webDriver: WebDriver ) = {
    if (screenSize.isEmpty || origin.isEmpty) {
      if (!screenInfoNotSupported) {
        synchronized {
          if (screenSize.isEmpty || origin.isEmpty) {
            if (!screenInfoNotSupported) {
              try {
                val window = webDriver.manage().window()
                window.maximize()
                screenSize = Some(window.getSize)
                origin = Some(window.getPosition)
              } catch {
                case x: WebDriverException =>
                  // maximize is not supported
                  screenInfoNotSupported = true
              }
            }
          }
        }
      }
    }
    !screenInfoNotSupported
  }

  /**
   * Get the specified property as either a java property or an environment variable.
   * If both are set, the java property wins.
   * @param name the property name
   * @return the property value wrapped in a Some object.  None property not set.
   */
  def getPropOrEnv( name: String ): Option[String] = sys.props.get(name) match {
    case v: Some[String] =>
      testlog.fine("getPropOrEnv: found system property: "+name+"="+v.get)
      v
    case None =>
      sys.env.get(name) match {
        case v: Some[String] =>
          testlog.fine("getPropOrEnv: found env var: "+name+"="+v.get)
          v
        case None =>
          testlog.fine("getPropOrEnv: did not find system property or env var: "+name)
          None
      }
  }

  val sessionCounter = new AtomicLong()
}

class DirectorSession extends Session
class CompleteSession extends Session
class TableSession( val table: String ) extends Session {
  val number = Id.tableIdToTableNumber(table)
}
