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
case class routingmessages(fileline :String, counter :Int)

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

      /* Creating A router to route "Workers" to ectract citations for each line of the file */
      val router ={
         val routees=Vector.fill(5){
            val lineseparator=context.actorOf(Props[LineSeparator])
            context watch lineseparator
            ActorRefRoutee(lineseparator)
         }
         Router(RoundRobinRoutingLogic(), routees)
      }
      /* Creating A router to route "Workers" to ectract citations for each line of the file */

      //val lineseparator=context.actorOf(Props[LineSeparator], "lineSeparating")
       //println(filename.length().toString())
       //println("file's length is"+filename.length())
       var counter=0
       for(line <- Source.fromFile(filename).getLines()){
          router.route(routingmessages(line,counter), sender())
       }

    case _ =>
      println("I got nothing")

  }


}

class LineSeparator extends Actor with ActorLogging {
  def receive ={

    case routingmessages(line,counter) =>
    val references_ar=for( i <-0 to (line.length()-1) if(line.charAt(i) == '[') )yield i
    val references_de=for( i <-0 to (line.length()-1) if(line.charAt(i) == ']') )yield i

    if (!references_ar.isEmpty){
      if(references_ar.length.>(references_de.length)){
        val reference_array=for(i <- 0 to (references_ar.length -2) )yield line.substring(references_ar(i),references_de(i)+1)   //references_ar.foreach(reference => line.substring(reference,references_de) )
        println(reference_array)
      }
     // else{
     //   val reference_array=for( i <- 0 to (references_de.length -1) )yield line.substring(references_ar(i),references_de(i))
     // }
    }

    case _ =>
      println("No line received")

  }

}
