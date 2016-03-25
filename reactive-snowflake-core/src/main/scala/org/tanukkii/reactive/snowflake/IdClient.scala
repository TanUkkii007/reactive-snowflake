package org.tanukkii.reactive.snowflake

import akka.actor.{ActorRef, Props, Actor}

object IdClientProtocol {
  case object GenerateId
}

class IdClient(datacenterId: Int, idGenerator: ActorRef) extends Actor {
  import IdClientProtocol._

  var workerId: Long = 0

  def receive: Receive = {
    case GenerateId => {
      idGenerator forward IdRouterProtocol.GenerateId(datacenterId, workerId % 32 toInt)
      workerId += 1
    }
  }
}

object IdClient {
  def props(datacenterId: Int, idGenerator: ActorRef): Props = Props(new IdClient(datacenterId, idGenerator))
}