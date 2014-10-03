package sample.hello

import java.nio.channels.FileChannel

import akka.actor._
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import java.io._
import java.io.File
import scala.io.Source
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic

/**
 * Created by root on 9/30/14.
 */
case class file_properties(filename :File,fileid :Int)

object FileIndexer{
  def main(args: Array[String]): Unit = {
   var path_filename=new File(" ")
   var counter=0
   val current_directory=new File("/root/Desktop/")
   val indexingSystem= ActorSystem("indexingsystem")//,ConfigFactory.load(application_is_remote))
   val actor_ref_file = indexingSystem.actorOf(Props[FileReceiver],"indexing")
   for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      path_filename=new File(file.toString())//current_directory.toString.+(file.toString))
      //println(path_filename)
      counter+=1
      actor_ref_file ! file_properties(path_filename,counter)
   }
  }
}

class FileReceiver extends Actor{
  def receive ={
    case file_properties(filename, fileid) =>
      val router ={
         val routees=Vector.fill(5){
            val lineseparator=context.actorOf(Props[LineSeparator],"lineSeparating")
            context watch lineseparator
            ActorRefRoutee(lineseparator)
         }
         Router(RoundRobinRoutingLogic(), routees)
      }

      //val lineseparator=context.actorOf(Props[LineSeparator], "lineSeparating")
       //println(filename.length().toString())
       //println("file's length is"+filename.length())
       for(line <- Source.fromFile(filename).getLines()){
          router.route(line, sender())
       }

    case _ =>
      println("I got nothing")

  }


}

class LineSeparator extends Actor with ActorLogging {
  def receive ={

    case line :String =>

    case _ =>
      println("No line received")

  }

}