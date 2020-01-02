package com.github.thebridsk.bridge.server.test

import org.scalatest.Sequential

/**
 * @author werewolf
 */
class AllUnitTests extends Sequential(

  new TestStartLogging,
  new TestCacheStore,
  new TestCacheStoreForFailures,
  new TestBoardSetsAndHands,
  new SwaggerSpec,
  new TestRoute,
  new TestScoring,
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
