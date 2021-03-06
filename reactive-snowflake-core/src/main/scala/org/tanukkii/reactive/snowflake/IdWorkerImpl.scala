/** Copyright 2016 Yusuke Yasuda
  * Copyright 2010-2012 Twitter, Inc.
  *
  * You can find original works at
  * https://github.com/twitter/snowflake/blob/snowflake-2010/src/main/scala/com/twitter/service/snowflake/IdWorker.scala
  */

package org.tanukkii.reactive.snowflake


@SerialVersionUID(1L) case class InvalidSystemClock(message: String, lastTimestamp: Long, lastSequenceId: Long) extends Exception(message)

case class NextId(id: Option[Long], lastTimestamp: Long, nextSequence: Long)

private [snowflake] trait IdWorkerImpl {

  val workerId: Long
  val datacenterId: Long

  val twepoch = 1288834974657L

  val workerIdBits = 5L
  val datacenterIdBits = 5L
  val maxWorkerId = -1L ^ (-1L << workerIdBits)
  val maxDatacenterId = -1L ^ (-1L << datacenterIdBits)
  val sequenceBits = 12L
  val maxSequenceId = -1L ^ (-1L << sequenceBits)

  val workerIdShift = sequenceBits
  val datacenterIdShift = sequenceBits + workerIdBits
  val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
  val sequenceMask = -1L ^ (-1L << sequenceBits)

  final def nextId(timestamp: Long, lastTimestamp: Long, currentSequence: Long): NextId = {

    if (timestamp < lastTimestamp) {
      throw InvalidSystemClock("Clock moved backwards.  Refusing to generate id for %d milliseconds".format(
        lastTimestamp - timestamp
      ), lastTimestamp, currentSequence)
    }

    require(currentSequence < sequenceMask + 1)

    val sequence: Option[Long] = if (lastTimestamp == timestamp) {
      val s = (currentSequence + 1) & sequenceMask
      if (s == 0) None else Some(s)
    } else {
      Some(0)
    }

    val id = sequence.map { s =>
      ((timestamp - twepoch) << timestampLeftShift) |
        (datacenterId << datacenterIdShift) |
        (workerId << workerIdShift) |
        s
    }

    NextId(id, timestamp, sequence.getOrElse(maxSequenceId))
  }

  def timeGen(): Long = System.currentTimeMillis()
}
