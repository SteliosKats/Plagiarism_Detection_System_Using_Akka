package sample.hello

import java.io.{PrintWriter,FileWriter,File}
import akka.actor.{Props, Actor}
import java.io.File
import akka.routing.{RoundRobinRoutingLogic, Router, ActorRefRoutee}

import collection.JavaConversions._
import scala.math.pow
import scala.math.log
import scala.collection.mutable.HashMap
import collection.mutable.{MultiMap,Map}

/**
 * Created by root on 3/4/15.
 */
class Relevance extends Actor {
  var all_frg_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  var all_rel_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  var counter_rel :Int=0
  var files_read :Int =0
  var counter :Double=0

  //val info_gain=context.actorOf(Props[InformationGainEval], name= "information_gain_evaluation")

  def receive = {
    case calculate_features(wk_Arr_Dr, wk_Arr_Ds, fi_frg, seq_conc, normalised_term_frequency, normalised_source_term_freq, term_files_occ, id_total, compared_tuple_w_ids) =>
      //if(compared_tuple_w_ids._1.contains("LEMMA_asdasd.txt")){
        //println(compared_tuple_w_ids._3+" and "+compared_tuple_w_ids._4)
      var relevance_map: Map[String, Float] = Map()
      var Relevance_of_Sequnces: Array[Int] = Array.empty
      var ginomeno: Float = 1
      //println(compared_tuple_w_ids._1 + " and " + compared_tuple_w_ids._2)
      //println("wk_Arr_Dr : "+wk_Arr_Dr + "\n and wk_Arr_Ds :"+wk_Arr_Ds+"\n and "+ seq_conc)
      for (key2 <- seq_conc.keys) {
        //println(key2)
        val wk_Arr_R: Map[String, Int] = occ_wk(key2.split(" +"), wk_Arr_Dr) //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        val wk_Arr_S: Map[String, Int] = occ_wk(key2.split(" +"), wk_Arr_Ds) //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        //println(wk_Arr_R+"\t and \t"+wk_Arr_R)

        val first_fraction: Float = (1.toFloat / pow(2.71828, seq_conc.apply(key2) - 1)).toFloat

        val array_source: Array[Int] = wk_Arr_R.values.toArray //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto source file tou sygkekrimenou
        val array_plag: Array[Int] = wk_Arr_S.values.toArray //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto plagiarised file tou sygkekrimenou
        //println(wk_Arr_R+"\t and \t"+array_source.toSeq)
        for (k <- 0 to (key2.split(" +").length - 1)) {
          ginomeno = ginomeno * (2.toFloat / (array_plag(k).toFloat + array_source(k).toFloat))
          //println("Ginomeno:"+ginomeno)
        }
        val second_fraction = ginomeno
        val result: Float = first_fraction * second_fraction
        relevance_map = relevance_map + (key2 -> result) //pollaplasiasmos kai me ton arithmo twn emfanisewn ths koinhs akolouthias????
      }

      var relevance_features: Map[Int, Float] = Map()
      for (key <- relevance_map.keys) {
        if (relevance_features.containsKey(key.split(" +").length)) {
          //an to map periexei kleidi iso me ton arithmo twn leksewn ths sequence
          val relev_value = relevance_features.apply(key.split(" +").length) + relevance_map.apply(key)
          relevance_features = relevance_features.+(key.split(" +").length -> relev_value)
        }
        else {
          relevance_features = relevance_features.+(key.split(" +").length -> relevance_map.apply(key))
        }
      }
      //println("Fragmentation Features :" + fi_frg)
      //println("RELEVANCE features MAP:" + relevance_features)
      val data_dir: File = new File(new File(".").getAbsolutePath().dropRight(1) + "data/") //Creating the folder which will store data files (files containing fragmentation and relevance features)
      if (!data_dir.exists()) {
        data_dir.mkdir() //create temp directory
      }
      val filepath = new File(".").getAbsolutePath().dropRight(1) + "data/FragRelev.txt"
      if (!new java.io.File(filepath).exists()) {
        val frelfile = new PrintWriter(new File(filepath))
        frelfile.write("Fragmentation Features :" + fi_frg + " RELEVANCE features MAP:" + relevance_features + "\n")
        //nerfile.write("other"+"\tO\n")
        frelfile.close()
      }
      else {
        val fw = new FileWriter(filepath, true)
        try {
          fw.write("Fragmentation Features :" + fi_frg + " RELEVANCE features MAP:" + relevance_features + "\n")
        }
        finally fw.close()

      }

      for (kv <- fi_frg.iterator) {           //for(i <- 1 to 122){
        all_frg_y = all_frg_y.addBinding(kv._1, kv._2 + "@" + compared_tuple_w_ids._3 + "," + id_total._1) // all_frg_y=all_frg_y.addBinding(kv._1,kv._2+"#fragmentation_feature"+"@"+classify_plagiarisation._3+","+classify_plagiarisation._1)
      }
      for (kv <- relevance_features.iterator) {
        all_rel_y = all_rel_y.addBinding(kv._1, kv._2 + "@" + compared_tuple_w_ids._3 + "," + id_total._1) //(megethos koinhs akolouthias , timh feature gia sygkekrimeno megethos koinhs akolouthias @ plagiarised 'h oxi ,plag_id keimenou pou eksetazetai)
      }
      files_read += 1

      if (files_read == id_total._2) {
        //println(compared_tuple_w_ids)
        //println(all_frg_y+" and "+all_rel_y)
        context.actorSelection("/user/plag_analysis/comparing_s_p/returned_matches/classification").!(cosine_similarity(all_frg_y,all_rel_y,normalised_term_frequency,normalised_source_term_freq,term_files_occ,id_total,compared_tuple_w_ids))
        all_frg_y = new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
        all_rel_y = new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
        files_read = 0
      }
   // }
  }
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]

  def occ_wk(key_Arr :Array[String],old_wk_Arr:Map[String,Int]): Map[String,Int] ={
    var wk_arr : Map[String,Int]= Map()
    var counter :Int=0
    var external_counter= 0   //p.x. by roadgood by roadgood by  de mporousan ta by na pane ksexwrista sto map
    for(key1 <- key_Arr){
      //println(key1)
      wk_arr=wk_arr.+(key1+"@"+external_counter -> old_wk_Arr.apply(key1))   //p.x.  by roadgood by roadgood -> wk_arr=(by@0 -> 10 ,roadgood@1 -> 5 , by@3 -> 10 , roadgood@4 -> 5)
      external_counter+=1
      counter=0
    }
    return(wk_arr)
  }

}
