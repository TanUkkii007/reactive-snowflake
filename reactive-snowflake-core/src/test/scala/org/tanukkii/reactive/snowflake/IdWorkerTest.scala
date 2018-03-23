package org.tanukkii.reactive.snowflake

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._

class EasyTimeWorker(dcId: DatacenterId, wId: WorkerId, timeSequence: Seq[Long]) extends IdWorker(dcId, wId) {
  val timeSeqIter = timeSequence.iterator
  override def timeGen(): Long = {
    timeSeqIter.next()
  }
}

class WakingIdWorker(dcId: DatacenterId, wId: WorkerId, lastTime: Long, timeSequence: Seq[Long]) extends EasyTimeWorker(dcId, wId, timeSequence) {
  lastTimestamp = lastTime
  sequenceId = 4095L
}

/**
  * Some of this tests are taken from Snowflake.
  * https://github.com/twitter/snowflake/blob/snowflake-2010/src/test/scala/com/twitter/service/snowflake/IdWorkerSpec.scala
  */
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

    "sleep if we would rollover twice in the same millisecond" in {
      val timestamp = System.currentTimeMillis()
      val idWorker = system.actorOf(Props(new WakingIdWorker(dcId1, workerId1, timestamp, List(timestamp, timestamp, timestamp + 1L))))
      idWorker ! GenerateId(testActor)
      val id1 = expectMsgType[IdGenerated].value
      extractTimestamp(id1) must be(timestamp + 1)
      extractSequenceId(id1) must be(0L)
    }

    "generate only unique ids" in {
      val idWorker = system.actorOf(IdWorker.props(DatacenterId(31L), WorkerId(31L)))
      idWorker ! GenerateId(testActor)
      expectMsgType[IdGenerated]
    }

    "generate ids over 50 billion" in {
      val n = 2000000
      val idWorker = system.actorOf(IdWorker.props(DatacenterId(0L), WorkerId(0L)))
      1 to n foreach { _ =>
        idWorker ! GenerateId(testActor)
      }

      val generated = receiveN(n, 10 seconds).collect {
        case IdGenerated(id) => id
      }

      generated.size must be(n)
    }
  }

  def extractWorkerId(value: Long): Long = {
    val workerIdBits = 5L
    val sequenceBits = 12L
    val mask = ~(-1L << workerIdBits)
    (value >> sequenceBits) & mask
  }
  def extractSequenceId(value: Long): Long = {
    val sequenceBits = 12L
    val mask = ~(-1L << sequenceBits)
    value & mask
  }
  def extractTimestamp(value: Long): Long = {
    val workerIdBits = 5L
    val datacenterIdBits = 5L
    val sequenceBits = 12L
    val twepoch = 1288834974657L
    (value >> (datacenterIdBits + workerIdBits + sequenceBits)) + twepoch
  }

}
