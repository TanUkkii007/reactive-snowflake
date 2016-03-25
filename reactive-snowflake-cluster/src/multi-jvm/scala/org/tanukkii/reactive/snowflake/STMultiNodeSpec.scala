package org.tanukkii.reactive.snowflake

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{MustMatchers, WordSpecLike, BeforeAndAfterAll}

trait STMultiNodeSpec extends MultiNodeSpecCallbacks
with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}
