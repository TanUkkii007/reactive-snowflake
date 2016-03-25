package org.tanukkii.reactive.snowflake

import akka.actor.{ActorRef, Props, Actor}
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding}

class ShardedIdGenerator extends Actor {
  ClusterSharding(context.system).start(
    typeName = ShardedIdRouter.shardName,
    entityProps = ShardedIdRouter.props,
    settings = ClusterShardingSettings(context.system),
    extractEntityId = ShardedIdRouter.idExtractor,
    extractShardId = ShardedIdRouter.shardResolver
  )

  def shardedIdRouter: ActorRef = {
    ClusterSharding(context.system).shardRegion(ShardedIdRouter.shardName)
  }

  def receive: Receive = {
    case msg => {
      shardedIdRouter forward msg
    }
  }

}

object ShardedIdGenerator {
  def props: Props = Props(new ShardedIdGenerator)

  def name: String = "idGenerator"
}