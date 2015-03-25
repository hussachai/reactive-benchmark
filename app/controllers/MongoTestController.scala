package controllers

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClientURI, MongoClient}
import play.Play
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.Count
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Hussachai on 3/24/2015.
 */
object MongoTestController extends Controller with MongoController{
  val config = Play.application().configuration
  val asyncDb = db
  val syncDb =  MongoClient(MongoClientURI(config.getString("mongodb.uri"))).getDB("employees")
  val collectionName = "employees"

  case class EmployeeCount(male: Long, female: Long, startsWithA: Long)
  implicit val employeeCountFormat = Json.format[EmployeeCount]

  /*
    Async native. Parallelized executution.
   */
  def asyncNative1 = Action.async {
    val allEmpCountF = asyncDb.command(Count(collectionName))
    val femaleEmpCountF = asyncDb.command(Count(collectionName, Some(BSONDocument("gender" -> "M"))))
    val startsWithACountF = asyncDb.command(Count(collectionName, Some(BSONDocument("firstName" -> BSONDocument("$regex" -> "^H.*")))))
    for{
      allEmpCount <- allEmpCountF
      femaleEmpCount <- femaleEmpCountF
      startsWithACount <- startsWithACountF
    } yield{
      Ok(Json.toJson(EmployeeCount(allEmpCount - femaleEmpCount, femaleEmpCount, startsWithACount)))
    }
  }

  /*
   Async native. Serialized execution.
   */
  def asyncNative2 = Action.async {
    for{
      allEmpCount <- asyncDb.command(Count(collectionName))
      femaleEmpCount <- asyncDb.command(Count(collectionName, Some(BSONDocument("gender" -> "M"))))
      startsWithACount <- asyncDb.command(Count(collectionName, Some(BSONDocument("firstName" -> BSONDocument("$regex" -> "^H.*")))))
    } yield{
      Ok(Json.toJson(EmployeeCount(allEmpCount - femaleEmpCount, femaleEmpCount, startsWithACount)))
    }
  }

  def asyncFuture1 = Action.async{
    val allEmpCountF = Future(syncDb.getCollection(collectionName).count())
    val femaleEmpCountF = Future(syncDb.getCollection(collectionName).count(MongoDBObject("gender" -> "M")))
    val startsWithACountF = Future(syncDb.getCollection(collectionName).count(MongoDBObject("firstName" -> MongoDBObject("$regex" -> "^H.*"))))
    for{
      allEmpCount <- allEmpCountF
      femaleEmpCount <- femaleEmpCountF
      startsWithACount <- startsWithACountF
    } yield{
      Ok(Json.toJson(EmployeeCount(allEmpCount - femaleEmpCount, femaleEmpCount, startsWithACount)))
    }
  }

  def asyncFuture2 = Action.async{
    for{
      allEmpCount <- Future(syncDb.getCollection(collectionName).count())
      femaleEmpCount <- Future(syncDb.getCollection(collectionName).count(MongoDBObject("gender" -> "M")))
      startsWithACount <- Future(syncDb.getCollection(collectionName).count(MongoDBObject("firstName" -> MongoDBObject("$regex" -> "^H.*"))))
    } yield{
      Ok(Json.toJson(EmployeeCount(allEmpCount - femaleEmpCount, femaleEmpCount, startsWithACount)))
    }
  }

  def sync = Action {
    val allEmpCount = syncDb.getCollection(collectionName).count()
    val femaleEmpCount = syncDb.getCollection(collectionName).count(MongoDBObject("gender" -> "M"))
    val startsWithACount = syncDb.getCollection(collectionName).count(MongoDBObject("firstName" -> MongoDBObject("$regex" -> "^H.*")))
    Ok(Json.toJson(EmployeeCount(allEmpCount - femaleEmpCount, femaleEmpCount, startsWithACount)))
  }
}
