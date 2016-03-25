package org.tanukkii.reactive.snowflake

import akka.actor.{ActorRef, ActorLogging, Props, Actor}

object IdRouterProtocol {
  case class GenerateId(datacenterId: Int, workerId: Int)
}

class IdRouter extends Actor with ActorLogging {
  import IdRouterProtocol._

  def receive: Receive = {
    case cmd@GenerateId(datacenterId, workerId) => {
      require(datacenterId >= 0 && datacenterId < 32 && workerId >= 0 && workerId < 32)
      val childName = IdWorker.name(datacenterId, workerId)
      context.child(childName).fold(createIdWorker(datacenterId, workerId, childName) forward IdWorkerProtocol.GenerateId(sender())) { idWorker =>
        idWorker forward IdWorkerProtocol.GenerateId(sender())
      }
    }
  }

  def createIdWorker(datacenterId: Int, workerId: Int, name: String): ActorRef = {
    log.info("creating IdWorker with datacenterId {}, workerId {}", datacenterId, workerId)
    context.actorOf(IdWorker.props(DatacenterId(datacenterId), WorkerId(workerId)), name)
  }
}

object IdRouter {
  def props: Props = Props(new IdRouter)

  def name: String = "idRouter"
}