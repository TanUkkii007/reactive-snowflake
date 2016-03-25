package org.tanukkii.reactive.snowflake

import akka.actor.{Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate

object ShardedIdRouterProtocol {
  case object StopSending
}

class ShardedIdRouter extends IdRouter {
  import ShardedIdRouterProtocol._

  override def unhandled(msg: Any) = msg match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopSending)
    case StopSending => context.stop(self)
    case other => super.unhandled(other)
  }
}

object ShardedIdRouter {
  import IdRouterProtocol._

  def props: Props = Props(new ShardedIdRouter)

  def name: String = "IdRouter"

  val shardName: String = "IdRouter"

  val idExtractor: ShardRegion.ExtractEntityId = {
    case cmd@GenerateId(datacenterId, workerId) => (s"$datacenterId-$workerId", cmd)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case cmd@GenerateId(datacenterId, workerId) => s"${datacenterId % 32}-${workerId % 32}"
  }
}