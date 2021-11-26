package com.github.thebridsk.bridge.server.manualtest

import java.net.NetworkInterface

import scala.jdk.CollectionConverters._
import java.net.Inet4Address
import com.github.thebridsk.utilities.main.MainNoArgs

object GetIP extends MainNoArgs {

  def execute(): Int = {
    NetworkInterface.getNetworkInterfaces.asScala
      .filter { x => x.isUp() && !x.isLoopback() }
      .foreach { x =>
        println("Name: " + x.getName)
        println("Display: " + x.getDisplayName)
        println("isLoopback: " + x.isLoopback())
        println("isVirtual: " + x.isVirtual())
        println("isUp: " + x.isUp())
        println("IP: " + x.getInetAddresses.asScala.mkString("{", "}, {", "}"))
        println()
      }

    val x = NetworkInterface.getNetworkInterfaces.asScala.filter { x =>
      x.isUp() && !x.isLoopback()
    }
    val y = x.flatMap { ni => ni.getInetAddresses.asScala }
    val z = y.filter { x => x.isInstanceOf[Inet4Address] }.map { x =>
      x.getHostAddress
    }
    println("Found: " + z.mkString(","))
    0
  }
}
