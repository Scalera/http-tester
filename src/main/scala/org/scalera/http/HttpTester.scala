package org.scalera.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Random, Try}

object HttpTester extends App {

  val parallelRequests = 10
  val url = "http://es.wikipedia.org"

  implicit val system = ActorSystem("http-tester")
  implicit val materializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(system))
  lazy val http = Http(system)

  type StatusCod = Int

  def log(futureN: Int, msg: String): Unit = {
    println(s"[$futureN] $msg")
  }

  def request()(implicit ec: ExecutionContext): Future[HttpResponse] =
    http.singleRequest(
      HttpRequest(
        uri = url,
        method = HttpMethods.GET)
    )

  def workerJob(workerId: Int)(implicit ec: ExecutionContext): Unit = {
    Try{
      val t1 = System.currentTimeMillis()
      val response = Await.result(request(), Duration.Inf)
      (System.currentTimeMillis() - t1, response)
    }.map{
      case (time, response) =>
        log(workerId, s"$time ms : ${response.status.intValue()}")
    }.recover{
      case e =>
        log(workerId, s"${e.getMessage}")
    }
  }

  {
    import scala.concurrent.ExecutionContext.Implicits.global
    (1 to parallelRequests).foreach { id =>
      Future{
        while(true){
          workerJob(id)
          Thread.sleep(500 + Random.nextInt(250))
        }
      }
    }
  }

  //  Clean shutdown

  Runtime.getRuntime.addShutdownHook(new Thread{
    import scala.concurrent.ExecutionContext.Implicits.global
    override def run(): Unit = {
      Await.ready(
        for {
          _ <- http.shutdownAllConnectionPools()
          _ <- Future(materializer.shutdown())
          _ <- system.terminate()
        } yield (),
        Duration.Inf)
      println("Gracefully stopped")
    }
  })

}
