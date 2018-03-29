import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

// The class which runs the fight
class Engine(val channel: Channel[SceneStatus], val setup: SceneSetup) {

    private val mustStop = new AtomicBoolean(false)
    private var task: Future[Unit] = _
    private var status: SceneStatus = _

    // Start the engine asynchronously
    def start(): Unit = {
        status = new SceneStatus
        status.turnCount = 1
        status.actors =
            for { actor <- setup.actors }
            yield ActorStatus.from(actor)

        channel.push(status)

        task = Future { run() } (ExecutionContext.global)
    }

    // Ask the engine to stop, then wait him to finish
    def stop(): Unit = {
        mustStop.set(true)
        Await.result(task, 5.second)
    }

    private def run(): Unit = {
        var time = System.currentTimeMillis()
        while (!mustStop.get()) {
            val t2 = System.currentTimeMillis()
            if (t2 - time >= 1000) {
                time = t2
                channel.push(status)
            }
            Thread.sleep(10)
        }
    }

}
