package sample.hello

import akka.actor._
import akka.routing._
import scala.collection.mutable
import scala.io.Source
import collection.JavaConversions._
import collection.mutable.{HashMap,MultiMap,Map,Set}

/**
 * Created by root on 3/5/15.
 */
class ReturnedSourcePlagMatches extends Actor {
  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  val classify_plagiarisation =context.actorOf(Props[TF_IDF_Classification], name= "classification")
  var mixed_plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var mixed_source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]

  var plag_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var source_file_matches =new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
  var file_id_counter :Int=1
  var document_count_ids :Map [String,Int]= Map()

  def receive ={

    case returned_Multimaps(plag_tuple,source_word,times_found,comp_file_ids) =>
      //println(comp_file_ids)
      if(!source_word.isEmpty){
        mixed_source_file_matches.addBinding(source_word.head._1+"@"+comp_file_ids._1+","+comp_file_ids._2,source_word.head._2)
      }
      mixed_plag_file_matches.addBinding(plag_tuple._1+"@"+comp_file_ids._1+","+comp_file_ids._2, plag_tuple._2)

    case End_Of_SourceFile(id_size_filename_total,source_file_words,compared_tuple_w_ids) =>
      if(document_count_ids.contains(compared_tuple_w_ids._3+","+compared_tuple_w_ids._4)){
        document_count_ids=document_count_ids.+(compared_tuple_w_ids._3+","+compared_tuple_w_ids._4 -> (document_count_ids.apply(compared_tuple_w_ids._3+","+compared_tuple_w_ids._4)+1))
      }
      else
        document_count_ids=document_count_ids.+(compared_tuple_w_ids._3+","+compared_tuple_w_ids._4 -> 1)

      //println(external_counter)
      if(document_count_ids.containsValue(256)){                      //p.x. Map(1 -> 256 , 2 ->154)
        val x =document_count_ids.find(x => x._2 == 256).get                //pairnoume to 1 (mono ena ftanei prwto sto 256 giati yparxei "barrier" apo ta mailbox tou kathe routee)
        var new_source_file_matches=mixed_source_file_matches.filterKeys(z => z.substring(z.lastIndexOf("@")+1, z.length() ).==(x._1)) // filtraroume gia na paroume ta keys pou exoun to idio id me to arxeio pou theloume na steiloume gia pereterw epeksergasia
        for(kv <- new_source_file_matches.iterator){
          for(value <- kv._2.seq){
            source_file_matches=source_file_matches.addBinding(kv._1.substring(0,kv._1.lastIndexOf("@")) , value)
          }
          //new_source_file_matches=new_source_file_matches.+(kv._1.substring(0,kv._1.lastIndexOf("@")) -> kv._2)
          new_source_file_matches=new_source_file_matches.-(kv._1)
        }

        var new_plag_file_matches =mixed_plag_file_matches.filterKeys(z => z.substring(z.lastIndexOf("@")+1, z.length() ).==(x._1.toString())) // filtraroume gia na paroume ta keys pou exoun to idio id me to arxeio pou theloume na steiloume gia pereterw epeksergasia

        for(kv <- new_plag_file_matches.iterator){
          for(value <- kv._2.seq){
            plag_file_matches=plag_file_matches.addBinding(kv._1.substring(0,kv._1.lastIndexOf("@")) , value)
          }
          //new_plag_file_matches=new_plag_file_matches.+(kv._1.substring(0,kv._1.lastIndexOf("@")) -> kv._2)
          new_plag_file_matches=new_plag_file_matches.-(kv._1)
        }

        document_count_ids=document_count_ids.-(x._1)

        var  count_matches :Int =0 //plag_file_matches.values.iterator.length
        //plag_file_matches.values.iterator.length
        for(kv <- plag_file_matches.iterator){                  //briskoume ton arithmo twn koinwn leksewn tou plag file
          count_matches=count_matches+kv._2.iterator.length
        }
        source_file_matches.remove("")   //afairoume ta  " " pou vriskontai synithws san koines lekseis
        plag_file_matches.remove("")  //afairoume ta  " " pou vriskontai synithws san koines lekseis

        //println(compared_tuple_w_ids._1+"  and  "+compared_tuple_w_ids._2)
        //println("\t plagiarized file matches: "+plag_file_matches+"\t source file matches: "+source_file_matches)

       classify_plagiarisation.!(calculate_classification(source_file_matches,plag_file_matches,source_file_words,compared_tuple_w_ids,id_size_filename_total))

        //println("count_matches:"+count_matches+"\t for files:"+id_filename_total._3)

        /////Mhdenismos twn arxikwpoihmenwn hashmaps kai external counter (gia na synexistei h sygkrish source me alla plag files)
        plag_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
        source_file_matches=new HashMap[String, scala.collection.mutable.Set[Int]] with scala.collection.mutable.MultiMap [String,Int]
      }

    case _ =>
      println("Unexpected Error On Returning Matches Between Two Examined Files")


  }

}
