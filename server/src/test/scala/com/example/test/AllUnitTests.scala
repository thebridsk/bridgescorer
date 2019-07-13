package com.github.thebridsk.bridge.test

import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllUnitTests extends Sequential(

  new TestStartLogging,
  new TestCacheStore,
  new TestCacheStoreForFailures,
  new MyServiceSpec,
  new TestBoardSetsAndHands,
  new SwaggerSpec,
  new TestGetFromResource,
  new TestRoute,
  new TestScoring,
  new TestWebJarFinder,
  new TestResourceStore,
  new TestDuplicateRestSpec,
  new TestDuplicateScore,
  new TestChicagoScoring,
  new TestFileStore,
  new TestLoggingWebsocket,
  new TestWinnerSets,
  new TestCacheStoreWithRoute,
  new TestVersionedInstance,
  new TestDuplicateWebsocket,
  new TestGraphQL,
  new TestRemoteLoggingConfig,
  new TestPlayerComparisonStats
)
