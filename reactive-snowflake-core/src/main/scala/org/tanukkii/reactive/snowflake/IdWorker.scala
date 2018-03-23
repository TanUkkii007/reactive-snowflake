package org.tanukkii.reactive.snowflake

import akka.actor.{ActorLogging, Props, ActorRef, Actor}

case class DatacenterId(value: Long)

case class WorkerId(value: Long)

object IdWorkerProtocol {
  case class GenerateId(from: ActorRef)
  case class IdGenerated(value: Long)
}

class IdWorker(dcId: DatacenterId, wId: WorkerId) extends Actor
with IdWorkerImpl with ActorLogging {
  import IdWorkerProtocol._

  val datacenterId: Long = dcId.value
  val workerId: Long = wId.value

  var sequenceId: Long = 0L

  var lastTimestamp = -1L

  def receive: Receive = {
    case msg@GenerateId(replyTo) => {
      val NextId(idOpt, timestamp, nextSequence) = nextId(timeGen(), lastTimestamp, sequenceId)
      sequenceId = nextSequence
      lastTimestamp = timestamp
      idOpt match {
        case Some(id) => replyTo ! IdGenerated(id)
        case None => {
          log.debug("retrying to avoid id duplication")
          self ! msg
        }
      }
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    message match {
      case Some(msg: GenerateId) =>
        self forward msg
      case _ =>
    }
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    reason match {
      case InvalidSystemClock(_, timestamp, lastSequenceId) =>
        lastTimestamp = timestamp
        sequenceId = lastSequenceId
      case _ =>
    }
    super.postRestart(reason)
  }
}

object IdWorker {
  def props(datacenterId: DatacenterId, workerId: WorkerId): Props = Props(new IdWorker(datacenterId, workerId))

  def name(datacenterId: Long, workerId: Long): String = s"idWorker-$datacenterId-$workerId"
}