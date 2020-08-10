package com.github.thebridsk.bridge.server.manualtest

import org.openqa.selenium.firefox.FirefoxDriver

object SeleniumFirefoxTest {

  def main(args: Array[String]): Unit = {
//     System.setProperty("webdriver.gecko.driver", "geckodriver.exe");
    val driver = new FirefoxDriver();
    driver.navigate().to("https://www.google.com");
    driver.manage().window().maximize();
    println(s"Title is ${driver.getTitle}")
    driver.quit();

  }
}
