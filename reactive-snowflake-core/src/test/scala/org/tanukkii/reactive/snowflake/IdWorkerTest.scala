package org.tanukkii.reactive.snowflake

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}
import scala.concurrent.duration._

class IdWorkerTest extends TestKit(ActorSystem("IdWorkerTest"))
with WordSpecLike with MustMatchers {
  import IdWorkerProtocol._

  "idWorker" must {

    val dcId1 = DatacenterId(1L)
    val workerId1 = WorkerId(1L)

    "generate id" in {
      val idWorker = system.actorOf(IdWorker.props(dcId1, workerId1))
      idWorker ! GenerateId(testActor)
      expectMsgType[IdGenerated]
    }

    "generate increasing ids" in {
      val idWorker = system.actorOf(IdWorker.props(dcId1, workerId1))

      for (i <- 1 to 100) {
        idWorker ! GenerateId(testActor)
      }
      within(5 seconds) {
        receiveN(100).asInstanceOf[Seq[IdGenerated]].foldLeft(0L) { (prev, current) =>
          prev must be < current.value
          current.value
        }
      }
    }

    "generate 1 million ids quickly" in {
      val idWorker = system.actorOf(IdWorker.props(dcId1, workerId1))
      val t = System.currentTimeMillis
      for (i <- 1 to 1000000) {
        idWorker ! GenerateId(testActor)
      }
      receiveN(1000000)
      val t2 = System.currentTimeMillis
      println("generated 1000000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0/(t2-t)))
    }
  }
}
