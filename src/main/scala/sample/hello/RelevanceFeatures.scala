package sample.hello

import java.io.{PrintWriter,FileWriter,File}
import akka.actor.Actor
import java.io.File
import collection.JavaConversions._
import scala.math.pow
import collection.mutable.{HashMap,MultiMap,Map}

/**
 * Created by root on 3/4/15.
 */
class Relevance extends Actor {

  def receive = {
    case calculate_features(wk_Arr_Dr, wk_Arr_Ds, fi_frg, seq_conc) =>
      var relevance_map: Map[String, Float] = Map()
      var Relevance_of_Sequnces: Array[Int] = Array.empty
      var counter: Int = 0
      var ginomeno: Float = 1
      for (key2 <- seq_conc.keys) {
        //println(key2)
        val wk_Arr_R: Map[String, Int] = occ_wk(key2.split(" +"), wk_Arr_Dr) //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        val wk_Arr_S: Map[String, Int] = occ_wk(key2.split(" +"), wk_Arr_Ds) //to prwto orisma diagrafei ta kena sta keys tou cos_seq p.x.Map(the  -> 1, by  -> 5) diagrafei ta kena sto the kai to by
        //println(wk_Arr_R+"\t and \t"+wk_Arr_R)

        val first_fraction: Float = (1 / pow(2.71828, seq_conc.apply(key2) - 1)).toFloat
        //println(first_fraction)
        val array_source: Array[Int] = wk_Arr_R.values.toArray //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto source file tou sygkekrimenou
        val array_plag: Array[Int] = wk_Arr_S.values.toArray //pinakas pou periexei twn arithmo emfanisewn kathe lekshs tou key2 (sequence) sto plagiarised file tou sygkekrimenou
        //println(array_plag)
        for (k <- 0 to (key2.split(" +").length - 1)) {
          ginomeno = ginomeno * (2.toFloat / (array_plag(k).toFloat + array_source(k).toFloat)) //array_plag(k).toFloat
          //println("Ginomeno:"+ginomeno)
        }
        val second_fraction = ginomeno
        val result: Float = first_fraction * second_fraction
        relevance_map = relevance_map + (key2 -> result)
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
      println("Fragmentation Features :" + fi_frg)
      println("RELEVANCE features MAP:" + relevance_features)
      val filepath = new File(".").getAbsolutePath().dropRight(1) + "data/FragRelev.txt"
      if (!new java.io.File(filepath).exists()) {
        val frelfile = new PrintWriter(new File(filepath))
        frelfile.write("Fragmentation Features :"+fi_frg+" RELEVANCE features MAP:"+relevance_features+"\n")
        //nerfile.write("other"+"\tO\n")
        frelfile.close()
      }
      else{
        val fw = new FileWriter(filepath ,true)
        try {
          fw.write("Fragmentation Features :"+fi_frg+" RELEVANCE features MAP:"+relevance_features+"\n")
        }
        finally fw.close()

      }
  }
  def occ_wk(key_Arr :Array[String],old_wk_Arr:Map[String,Int]): Map[String,Int] ={
    var wk_arr : Map[String,Int]= Map()
    var counter :Int=0
    var external_counter= 0   //p.x. by roadgood by roadgood by  de mporousan ta by na pane ksexwrista sto map
    for(key1 <- key_Arr){
      //println(key1)
      wk_arr=wk_arr.+(key1+"@"+external_counter -> old_wk_Arr.apply(key1))
      external_counter+=1
      counter=0
    }
    return(wk_arr)
  }

}
