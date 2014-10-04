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
case class routingmessages(fileline :String, counter :Int,ref_act :ActorRef)
case class return_references(reference_array :IndexedSeq[String], line_num :Int)

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
          counter+=1
          router.route(routingmessages(line,counter,self), sender())
       }
    case return_references (ref_array, line_num) =>

      val mapped_refs :Map[String, Int]= Map(ref_array map{s => (s, line_num)} : _*)


    case _ =>
      println("I got nothing")

  }


}

class LineSeparator extends Actor with ActorLogging {
  def receive ={

    case routingmessages(line,counter,file_receiver_ref) =>
    val references_ar=for( i <-0 to (line.length()-1) if(line.charAt(i) == '[') )yield i
    val references_de=for( i <-0 to (line.length()-1) if(line.charAt(i) == ']') )yield i

    if (!references_ar.isEmpty || !references_de.isEmpty){
      if(references_ar.length==1 && references_de.isEmpty ) {
        val new_reference_array=IndexedSeq[String] (line.substring(references_ar(references_ar.length -1),line.length()))
        //println(new_reference_array)
        file_receiver_ref.!(return_references(new_reference_array,counter))
      }
      else if(references_de.length==1 && references_ar.isEmpty){
        val new_reference_array= IndexedSeq[String] (line.substring(0,references_de(references_de.length -1)+1))
        //println(new_reference_array)
        file_receiver_ref.!(return_references(new_reference_array,counter))
      }
      else if(references_ar.length.>(references_de.length) && !references_de.isEmpty){
        val reference_array=for(i <- 0 to (references_ar.length -2) )yield line.substring(references_ar(i),references_de(i)+1)   //references_ar.foreach(reference => line.substring(reference,references_de) )
        val new_reference_array=reference_array.++(line.substring(references_ar(references_ar.length -1),line.length()).split("[\r\n]+"))
        //println(new_reference_array)
        file_receiver_ref.!(return_references(new_reference_array,counter))
      }
      else if(references_ar.length.<(references_de.length)) {
        var reference_array=for( i <- 0 to (references_de.length -2) )yield line.substring(references_ar(i),references_de(i+1)+1)
        val new_reference_array=reference_array.++(line.substring(0,references_de(0)+1).split("[\r\n]+"))
        //println(new_reference_array)
        file_receiver_ref.!(return_references(new_reference_array,counter))
      }
      else{
        val new_reference_array=for(i <- 0 to (references_ar.length-1) )yield line.substring(references_ar(i),references_de(i)+1)
        //println(reference_array)
        file_receiver_ref.!(return_references(new_reference_array,counter))
      }

    }

    case _ =>
      println("No line received")

  }

}
