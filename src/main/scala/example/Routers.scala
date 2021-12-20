package part5infra
import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.io.Udp.SO
import akka.routing._
import com.typesafe.config.ConfigFactory

object Routers extends App {
  class Master extends Actor {
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    private val router = Router(RoundRobinRoutingLogic(), slaves)
    override def receive: Receive = {
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      case message =>
        router.route(message, sender())
    }
  }
  class Slave extends Actor with ActorLogging {
    override def receive: Receive = { case message =>
      log.info(message.toString)
    }
  }
  val system =
    ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])
  for (i <- 1 to 10)
    master ! s"[$i] Hello from the world"
}
