package sample.hello
import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import com.typesafe.config.ConfigFactory
import java.io._
import scala.collection.JavaConversions._

/**
 * Created by root on 9/30/14.
 */
object FileIndexer{
  def main(args: Array[String]): Unit = {
   var path_filename=""
   val current_directory=new File("/root/Desktop/")
   val indexingSystem= ActorSystem("indexingsystem")//,ConfigFactory.load(application_is_remote))
   val actor_ref_file = indexingSystem.actorOf(Props[FileReceiver],"indexing")
   for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      path_filename=current_directory.toString.+(file.toString)
      //println(path_filename)
      actor_ref_file ! path_filename
   }
  }
}

class FileReceiver extends Actor{
  def receive ={
    case filename: String =>
      println(filename)
    case _ =>
      println("I got nothing")

  }

}
