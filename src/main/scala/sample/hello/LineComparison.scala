package sample.hello

import akka.actor.{Props, ActorLogging, Actor}
import akka.routing._

import scala.collection.mutable.{HashMap, Map}
import scala.io.Source

/**
 * Created by root on 3/3/15.
 */
class LineComparison extends Actor with ActorLogging {

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val wordcompare2=context.actorOf(Props[WordComparison_Inception])
      context watch wordcompare2
      ActorRefRoutee(wordcompare2)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive ={

    case word_line_comp(word,plag_filepath,counter_source,comp_file_ids) =>
      var counter_plag=0
      //var plag_lines_size :Int=Source.fromFile(plag_filepath).getLines.size   ????????petage error (Too many files)
      val textsource =Source.fromFile(plag_filepath)
      for (line <- textsource.getLines()) {
        val line_arr :Array[String]=line.split(" ")
        router.route(word_line_comp_inception(word,line_arr,counter_source,counter_plag,plag_filepath,comp_file_ids),sender())
        counter_plag=counter_plag+ line.split(" ").size
      }
      textsource.close()
    case Routees_Termination(id_size_filename_total,source_file_words,compared_tuple_w_ids) =>
      router.route(Broadcast(Routees_Inception_Termination(id_size_filename_total,source_file_words,compared_tuple_w_ids)),sender())

    case _ =>
      println("I didn't got word from the Source File this time!")

  }

}

class WordComparison_Inception extends Actor with ActorLogging {
  var plag_tuple :Tuple2[String,Int]=new Tuple2("",0)
  var cassify :Map[String,Int]=Map()    ///this map contains
  def receive ={
    case word_line_comp_inception(word,line_arr,counter_source,counter_plag,plag_filepath,comp_file_ids) =>
      var inception_counter=0
      var times_found=0
      var word_found :Boolean=false
      for(plag_word <-line_arr){
        inception_counter+=1
        if(plag_word==word){
          times_found+=1
          word_found=true
          plag_tuple =(word,counter_plag+inception_counter)
         // if(plag_filepath.contains("LEMMA_ok.txt")){
           // println("File id:"+plagfile_id +"\t "+plag_tuple)
        // }
          val source_word :Map[String,Int]= Map().+(word -> counter_source)
          context.actorSelection("/user/plag_analysis/comparing_s_p/returned_matches").!(returned_Multimaps(plag_tuple,source_word,times_found,comp_file_ids))
        }
      }

    case Routees_Inception_Termination(id_size_filename_total,source_file_words,compared_tuple_w_ids) =>
      context.actorSelection("/user/plag_analysis/comparing_s_p/returned_matches").!(End_Of_SourceFile(id_size_filename_total,source_file_words,compared_tuple_w_ids))

    case _ =>
      println("Wrong data sent from the source or pontentially palgiarised file!")

  }

}