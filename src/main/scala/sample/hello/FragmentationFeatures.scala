package sample.hello
import akka.actor._
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map}
import scala.collection.mutable

/**
 * Created by root on 3/3/15.
 */
class FragmentationFeatures extends Actor{
  val relevance= context.actorOf(Props[Relevance], name= "relevance_features")

  var old_fi_frg : Map[String,Int]= mutable.Map()
  var old_seq_conc :Map[String,Int]=mutable.Map()
  var clearing_map =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  //var clearing_map2 =new HashMap[String, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [String,String]
  var wk_Arr_Dr :Map[String,Int]= Map()
  var wk_Arr_Ds :Map[String,Int]= Map()

  var new_source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var new_plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var document_count_ids :Map [String,Int]= Map()

 def receive ={

   case frag_calculate(seq_str,start_end_fltr) =>

    val condition :String=(start_end_fltr(0)+start_end_fltr(3))+"."+start_end_fltr(4)+"."+(start_end_fltr(4)+start_end_fltr(3))+"@"+start_end_fltr(6)+","+start_end_fltr(7)
    if(!clearing_map.entryExists(seq_str,_ == (condition)) ){ //(seq_str -> plag_string_start_point)  && source_file_point != apo to idio key me source file point
      clearing_map=clearing_map.addBinding(seq_str , condition)

      val frg_condition=start_end_fltr(3)+"@"+start_end_fltr(6)+","+start_end_fltr(7)   //arithmos leksewn tou koinou string+"@"+id source,plag keimenou
      //if(start_end_fltr(3) > 100)
        //println(seq_str+" and "+start_end_fltr(0)+"  "+start_end_fltr(1))
      if(old_fi_frg.containsKey(frg_condition) ){                   //if else gia ton ypologismo map Fragmentation features
        val new_value :Int=old_fi_frg.apply(frg_condition)+start_end_fltr(3)
        //common_sequences.put(temp_str,new_value)
        old_fi_frg = old_fi_frg.+(frg_condition -> new_value)
      }
      else {
        if(start_end_fltr(3) != 0) {
          old_fi_frg = old_fi_frg.+(frg_condition -> start_end_fltr(3))
        }
      }

      val seq_condition=seq_str+"@"+start_end_fltr(6)+","+start_end_fltr(7)
      if(old_seq_conc.containsKey(seq_condition)){
        val new_value2 :Int=old_seq_conc.apply(seq_condition)+1
        old_seq_conc=old_seq_conc.+(seq_condition -> new_value2)
      }
      else{
        if(!seq_condition.isEmpty()) {
          old_seq_conc = old_seq_conc.+(seq_condition -> 1)
        }
      }
    }

  case FR_Calcs_Routees_Terminated(source_file_matches,plag_file_matches,normalised_term_frequency,normalised_source_term_freq,term_files_occ,id_total,compared_tuple_w_ids) =>
    val plagfile_id :String=compared_tuple_w_ids._3+","+compared_tuple_w_ids._4     //id source file+","+id plag file

    if(document_count_ids.contains(plagfile_id)){
      document_count_ids=document_count_ids.+(plagfile_id -> (document_count_ids.apply(plagfile_id)+1))
    }
    else
      document_count_ids=document_count_ids.+(plagfile_id -> 1)

    if(document_count_ids.containsValue(16)){

      val x =document_count_ids.find(x => x._2 == 16).get   //afairoume to (doument_id -> 16) stoixeio apo to Map document_count_ids
      document_count_ids=document_count_ids.-(x._1)

      for(keyvalue <- source_file_matches.iterator){
        wk_Arr_Dr=wk_Arr_Dr.+(keyvalue._1 -> keyvalue._2.size )   //to wk_Arr_Dr periexei ws keys ta matched keys tou source file kai values ton arithmo twn emfanisewn tous sto (source) keimeno
      }

      for(keyvalue2 <- plag_file_matches.iterator){
        wk_Arr_Ds=wk_Arr_Ds.+(keyvalue2._1 -> keyvalue2._2.size )       // to eixa wk_Arr_Ds=wk_Arr_Dr.+(keyvalue2._1 -> keyvalue2._2.size )   ????
      }

      var fi_frg :Map[Int,Int]= Map()
      for(kv <- old_fi_frg.iterator) {
        if (kv._1.substring(kv._1.lastIndexOf("@") + 1, kv._1.length()).==(x._1)) {
          fi_frg = fi_frg.+(kv._1.substring(0, kv._1.lastIndexOf("@")).toInt -> kv._2)
          old_fi_frg = old_fi_frg.-(kv._1)
        }
      }
      //println("fi_frg:"+fi_frg)
      var seq_conc :Map[String,Int]= Map()
      for(kv <- old_seq_conc.iterator) {
        if (kv._1.substring(kv._1.lastIndexOf("@") + 1, kv._1.length()).==(x._1)) {
          seq_conc = seq_conc.+(kv._1.substring(0, kv._1.lastIndexOf("@")) -> kv._2)
          old_seq_conc = old_seq_conc.-(kv._1)
        }
      }
      ////////
      //if(compared_tuple_w_ids._3 ==1 && compared_tuple_w_ids._4==1){
       // println(source_file_matches+" and "+plag_file_matches)
      //}
      ////////
      //println("seq_conq: "+seq_conc+"\t fi_frg: "+fi_frg+"\t wk_Arr_Ds: "+wk_Arr_Ds+"\t wk_Arr_Dr: "+wk_Arr_Dr)
      relevance.!(calculate_features(wk_Arr_Dr,wk_Arr_Ds,fi_frg,seq_conc,normalised_term_frequency,normalised_source_term_freq,term_files_occ,id_total,compared_tuple_w_ids) )
      wk_Arr_Ds= Map()
      wk_Arr_Dr=Map()
    }

   case _ =>
     println("There was a problem fetching the data for the calculation of the fragmentation features")
 }


}
