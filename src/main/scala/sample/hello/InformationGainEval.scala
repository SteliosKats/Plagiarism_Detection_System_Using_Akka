package sample.hello

import java.io.{BufferedReader, File,FileReader, PrintWriter}

import akka.actor.ActorLogging
import akka.actor.{Props, Actor}
import weka.core.Instances
import weka.core.converters.ConverterUtils.DataSource

import weka.core.Attribute
import scala.collection.mutable.HashMap
import scala.collection.mutable.{Map,Set}
import weka.attributeSelection.{Ranker, InfoGainAttributeEval, AttributeSelection, AttributeEvaluator}
import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.Evaluation
import java.util.Random


/**
 * Created by root on 3/27/15.
 */



class InformationGainEval extends Actor with ActorLogging{
  //val avg_ig =context.actorOf(Props[AveragedIG], name= "average_information_gain")
  def receive ={

   case EvalIG(subseq,classification_map,counter2,plag_id_counter,source_file_counter) =>
      val filepath = new File(".").getAbsolutePath().dropRight(1) + "data/FeaturesSubset"+counter2+".arff"
      val relationName :String="relation-"+counter2
      var feature_file :PrintWriter = new PrintWriter(new File(filepath))
      var attribute_map :List[String]=List()

      feature_file.println(s"@RELATION $relationName\n")
      for(length <- subseq.keys){
        feature_file.println(s"@ATTRIBUTE of_sequence_length-$length NUMERIC")
      }
       //println(subseq.toSeq.sortBy(_._1))
      //println(attribute_map.reverse)

      feature_file.println(s"@ATTRIBUTE class {Plagiarised,Not_Plagiarised}\n")
      feature_file.println("@DATA")

     //val data_string :String= "?,".*(indexi)+data._2+",?".*(attribute_map.length -(indexi +1))+","+classify_plagiarisation
      var data_string :String = new String()
      var data_string_dup :String = new String()
      var plagornot :String = new String()
      var plagornot_dup :String = new String()
      var entered_once :Boolean =false
      var duplicate_found :Boolean =false
      var counter :Int =0
      var found :Boolean =false //an vrethike h timh enos feature se ena value apo to keyvalue tou hashmap
      for(i <- 1 to source_file_counter){
        for ( k <- 1 to plag_id_counter){
          for(data <- subseq.iterator){
            //breakable {
              for(value <- data._2.iterator){
                //println(value)
                if(value.substring(value.lastIndexOf(",")-1,value.length()).==(i+","+k) && counter==0){
                   //println(value.substring(value.lastIndexOf(",")-1,value.length()))
                   data_string=data_string.+(value.substring(0,value.indexOf("@")))+","
                   plagornot=value.substring(value.indexOf("@")+1,value.indexOf(","))
                   //println(data_string)
                   found =true
                   counter+=1
                   //break()
                }
                else if (value.substring(value.lastIndexOf(",")-1,value.length()).==(i+","+k) && counter==1){
                  entered_once=true
                  data_string_dup=data_string_dup.+(value.substring(0,value.indexOf("@")))+","
                  plagornot_dup=value.substring(value.indexOf("@")+1,value.indexOf(","))
                  duplicate_found=true
                }

              }
              counter=0
            //}
            if(found==false)
              data_string=data_string.+("?,")
            else
              found=false
            if(duplicate_found==false)
              data_string_dup=data_string_dup.+("?,")
            else
              duplicate_found=false
          }
          if(plagornot==""){
            plagornot=classification_map.apply(k)
            plagornot_dup=plagornot
          }
          data_string=data_string.+(plagornot)
          data_string_dup=data_string_dup.+(plagornot_dup)
          if(plagornot!=""){          //an den eksetazetai arxeio dhladh an  exei stalthei ws subseq  oxi mia 10ada apo to map alla mikortero megethos (ypoloipo)
            feature_file.println(s"$data_string")
          }
          if(plagornot_dup!="" && entered_once==true){
            feature_file.println(s"$data_string_dup")
          }
          data_string=new String()
          data_string_dup=new String()
          entered_once=false
          plagornot=new String()
          plagornot_dup=new String()
        }
      }
      feature_file.close()

      val source :DataSource = new DataSource(filepath)
      val data :Instances= source.getDataSet()
      if (data.classIndex() == -1)
        data.setClassIndex(data.numAttributes() - 1)


      val attributeSelection :AttributeSelection = new  AttributeSelection()

       val classifier :NaiveBayes =new NaiveBayes()
       classifier.buildClassifier(data)
       val eval :Evaluation = new Evaluation(data)
       eval.crossValidateModel(classifier, data, 10,new Random(1))
       val wF_measure :Double=eval.weightedFMeasure()
       //println(wF_measure)

      val info_gain_eval :InfoGainAttributeEval =new InfoGainAttributeEval()
      info_gain_eval.buildEvaluator(data)
      attributeSelection.setEvaluator(info_gain_eval)

      for (i <-0 to (data.numAttributes()-1) ) {
        val t_attr :Attribute= data.attribute(i)
        var infogain :Double = info_gain_eval.evaluateAttribute(i)
        //if(t_attr.toString().contains("@attribute of_sequence_length-221 numeric")){
          //println("Ok!!!!!! "+infogain)
        context.actorSelection("../../average_information_gain").!(average_ig_calculation(infogain,wF_measure./(data.numAttributes().toDouble),t_attr,counter2))
        //}
      }
      //println(infogainscores)

   case All_Features_Sent =>
     context.actorSelection("../../average_information_gain").!(Routee_Finished)

    case _ =>
      println("Wrong values sent to Information Gain Actor")

  }

}

