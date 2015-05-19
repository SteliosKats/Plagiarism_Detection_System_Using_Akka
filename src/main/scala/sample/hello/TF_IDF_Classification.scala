package sample.hello

import java.io.File

import akka.actor._
import akka.actor.ActorLogging
import akka.routing._
import scala.collection.mutable
import scala.io.Source
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map,Set}
import scala.math._

/**
 * Created by root on 4/22/15.
 */
class TF_IDF_Classification extends Actor with ActorLogging{
 var term_files_occ :Map [String,Int]=Map()
 var external_counter=0
 var normalised_term_frequency :Map[String,Float]=Map()
 var source_term_frequency :Map[String,Float]=Map()
 var source_term_occ :Map [String,Int]=Map()
 var normalised_source_term_freq :Map[String,Float]=Map()
 val frg_f=context.actorOf(Props[FragmentationFeatures], name= "fragmentation_features")
 val frgm_calc=context.actorOf(Props[FragFeaturesCalc], name= "fragmentation_calculations")
 val chunk_maps =context.actorOf(Props[Chunking_Maps_Before_IG], name= "IG_map_chunking")
 var plagid_size :Map[Int,Int]=Map()

 def receive ={
   case calculate_classification(source_file_matches,plag_file_matches,source_file_words,compared_tuple_w_ids,id_size_filename_total) =>
     external_counter+=1   //metraei ta arxeia pou pernoun apo ton sygkekrimeno actor (xrhsimeyei gia  to term_files_occ map)
     if(external_counter<=id_size_filename_total._4)
        plagid_size=plagid_size.+(id_size_filename_total._1 -> id_size_filename_total._2) //Map me stoixeia key: id tou suspicious arxeiou kai value:to megethos tou se lekseis

      var total_num :Int =0
      var term_frequency :Map[String ,Int]= Map()
      //println("source_file_words :"+source_file_words)
      for (kv <- plag_file_matches.iterator){
          total_num=plagid_size.apply(id_size_filename_total._1)
          term_frequency=term_frequency.+(kv._1 -> kv._2.size)
          if(term_files_occ.exists(keyvalue => keyvalue._1 == kv._1))
            term_files_occ=term_files_occ.+(kv._1 -> (term_files_occ.apply(kv._1)+1))
          else
            term_files_occ=term_files_occ.+(kv._1 -> 1)
      }
      for (keyvalue <- term_frequency.iterator){
        normalised_term_frequency=normalised_term_frequency.+(keyvalue._1+"@"+id_size_filename_total._1 -> (keyvalue._2.toFloat/total_num.toFloat))
      }
      term_frequency=Map()
      total_num=0
      for (kv <- source_file_words.iterator){
        total_num=total_num.+(kv._2)
        term_frequency=term_frequency.+(kv._1 -> kv._2)
        if(!source_term_occ.exists(_._1 == kv._1))
          source_term_occ=source_term_occ.+(kv._1 -> source_file_words.apply(kv._1))
      }
      for (keyvalue <- term_frequency.iterator){
        normalised_source_term_freq=normalised_source_term_freq.+(keyvalue._1 -> (keyvalue._2.toFloat/total_num.toFloat))
      }
     //println("term file occurences :"+term_files_occ+"\t and normalised_term_frequency:"+normalised_term_frequency)
      //println(normalised_source_term_freq)
      val id_total :Tuple2[Int,Int]=(id_size_filename_total._1,id_size_filename_total._4)
      frgm_calc.!(calculate_wks(plag_file_matches,source_file_matches,normalised_term_frequency,normalised_source_term_freq,term_files_occ,id_total,compared_tuple_w_ids))
      source_term_occ=Map()
      if(external_counter==id_size_filename_total._4){
        term_files_occ=Map()
        normalised_term_frequency=Map()
        normalised_source_term_freq=Map()
        external_counter=0
      }

   case cosine_similarity(all_frg_y,all_rel_y,norm_term_freq,norm_source_term_freq,term_occ,id_total,compared_tuple_w_ids) =>
     var terminate :Boolean =false
     val total_files=id_total._2
     var idf :Map [String ,Double]= Map()
     var tf_idf :Map [String ,Double]= Map()

     for(kv <- term_occ.iterator){
        val idf_value :Double = 1.toDouble + math.log((total_files.toDouble/kv._2.toDouble))
        idf=idf.+(kv._1 -> idf_value)   //idf for every term appeared on plag files (having been compared with source file terms)
        val filtered_keys =norm_term_freq.filterKeys(key => key.substring(0,key.lastIndexOf("@")) == kv._1).toMap  //gia kathe key sto  normalized term occurency map pou exei idio substring prin to @
        for(keyval <- filtered_keys.iterator ){  // briskoume thn timh tf*idf tou (dhladh gia kathe oro se kathe plag keimeno vriskoume thn timh tf*idf tou)
        val n_tf_value :Double =keyval._2.toDouble
           tf_idf=tf_idf.+(keyval._1 ->  (n_tf_value.*(idf_value)) )   //oi times tf_idf gia kathe plagiarised file (oles mazi)
        }
     }

     var tf_idf_source :Map [String ,Double]= Map()
     for(kv <- norm_source_term_freq.iterator){
       if(idf.contains(kv._1)){
         val idf_value :Double =idf.apply(kv._1)
         tf_idf_source=tf_idf_source.+(kv._1 -> (kv._2.toDouble.*(idf_value)))   //tif_idf gia to source file
       }
       else
         tf_idf_source=tf_idf_source.+(kv._1 -> 0)
     }
     //println(tf_idf) //tf_idf plag
     //println(tf_idf_source)  //tf_idf source
     var cosine_similarity_map :Map[Int,Double]=Map()
     for(i <- 1 to id_total._2){
       var dot_product :Double =0
       var norm2_source :Double=0
       var norm2_plag :Double =0
       val filtered_source =tf_idf.filterKeys(key => key.substring(key.lastIndexOf("@")+1,key.length()) == i.toString())
       //println(filtered_source)
       for(kv2 <- tf_idf_source.iterator){
         if(filtered_source.contains(kv2._1+"@"+i.toString())){
           dot_product=dot_product.+(kv2._2.*(filtered_source.apply(kv2._1+"@"+i)))  //Dot product(SourceDoc, SuspiciousDoc)
         }


         norm2_source=norm2_source.+(pow(kv2._2, 2))

         if(filtered_source.contains(kv2._1+"@"+i.toString()))
           norm2_plag=norm2_plag.+(pow(filtered_source.apply(kv2._1+"@"+i.toString()) ,2))
       }
       norm2_source=sqrt(norm2_source)
       norm2_plag=sqrt(norm2_plag)
       val cosine :Double =dot_product./(norm2_source.*(norm2_plag))
       cosine_similarity_map=cosine_similarity_map.+(i -> cosine)
     }

     var classification_map :Map[Int ,String]=Map()
     for(kv <-cosine_similarity_map.iterator){
         if(kv._2 >= 0.5)
           classification_map=classification_map+(kv._1 -> "Plagiarised")
         else
           classification_map=classification_map+(kv._1 -> "Not_Plagiarised")
     }
     chunk_maps.!(ig_chunking(all_frg_y,all_rel_y,classification_map,id_total,compared_tuple_w_ids))

   case _ =>
     println("No Document words found for classification ")
 }
}

