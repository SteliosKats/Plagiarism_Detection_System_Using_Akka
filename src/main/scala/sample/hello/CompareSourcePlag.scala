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
    case compare_source_plag(source_filepath,plag_filepath,source_file_words,comp_file_ids,tup_le) =>
      plag_lines_size =Source.fromFile(plag_filepath).getLines().size
      //println("Comparing:"+source_filepath+"and :"+plag_filepath)
      var counter :Int=0
      val textsource =Source.fromFile(source_filepath)
      for (line <- textsource.getLines()) {
        val word_arr=line.mkString.split(" ")
        for(word <- word_arr){
          counter+=1
          //println(word+" , "+plag_filepath)
          router.route(word_line_comp(word,plag_filepath,counter,comp_file_ids),sender())
        }
      }
      //println(source_filepath+"\t"+plag_filepath)
      textsource.close()
      val id_size_filename_total :Tuple4[Int,Int,String,Int]=(comp_file_ids._2,tup_le._2,tup_le._1,tup_le._3) //plag_id_counter==id_filename_total._3
      val compared_tuple_w_ids :Tuple4 [String,String,Int,Int]=(source_filepath,plag_filepath,comp_file_ids._1,comp_file_ids._2)
      router.route(Broadcast(Routees_Termination(id_size_filename_total,source_file_words,compared_tuple_w_ids)), sender())

    case _ =>
      println("nothing happened")
  }

}


