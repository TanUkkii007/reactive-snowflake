package org.tanukkii.reactive.snowflake

import org.scalatest.{MustMatchers, WordSpecLike}

class IdWorkerImplTest extends WordSpecLike with MustMatchers {
  "IdWorkerImpl" must {

    val workerMask     = 0x000000000001F000L
    val datacenterMask = 0x00000000003E0000L
    val timestampMask  = 0xFFFFFFFFFFC00000L

    "properly mask worker id" in {
      val wId = 0x1FL

      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long = wId
      }
      for (i <- 1 to 1000) {
        val lastTimestamp = System.currentTimeMillis()
        val id = worker.nextId(System.currentTimeMillis(), lastTimestamp, 0L)
        ((id.id.get & workerMask) >> 12) must be(wId)
      }
    }

    "properly mask dc id" in {
      val wId = 0
      val dcId = 0x1F

      val worker = new IdWorkerImpl {
        override val datacenterId: Long = dcId
        override val workerId: Long = wId
      }

      val lastTimestamp = System.currentTimeMillis()

      val id = worker.nextId(System.currentTimeMillis(), lastTimestamp, 0L)
      ((id.id.get & datacenterMask) >> 17) must be(dcId)
    }

    "properly mask timestamp" in {
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long = 1L
      }

      for (i <- 1 to 100) {
        val t = System.currentTimeMillis
        val id = worker.nextId(t, t - 1L, 0L)
        ((id.id.get & timestampMask) >> 22) must be(t - worker.twepoch)
      }
    }

    "roll over sequence id" in {
      val wId = 4
      val dcId = 4
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = dcId
        override val workerId: Long = wId
      }
      var sequence: Long = 0x000
      val endSequence = 0xFFF

      val timestamp = System.currentTimeMillis()

      for (i <- sequence until endSequence) {
        val id = worker.nextId(timestamp, timestamp, sequence)
        ((id.id.get & workerMask) >> 12) must be(wId)
        sequence = id.nextSequence
      }
    }

    "does not generate ids over max sequence" in {
      val wId = 4
      val dcId = 4
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = dcId
        override val workerId: Long = wId
      }

      val timestamp = System.currentTimeMillis()

      val id = worker.nextId(timestamp, timestamp, 0xFFF)
      id.id must be(None)

    }

    "generate increasing ids" in {
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long = 1L
      }

      var lastId = 0L
      var currentSequence = 0L

      for (i <- 1 to 100) {
        val lastTimestamp = System.currentTimeMillis()
        val id = worker.nextId(System.currentTimeMillis(), lastTimestamp, currentSequence)
        id.id.get must be > lastId
        lastId = id.id.get
        currentSequence = id.nextSequence
      }
    }

    "generate 1 million ids quickly" in {
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long = 1L
      }
      var lastTimestamp = -1L
      val t = System.currentTimeMillis
      for (i <- 1 to 1000000) {
        val currentTimestamp = System.currentTimeMillis()
        worker.nextId(currentTimestamp, lastTimestamp, 0L)
        lastTimestamp = currentTimestamp
      }
      val t2 = System.currentTimeMillis
      println("generated 1000000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0/(t2-t)))
      1 must be(1)
    }
  }
}
