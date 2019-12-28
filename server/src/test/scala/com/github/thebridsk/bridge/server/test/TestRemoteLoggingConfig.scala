package com.github.thebridsk.bridge.server.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.server.logging.RemoteLoggingConfig
import com.github.thebridsk.bridge.server.yaml.YamlSupport

object TestRemoteLoggingConfig {

  val testdata = """
configs:
  ipad:
    all:
      loggers:
        - "[root]=ALL"
      appenders:
        - "console=ALL"
        - "server=ALL,bridge"
    info:
      loggers:
        - "[root]=INFO"
      appenders:
        - "console=INFO"
        - "server=INFO,bridge"
    noserver:
      loggers:
        - "[root]=ALL"
      appenders:
        - "console=ALL"
    nologging:
      loggers:
        - "[root]=NONE"
      appenders:
        - "console=NONE"
  default:
    all:
      loggers:
        - "[root]=ALL"
      appenders:
        - "console=ALL"
        - "server=ALL,bridge"
"""
}

class TestRemoteLoggingConfig extends AnyFlatSpec with Matchers {

  behavior of "Duplicate Bridge Scoring"

  it should "parse the yaml file" in {
    val config = RemoteLoggingConfig.read(TestRemoteLoggingConfig.testdata)

    config.browserNames() must equal ( Set("ipad","default") )
    config.browserConfigs("ipad") must equal ( Set("all","info","noserver","nologging") )
    config.browserConfigs("default") must equal ( Set("all") )

    config.browserConfig("xxx", "all") mustBe None

    config.browserConfig("ipad", "xxx") mustBe None

    val allipad = config.browserConfig("ipad", "all")
    allipad must not be None

    allipad.foreach { c =>
      c.appenders mustBe List("console=ALL","server=ALL,bridge")
      c.loggers mustBe List("[root]=ALL")
    }

  }

  it should "write out yaml file" in {
    val config = RemoteLoggingConfig.read(TestRemoteLoggingConfig.testdata)

    val yaml = YamlSupport.writeYaml(config)
    println(yaml)

  }

  it should "get the default remote logging configuration" in {
    RemoteLoggingConfig.getDefaultRemoteLoggerConfig() match {
      case Some(rlc) =>
        rlc.browserConfig("default", "default") must not be None
      case None =>
        fail("Did not find default remote logging configuration")
    }

  }
}
