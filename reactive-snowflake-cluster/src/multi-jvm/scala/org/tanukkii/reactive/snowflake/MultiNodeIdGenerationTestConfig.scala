package org.tanukkii.reactive.snowflake

import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.ConfigFactory

object MultiNodeIdGenerationTestConfig extends MultiNodeConfig {
  val worker1_1 = role("worker-1-1")
  val worker1_2 = role("worker-1-2")
  val worker2_1 = role("worker-2-1")
  val worker2_2 = role("worker-2-2")

  commonConfig(ConfigFactory.parseString(
    """
      |akka.cluster.metrics.enabled=off
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.loglevel = INFO
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
    """.stripMargin)
  )
}