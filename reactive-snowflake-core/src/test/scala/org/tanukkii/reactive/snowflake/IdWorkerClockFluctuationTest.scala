package org.tanukkii.reactive.snowflake

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._

object FluctuatingIdWorker {
  def props(dcId: DatacenterId, workerId: WorkerId): Props = Props(new IdWorker(dcId, workerId) {
    override def timeGen(): Long = {
      super.timeGen() + (if (math.random > 0.6) -1 else 0) * 100
    }
  })
}

class IdWorkerClockFluctuationTest extends TestKit(ActorSystem("IdWorkerClockFluctuationTest"))
  with WordSpecLike
  with MustMatchers with ImplicitSender {
  import IdWorkerProtocol._

  "idWorker under fluctuating clock" must {

    val dcId1 = DatacenterId(1L)
    val workerId1 = WorkerId(1L)

    "generate only unique ids, even when time goes backwards" in {
      val n = 1000

      val idWorker = system.actorOf(FluctuatingIdWorker.props(dcId1, workerId1))

      (1 to n).foreach { _ =>
        idWorker ! GenerateId(testActor)
      }

      within(10 seconds) {
        receiveN(n).asInstanceOf[Seq[IdGenerated]].foldLeft(0L) { (prev, current) =>
          prev must be < current.value
          current.value
        }
      }
    }
  }

}