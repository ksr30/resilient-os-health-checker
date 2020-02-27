import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.knoldus.Supervisor
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._
import scala.language.postfixOps

class TestSpec extends TestKit(ActorSystem("firstActorSystem")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A simple actor " should {
    "send back" in{
      within(20 second){
      val testActor = system.actorOf(Props[Supervisor])
      val message = "/home/knoldus/Documents/resilient-os-health-checker/src/main/resources/LogFiles"
      testActor ! message
      val expectedMessage=791
      expectMsg(expectedMessage)
  }}
}
}
