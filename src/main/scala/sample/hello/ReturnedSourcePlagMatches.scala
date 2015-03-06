package sample.hello

import akka.actor._
import akka.routing._
import scala.collection.mutable
import scala.io.Source
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map}
import scala.math._
/**
 * Created by root on 3/5/15.
 */
class ReturnedSourcePlagMatches extends Actor {
  val frg_f=context.actorOf(Props[FragmentationFeatures], name= "fragmentation_features")
  val frgm_calc=context.actorOf(Props[FragFeaturesCalc], name= "fragmentation_calculations")
  var plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var external_counter :Int =0
  var file_id_counter :Int=1

  def receive ={

    case returned_Multimaps(plag_tuple,source_word,times_found,plagfile_id) =>
      if(!source_word.isEmpty && file_id_counter==plagfile_id){
        source_file_matches.addBinding(source_word.head._1,source_word.head._2)
      }
      if(file_id_counter==plagfile_id) {
        plag_file_matches.addBinding(plag_tuple._1, plag_tuple._2)
      }

    case "End of Source File" =>
      external_counter+=1
      //println(external_counter)
      if(external_counter==pow(Runtime.getRuntime().availableProcessors()*2,2)){
        println("\t plagiarized file matches: "+plag_file_matches+"\t source file matches: "+source_file_matches)
        frgm_calc.!(calculate_wks(plag_file_matches,source_file_matches))
        frg_f.!(save_globalHashMaps(source_file_matches,plag_file_matches)) //stlenoume ta HashMaps gia apothikeysh sto child Actor giati stis
                                                                         //epomenes ekteleseis tou paronta actor ta sygkekrimena Hashmap values allazoun

        /////Mhdenismos twn arxikwpoihmenwn hashmaps kai external counter (gia na synexistei h sygkrish source me alla plag files)
        external_counter=0
        plag_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
        source_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
        file_id_counter+=1
      }

    case _ =>
      println("Unexpected Error On Returning Matches Between Two Examined Files")


  }

}