class Chunking_Maps_Before_IG extends Actor with ActorLogging{
  var counter :Int=0
  override def preStart(): Unit = {
    //var counter :Int=0
    val source_filepath =new File(".").getAbsolutePath().dropRight(1)+"source_files/"
    val source_dir :File= new File(source_filepath)
    for(file <- source_dir.listFiles if(file.getName.endsWith(".txt") )){
      counter+=1  //count the number of source files on souce_file folder
    }
  }

  var router ={
    val workerCount=Runtime.getRuntime().availableProcessors()
    val routees=Vector.fill(workerCount*2){
      val info_gain_eval=context.actorOf(Props[InformationGainEval])
      context watch info_gain_eval
      ActorRefRoutee(info_gain_eval)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  var new_all_frg_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  var new_all_rel_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  var ext_counter2 :Int =0

  def receive ={
    case ig_chunking(all_frg_y,all_rel_y,classification_map,id_total,compared_tuple_w_ids) =>
      ext_counter2 +=1
      for(i <- 1 to id_total._2) {
        for (kv <- all_frg_y.iterator) {
          for (key <- kv._2) {
            if (key.substring(key.lastIndexOf(",")+1,key.length()).toInt == i) {
              val temp_key :String=key.substring(0,key.lastIndexOf("@")+1)+classification_map.apply(i)+","+compared_tuple_w_ids._3+","+key.substring(key.lastIndexOf(",")+1,key.length())
              //all_frg_y.removeBinding(kv._1, key)
              new_all_frg_y.addBinding(kv._1,temp_key) //+key.substring(key.lastIndexOf(",")+1,key.length()))
            }
          }
        }
      }
      for(i <- 1 to id_total._2) {
        for (kv <- all_rel_y.iterator) {
          for (key <- kv._2) {
            if (key.substring(key.lastIndexOf(",")+1,key.length()).toInt == i) {
              val temp_key :String=key.substring(0,key.lastIndexOf("@")+1)+classification_map.apply(i)+","+compared_tuple_w_ids._3+","+key.substring(key.lastIndexOf(",")+1,key.length())
              //all_rel_y.removeBinding(kv._1, key)
              new_all_rel_y.addBinding(kv._1,temp_key)
            }
          }
        }
      }

      //println(counter)
     if(ext_counter2==counter){
      //println(new_all_frg_y)
      //println(new_all_rel_y)
      var terminate :Boolean =false
      var external_counter :Int =0  //counter gia na dhmiourgithoun  ola ta arff arxeia kai na mhn yparksoun diplotypa metaksy rel and frag arffs
      val map_size1 :Int =new_all_frg_y.size
      val map_size2 :Int =new_all_rel_y.size
      val feature_map_size = map_size1 +map_size2
      val pre1 = feature_map_size.toDouble/10.toDouble
      val pre2 = scala.math.floor(feature_map_size.toDouble/10.toDouble)
      val remainder = pre1 - pre2
      val frg_floor= scala.math.floor(map_size1.toDouble/10.toDouble)
      println(feature_map_size+"\t"+pre1+"\t"+pre2+"\t"+remainder+"\t"+frg_floor)
      var rem_subset:Map[Int, scala.collection.mutable.Set[java.lang.String]]= Map()
      if(map_size1 >=10) {             //An to map me ta fragmentation features einai perissotera apo 10 opote mporoume na to xwrisoume estw kai mia fora
        for (i <- 1 to frg_floor.toInt) {
          external_counter+=1
          if (i == pre2.toInt && (remainder * 10) != 0) {
            // i==5  apo 40
            val subseq = new_all_frg_y.toSeq.sortBy(_._1).slice(10 * (i - 1), 10 * (i)).map(x => x._1 -> x._2).toMap
            rem_subset = rem_subset.++(new_all_frg_y.toSeq.sortBy(_._1).slice(10 * (i), 10 * (i) + (pre1 * 10 - pre2 * 10).toInt).map(x => x._1 -> x._2).toMap)    //  rem_subset = rem_subset.++(all_frg_y.toSeq.sortBy(_._1).slice(10 * (i), 10 * (i) + (pre1 * 10 - pre2 * 10).toInt).map(x => x._1 -> x._2).toMap)
            terminate = true
            router.route(EvalIG(subseq,external_counter ,id_total._2,compared_tuple_w_ids._3),sender())
          }
          else if (terminate.!=(true)) {
            val subseq = new_all_frg_y.toSeq.sortBy(_._1).slice(10 * (i - 1), 10 * i).map(x => x._1 -> x._2).toMap
            //println(new_all_frg_y.toSeq.slice(10 * (i - 1), 10 * (i)).map(x => x._1 -> x._2))
            router.route(EvalIG(subseq,external_counter,id_total._2,compared_tuple_w_ids._3),sender())
          }
        }
      }
      else {                                            //alliws to stelnoume  mazi me ta ypoloipa relevance features
        rem_subset=new_all_frg_y
      }

      for(kv <- rem_subset.iterator){
        for(values <- kv._2.seq){
          new_all_rel_y.addBinding(kv._1,values)
        }
      }

      val rel_features_size :Int =new_all_rel_y.size
      val rel_and_subset=rem_subset.size +rel_features_size
      val pre11 = rel_and_subset.toDouble/10.toDouble
      val pre22 = scala.math.floor(rel_and_subset.toDouble/10.toDouble)
      val rem = pre11 - pre22
      if(rel_and_subset >=10) {
        for (i <- 1 to pre22.toInt) {
          external_counter+=1
          if (i == pre22.toInt && (rem * 10) <= 5) {
            // i==5  apo 40
            val subseq = new_all_rel_y.toSeq.sortBy(_._1).slice(10 * (i - 1), (10 * (i)).+((rem * 10).toInt)).map(x => x._1 -> x._2).toMap
            terminate = true
            router.route(EvalIG(subseq,external_counter ,id_total._2,compared_tuple_w_ids._3),sender())
          }
          else if(i == pre22.toInt && (rem * 10) >=5) {
            val subseq = new_all_rel_y.toSeq.sortBy(_._1).slice(10 * (i - 1), 10 * (i)).map(x => x._1 -> x._2).toMap
            router.route(EvalIG(subseq,i,id_total._2,compared_tuple_w_ids._3),sender())
            val subseq2 = new_all_rel_y.toSeq.sortBy(_._1).slice(10 * (i), 10 * (i).+((rem * 10).toInt)).map(x => x._1 -> x._2).toMap
            router.route(EvalIG(subseq2,external_counter,id_total._2,compared_tuple_w_ids._3),sender())
            terminate = true
          }
          else if (terminate.!=(true)) {
            val subseq = new_all_frg_y.toSeq.sortBy(_._1).slice(10 * (i - 1), 10 * i).map(x => x._1 -> x._2).toMap
            //println(subseq)
            router.route(EvalIG(subseq,i,id_total._2,compared_tuple_w_ids._3),sender())
          }
        }
      }
      else if(rel_and_subset != 0){
        external_counter+=1
        val subseq = new_all_rel_y.toSeq.slice(0, rel_and_subset).map(x => x._1 -> x._2).toMap
        router.route(EvalIG(subseq,external_counter,id_total._2,compared_tuple_w_ids._3),sender())
      }

    }

    case _ =>
      println("No Maps Given as an input for chunking ")
  }

  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]

}
