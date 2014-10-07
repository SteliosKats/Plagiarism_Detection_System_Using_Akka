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
import akka.actor.PoisonPill
import akka.routing.Broadcast
import akka.actor.Terminated


/**
 * Created by root on 9/30/14.
 */
case class file_properties(filename :File,fileid :Int, total_files: Int)
case class routingmessages(fileline :String, counter :Int,ref_act :ActorRef,fileid :Int)
case class return_references(reference_array :IndexedSeq[String], line_num :Int ,fileid: Int, poisoned_routees :Int)

object FileIndexer{
  def main(args: Array[String]): Unit = {
    var path_filename=new File(" ")
    var fileid=0
    var tot_files=0
    val current_directory=new File("/root/Desktop/")
    val indexingSystem= ActorSystem("indexingsystem")//,ConfigFactory.load(application_is_remote))
    val actor_ref_file = indexingSystem.actorOf(Props[FileReceiver],"indexing")
    for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      tot_files+=1
    }
    for(file <- current_directory.listFiles if file.getName endsWith ".txt"){
      path_filename=new File(file.toString())//current_directory.toString.+(file.toString))
      //println(path_filename)
      fileid+=1
      actor_ref_file ! file_properties(path_filename,fileid,tot_files)
    }
  }
}

class FileReceiver extends Actor{
  var counter_terminated=0
  var all_refs :Map[String,Int]=Map(" " -> 0)
  var file_counter=0
  /* Creating A router to route "Workers" to extract citations for each line of the file */
  var router ={
    val routees=Vector.fill(5){
      val lineseparator=context.actorOf(Props[LineSeparator])
      context watch lineseparator
      ActorRefRoutee(lineseparator)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  /* Creating A router to route "Workers" to ectract citations for each line of the file */

  def receive = {

    case file_properties(filename, fileid, total_files) =>
      var counter = 0
      file_counter+=1
      for (line <- Source.fromFile(filename).getLines()) {
        counter += 1
        router.route(routingmessages(line, counter, self, fileid), sender())
      }
      if (file_counter == total_files) {
        router.route(Broadcast(PoisonPill), sender())
      }
    case return_references (ref_array, line_num,fileid,poisoned_routees) =>
      //val mapped_refs :Map[String, Int]= Map(ref_array map{s => (s, line_num)} : _*)

      /* to "," sto value tou Map ksexwrizei ton monadiko arithmo keimenou apo ton arithmo grammhs tou sygkrkrimenou keimenou */
      if(poisoned_routees!=5) {
        // Store in  Map[reference -> file_id,line]  format
        val mapped_refs :Map[String, Int] = Map(ref_array map { s => (s, fileid)}: _*)
        all_refs=all_refs.++(mapped_refs)
      }
      else {
        println(all_refs)

      }
    case Terminated (corpse) =>
      router = router.removeRoutee(corpse)
      counter_terminated.+=(1)
      if(counter_terminated==5){
        self.!(return_references(Array(" "),0,0,5))
      }
    case _ =>
      println("I got nothing")

  }


}

class LineSeparator extends Actor with ActorLogging {
  def receive ={

    case routingmessages(line,counter,file_receiver_ref,fileid) =>
      val references_ar=for( i <-0 to (line.length()-1) if(line.charAt(i) == '[') )yield i
      val references_de=for( i <-0 to (line.length()-1) if(line.charAt(i) == ']') )yield i

      if (!references_ar.isEmpty || !references_de.isEmpty){
        if(references_ar.length==1 && references_de.isEmpty ) {
          val new_reference_array=IndexedSeq[String] (line.substring(references_ar(references_ar.length -1),line.length())+counter.toString())
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_de.length==1 && references_ar.isEmpty){
          val new_reference_array= IndexedSeq[String] (line.substring(0,references_de(references_de.length -1)+1)+counter.toString())
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_ar.length.>(references_de.length) && !references_de.isEmpty){
          val reference_array=for(i <- 0 to (references_ar.length -2) )yield line.substring(references_ar(i),references_de(i)+1)+counter.toString()
          val new_reference_array=reference_array.++((line.substring(references_ar(references_ar.length -1),line.length())+counter.toString()).split("[\r\n]+"))
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else if(references_ar.length.<(references_de.length)) {
          var reference_array=for( i <- 0 to (references_de.length -2) )yield line.substring(references_ar(i),references_de(i+1)+1)+counter.toString()
          val new_reference_array=reference_array.++((line.substring(0,references_de(0)+1)+counter.toString()).split("[\r\n]+"))
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }
        else{
          val new_reference_array=for(i <- 0 to (references_ar.length-1) )yield line.substring(references_ar(i),references_de(i)+1)+counter.toString()
          //println(new_reference_array)
          file_receiver_ref.!(return_references(new_reference_array,counter,fileid,0))
        }

      }

    case PoisonPill =>
      context.stop(self)

    case _ =>
      println("No line received")

  }

}

