package sample.hello
import akka.actor._
import akka.routing._
import scala.io.Source
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map}
import scala.math._

/**
 * Created by root on 3/3/15.
 */
class CompareSourcePlag extends Actor {
  val ret_plgs_srcs=context.actorOf(Props[ReturnedSourcePlagMatches], name= "returned_matches")
  var plag_lines_size :Int= -1

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val linecompare=context.actorOf(Props[LineComparison])
      context watch linecompare
      ActorRefRoutee(linecompare)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive ={
    case compare_source_plag(source_filepath,plag_filepath,plagfile_id) =>
      plag_lines_size =Source.fromFile(plag_filepath).getLines().size
      println(plag_filepath) //edw kati trexei
      var counter=0
      for (line <- Source.fromFile(source_filepath).getLines()) {
        val word_arr=line.mkString.split(" ")
        for(word <- word_arr){
          counter+=1
          //println(word+" , "+plag_filepath)
          router.route(word_line_comp(word,plag_filepath,counter,plagfile_id),sender())
        }
      }
      router.route(Broadcast("Terminate"), sender())

    case _ =>
      println("nothing happened")
  }

}