class AveragedIG extends Actor with ActorLogging{
  val infogainscores :Map[String,Double]=Map()
  var weighted_F_measure:Double = 0.0
  var routee_counter :Int =0
  val workerCount=Runtime.getRuntime().availableProcessors()
  var preserved_values :Set[Int]= Set()
  var max_fold_number :Int=0
  def receive ={
    case average_ig_calculation(infogain,wF_measure,t_attr,fold_number) =>

      if(infogainscores.contains(t_attr.toString())){
        val new_infogain=(infogainscores.apply(t_attr.toString()).+(infogain))./(2)
        infogainscores.put(t_attr.toString(), new_infogain)
      }
      else{
        infogainscores.put(t_attr.toString(), infogain)
      }
      //println(wF_measure)
      if(max_fold_number.<(fold_number))
        max_fold_number=fold_number

      weighted_F_measure=weighted_F_measure.+(wF_measure)


    case Routee_Finished =>
      routee_counter+=1
      var found_non_zero :Boolean=false
      if(routee_counter==workerCount*2){
         if(infogainscores.values.exists( _ > 0.0)){
            found_non_zero=true
            for(kv <- infogainscores.iterator if(!kv._1.contains("@attribute class {Plagiarised,Not_Plagiarised}")) ){
               if(kv._2 >= 0.1){
                 //println(kv._1+"\tand\t"+kv._2)
                 preserved_values=preserved_values.+(kv._1.substring(kv._1.lastIndexOf("-")+1,kv._1.lastIndexOf("numeric")-1).toInt)
               }
            }
           val s_set = collection.immutable.SortedSet[Int]().++(preserved_values)
           val avg_weighted_F_measure =weighted_F_measure./(max_fold_number.toDouble)
           context.actorSelection("../IG_map_chunking/value_m_configuration").!(define_m(s_set,avg_weighted_F_measure))

         }
         if(found_non_zero==false){
           for(kv <- infogainscores.iterator if(!kv._1.contains("@attribute class {Plagiarised,Not_Plagiarised}")) ){
             preserved_values=preserved_values.+(kv._1.substring(kv._1.lastIndexOf("-")+1,(kv._1.lastIndexOf("numeric"))-1).toInt)
           }
           val s_set = collection.immutable.SortedSet[Int]().++(preserved_values)
           //println(weighted_F_measure+"    and    "+max_fold_number)
           val avg_weighted_F_measure =weighted_F_measure./(max_fold_number.toDouble)
           context.actorSelection("../IG_map_chunking/value_m_configuration").!(define_m(s_set,avg_weighted_F_measure))
         }


      }

    case _ =>
      println("Wrong values sent to AveragedIG Actor ")

  }


}


class M_Variable_Definition extends Actor with ActorLogging {
  var frg_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  var rel_y =new HashMap[Int, scala.collection.mutable.Set[String]] with scala.collection.mutable.MultiMap [Int,String]
  def receive ={
    case presend_variables(new_all_frg_y,new_all_rel_y) =>
      frg_y=new_all_frg_y
      rel_y=new_all_rel_y

    case define_m(s_set,avg_weighted_F_measure) =>
        //println("S_set:  "+s_set+"\n And Average weighted F1 Measure:  "+avg_weighted_F_measure)
       val all_features_size :Int= s_set.size.*(2)
       //println(frg_y+" and "+rel_y)
      val local_router2=context.actorSelection("akka.tcp://XmlWrappingSystem@127.0.0.1:5150/user/xls_wrapper")
      local_router2.!(send_lexical_results(frg_y ,rel_y,s_set,avg_weighted_F_measure))

    case _ =>
      println("Wrong Message sent to Actor who defines the m variable ")
  }

}