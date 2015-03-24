package controllers

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import play.api.Logger
import play.api.mvc._
import redis.RedisClient
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{PMessage, Message}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object PubsubController extends Controller {

  val log = Logger(getClass)

  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient()

  def index = Action { implicit request =>
    Ok(views.html.pubsub())
  }

  def subscribe = WebSocket.tryAcceptWithActor[String, String] { request =>
    def props(channel: String)(out: ActorRef) = Props(classOf[SubscribeActor], out, Seq(channel), Nil)
      .withDispatcher("rediscala.rediscala-client-worker-dispatcher")
    Future.successful(request.getQueryString("channel") match {
      case None => Left(Forbidden)
      case Some(channel) => Right(props(channel))
    })
  }

  def publish(channel: String) = Action.async { implicit request =>
    request.body.asFormUrlEncoded.flatMap{ params =>
      params.get("message").map{ message =>
        redis.publish(channel, message.head).map{ n =>
          if(n > 0) {
            log.debug(s"Number of subscriber: $n")
            Ok(n.toString)
          }
          else {
            log.debug("No recipients")
            Gone
          }
        }
      }
    }.getOrElse(Future.successful(BadRequest("No content received")))
  }

  class SubscribeActor(out: ActorRef, channels: Seq[String] = Nil, patterns: Seq[String] = Nil) extends RedisSubscriberActor(
    new InetSocketAddress(redis.host, redis.port), channels, patterns) {

    def onMessage(message: Message) {
      out ! message.data
    }

    def onPMessage(pmessage: PMessage) {}
  }

}