package sample.hello

import java.io.{BufferedReader, File,FileReader, PrintWriter}

import akka.actor.ActorLogging
import akka.actor.{Props, Actor}
import weka.core.Instances
import weka.core.converters.ConverterUtils.DataSource

import weka.core.Attribute
import scala.collection.mutable.HashMap
import weka.attributeSelection.{InfoGainAttributeEval, AttributeSelection, AttributeEvaluator}
import scala.sys.process._

/**
 * Created by root on 3/27/15.
 */
class InformationGainEval extends Actor with ActorLogging{

  def receive ={

    case EvalIG(subseq,y_legths,counter2,classify_plagiarisation) =>
      println(subseq+ "\t"+y_legths+"\t"+classify_plagiarisation)
      val filepath = new File(".").getAbsolutePath().dropRight(1) + "data/FeaturesSubset"+counter2+".arff"
      val relationName :String="relation-"+counter2
      var feature_file :PrintWriter = new PrintWriter(new File(filepath))
      var attribute_map :List[String]=List()

      feature_file.println(s"@RELATION $relationName\n")
      for(key <- subseq.keys ){
        val length=y_legths.apply(key)
        if(!attribute_map.contains(length.toString)){  //if(!attribute_map.contains("@ATTRIBUTE sequence_length-"+length.toString)){
          attribute_map=(length.toString) :: attribute_map  //attribute_map=("@ATTRIBUTE sequence_length-"+length.toString) :: attribute_map
          feature_file.println(s"@ATTRIBUTE sequence_length-$length NUMERIC")
        }
      }
      //println(attribute_map.reverse)

      feature_file.println(s"@ATTRIBUTE class {Plagiarised,Not_Plagiarised}\n")
      feature_file.println("@DATA")

      for(data <- subseq.iterator){
        if(attribute_map.contains(y_legths.apply(data._1).toString)){
          val indexi :Int=attribute_map.reverse.indexWhere(_.toInt == y_legths.apply(data._1))
          val data_string :String= "?,".*(indexi)+data._2+",?".*(attribute_map.length -(indexi +1))+","+classify_plagiarisation
          //println(indexi+"\t"+attribute_map.length)
          //println(data_string)
          feature_file.println(s"$data_string") //feature_file.println(s"${instanceClass.getValuesList.mkString(",")},${instanceClass.sense}")
        }
      }
      feature_file.close()

      Process(" java weka.attributeSelection.InfoGainAttributeEval -s weka.attributeSelection.Ranker -i FeaturesSubset1.arff ")!
      val source :DataSource = new DataSource(filepath)
      val data :Instances= source.getDataSet()
      if (data.classIndex() == -1)
        data.setClassIndex(data.numAttributes() - 1)

      val infogainscores :HashMap [Attribute,Double]=new HashMap()//with scala.collection.mutable.Map [Int,Double]()
      val attributeSelection :AttributeSelection = new  AttributeSelection()
      val info_gain_eval :InfoGainAttributeEval =new InfoGainAttributeEval()
      attributeSelection.setEvaluator(info_gain_eval)
     // println(data.classIndex())

      for (i <-0 to (data.numAttributes()-1) ) {
        val t_attr :Attribute= data.attribute(i)
        //println(t_attr)
        var infogain :Double = info_gain_eval.evaluateAttribute(i)
        //println(infogain)
        if (infogain != 0) {
          infogainscores.put(t_attr, infogain);
        }
      }
    case _ =>

  }

}
