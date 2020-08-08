package com.github.thebridsk.bridge.test

import org.scalatest.Sequential

/**
  * @author werewolf
  */
class AllSSLTests
    extends Sequential(
      new CertTest
    ) {}
