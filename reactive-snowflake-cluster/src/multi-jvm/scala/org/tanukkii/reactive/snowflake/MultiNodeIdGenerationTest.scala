package org.tanukkii.reactive.snowflake

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import scala.concurrent.duration._

class MultiNodeIdGenerationTestMultiJvmNode1 extends MultiNodeIdGenerationTest
class MultiNodeIdGenerationTestMultiJvmNode2 extends MultiNodeIdGenerationTest
class MultiNodeIdGenerationTestMultiJvmNode3 extends MultiNodeIdGenerationTest
class MultiNodeIdGenerationTestMultiJvmNode4 extends MultiNodeIdGenerationTest

class MultiNodeIdGenerationTest extends MultiNodeSpec(MultiNodeIdGenerationTestConfig)
with STMultiNodeSpec with ImplicitSender {
  import MultiNodeIdGenerationTestConfig._

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

  def initialParticipants: Int = roles.size


  def join(from: RoleName, to: RoleName): Unit = {
    runOn(from) {
      Cluster(system) join node(to).address
      startSharding()
    }
    enterBarrier(from.name + "-joined")
  }

  def startSharding(): Unit = ClusterSharding(system).start(
    typeName = ShardedIdRouter.shardName,
    entityProps = ShardedIdRouter.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = ShardedIdRouter.idExtractor,
    extractShardId = ShardedIdRouter.shardResolver
  )

  "id cluster" must {

    "join cluster" in within(15 seconds) {
      join(worker1_1, worker1_1)
      join(worker1_2, worker1_1)
      join(worker2_1, worker1_1)
      join(worker2_2, worker1_1)
      enterBarrier("join cluster")
    }

    "generate id" in {
      runOn(worker1_1) {
        val datacenterMask = 0x00000000003E0000L
        val datacenterId = 0x1F
        val idRouter = ClusterSharding(system).shardRegion(ShardedIdRouter.shardName)
        val client = system.actorOf(IdClient.props(datacenterId, idRouter))
        within(10 second) {
          for (i <- 0 until 100) {
            client ! IdClientProtocol.GenerateId
          }
          receiveN(100).map {
            case IdWorkerProtocol.IdGenerated(id) => {
              ((id & datacenterMask) >> 17) must be(datacenterId)
            }
          }
        }
      }
      enterBarrier("generate id")
    }
  }
}
