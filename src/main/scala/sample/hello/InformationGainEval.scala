package sample.hello

import java.io.{BufferedReader, File,FileReader, PrintWriter}

import akka.actor.ActorLogging
import akka.actor.{Props, Actor}
import weka.core.Instances
import weka.core.converters.ConverterUtils.DataSource

import weka.core.Attribute
import scala.collection.mutable.HashMap
import weka.attributeSelection.{Ranker, InfoGainAttributeEval, AttributeSelection, AttributeEvaluator}
import scala.util.control.Breaks._

/**
 * Created by root on 3/27/15.
 */
class InformationGainEval extends Actor with ActorLogging{
  val avg_ig =context.actorOf(Props[AveragedIG], name= "average_information_gain")
  def receive ={

   case EvalIG(subseq,counter2,plag_id_counter,source_file_counter) =>
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
      var plagornot :String = new String()
      var found :Boolean =false //an vrethike h timh enos feature se ena value apo to keyvalue tou hashmap
      for(i <- 1 to source_file_counter){
        for ( k <- 1 to plag_id_counter){
          for(data <- subseq.iterator){
            breakable {
              for(value <- data._2.iterator){
                if(value.substring(value.lastIndexOf(",")-1,value.length()).==(i+","+k)){
                   //println(value.substring(value.indexOf(",")+1,value.length()).toInt)
                   data_string=data_string.+(value.substring(0,value.indexOf("@")))+","
                   plagornot=value.substring(value.indexOf("@")+1,value.indexOf(","))
                   found =true
                   break()
                }

              }
            }
            if(found==false){
              data_string=data_string.+("?,")
            }
            else
              found=false
          }
          data_string=data_string.+(plagornot)
          if(plagornot!=""){          //an den eksetazetai arxeio dhladh an  exei stalthei ws subseq  oxi mia 10ada apo to map alla mikortero megethos (ypoloipo)
            feature_file.println(s"$data_string")
          }
          data_string=new String()
        }
      }
      feature_file.close()

      val source :DataSource = new DataSource(filepath)
      val data :Instances= source.getDataSet()
      if (data.classIndex() == -1)
        data.setClassIndex(data.numAttributes() - 1)


      val attributeSelection :AttributeSelection = new  AttributeSelection()

      val infogainscores :HashMap [Attribute,Double]=new HashMap()//with scala.collection.mutable.Map [Int,Double]()
      val info_gain_eval :InfoGainAttributeEval =new InfoGainAttributeEval()
      info_gain_eval.buildEvaluator(data)
      attributeSelection.setEvaluator(info_gain_eval)

      for (i <-0 to (data.numAttributes()-1) ) {
        val t_attr :Attribute= data.attribute(i)
        //println(t_attr)
        var infogain :Double = info_gain_eval.evaluateAttribute(i)
        //println(infogain)
        if (infogain != 0) {
          infogainscores.put(t_attr, infogain);
        }
      }
      println(infogainscores)
      if(!infogainscores.isEmpty){
        avg_ig.!(average_ig_calculation(infogainscores))
      }


    case _ =>
      println("Wrong values sent to Information Gain Actor")

  }

}

class AveragedIG extends Actor with ActorLogging{

  def receive ={
    case average_ig_calculation(infogainscores) =>
      println(infogainscores)


    case _ =>
      println("Wrong values sent to AveragedIG Actor ")

  }


}