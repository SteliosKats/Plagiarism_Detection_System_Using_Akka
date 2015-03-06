package sample.hello
import akka.actor._
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map}

/**
 * Created by root on 3/3/15.
 */
class FragmentationFeatures extends Actor{
  val relevance= context.actorOf(Props[Relevance], name= "relevance_features")

  //var source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  //var plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var external_counter :Int =0

  var fi_frg : Map[Int,Int]= Map()
  var seq_conc :Map[String,Int]=Map()
  var clearing_map =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  //var clearing_map2 =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  var wk_Arr_Dr :Map[String,Int]= Map()
  var wk_Arr_Ds :Map[String,Int]= Map()

  var new_source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var new_plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var all_terminated :Int=0

 def receive ={

   case frag_calculate(seq_str,start_end_fltr) =>

    val condition :String=(start_end_fltr(0)+start_end_fltr(3))+"."+start_end_fltr(4)+"."+(start_end_fltr(4)+start_end_fltr(3))
    if(!clearing_map.entryExists(seq_str,_ == (condition)) ){ //(seq_str -> plag_string_start_point)  && source_file_point != apo to idio key me source file point
      //println("Seq_str:"+seq_str+"  plag_string_start_point:"+start_end_fltr(0))
      clearing_map=clearing_map.addBinding(seq_str , condition)
      if(fi_frg.containsKey(start_end_fltr(3))){                   //if else gia ton ypologismo map Fragmentation features
        val new_value :Int=fi_frg.apply(start_end_fltr(3))+start_end_fltr(3)
        //common_sequences.put(temp_str,new_value)
        fi_frg = fi_frg.+(start_end_fltr(3) -> new_value)
      }
      else {
        if(start_end_fltr(3) != 0) {
          fi_frg = fi_frg.+(start_end_fltr(3) -> start_end_fltr(3))
        }
      }

      if(seq_conc.containsKey(seq_str)){
        val new_value2 :Int=seq_conc.apply(seq_str)+1
        seq_conc=seq_conc.+(seq_str -> new_value2)
      }
      else{
        if(!seq_str.isEmpty()) {
          seq_conc = seq_conc.+(seq_str -> 1)
        }
      }
    }

  case  save_globalHashMaps(source_file_matches,plag_file_matches) =>
     new_plag_file_matches=plag_file_matches
     new_source_file_matches=source_file_matches

  case "FR Calcs Routees Terminated" =>
    all_terminated +=1
    if(all_terminated==(Runtime.getRuntime().availableProcessors()*2)){
      for(keyvalue <- new_source_file_matches.iterator){
        wk_Arr_Dr=wk_Arr_Dr.+(keyvalue._1 -> keyvalue._2.size )
      }

      for(keyvalue2 <- new_plag_file_matches.iterator){
        wk_Arr_Ds=wk_Arr_Dr.+(keyvalue2._1 -> keyvalue2._2.size )
      }
      println("seq_conq: "+seq_conc+"\t fi_frg: "+fi_frg+"wk_Arr_Ds: "+wk_Arr_Ds+"\t wk_Arr_Dr: "+wk_Arr_Dr)
      relevance.!(calculate_features(wk_Arr_Dr,wk_Arr_Ds,fi_frg,seq_conc) )
      external_counter=0
      all_terminated=0
    }

   case _ =>
     println("There was a problem fetching the data for the calculation of the fragmentation features")
 }


}
