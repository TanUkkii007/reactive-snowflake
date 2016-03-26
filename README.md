# reactive-snowflake

Akka based id generation service with the same algorithm with [Twitter Snowflake](https://github.com/twitter/snowflake)


## ID format

Same as Snowflake.

## Usage

```scala
val dcId1 = DatacenterId(1L)
val workerId1 = WorkerId(1L)

val idWorker = system.actorOf(IdWorker.props(dcId1, workerId1))

idWorker ! GenerateId(testActor)

expectMsgType[IdGenerated]

```

## Cluster Support

To automatically manage workerId you can use Akka cluster shardhing.

```scala
val datacenterId = 0x01

val idRouter = ClusterSharding(system).shardRegion(ShardedIdRouter.shardName)

val client = system.actorOf(IdClient.props(datacenterId, idRouter))

client.ask(IdClientProtocol.GenerateId).mapTo[IdGenerated].foreach(println)

```