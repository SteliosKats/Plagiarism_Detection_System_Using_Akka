package com.scalaner.training

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.sequences.SeqClassifierFlags
import scala.io.Source
import java.util.StringTokenizer
import java.util.ArrayList


case class Performance(precision: Double, recall: Double, f1: Double)

case class PerformanceCounter(var fp: Double, var fn: Double, var tp: Double)

object NERModel {

  private def setFlags(trainingDataLocation: String, modelSaveLocation: String, features: IndexedSeq[(String, Any)]
                       , gazLocation: String): SeqClassifierFlags = {

    val flags = new SeqClassifierFlags()

    flags.trainFile = trainingDataLocation
    flags.serializeTo = modelSaveLocation
    flags.map = "word=0,answer=1"

    val gazLoc = new ArrayList[String]()

    if (gazLocation != "") {
      val st = new StringTokenizer(gazLocation, " ,;\t")

      while (st.hasMoreTokens) {
        gazLoc.add(st.nextToken())
      }

      flags.gazettes = gazLoc
      flags.useGazettes = true
    }

    //Set all features values
    for (feature <- features) {

      feature match {
        case ("maxLeft", x) => flags.maxLeft = x.asInstanceOf[Int]
        case ("useClassFeature", x) => flags.useClassFeature = x.asInstanceOf[Boolean]
        case ("useWord", x) => flags.useWord = x.asInstanceOf[Boolean]
        case ("useNGrams", x) => flags.useWord = x.asInstanceOf[Boolean]
        case ("noMidNGrams", x) => flags.noMidNGrams = x.asInstanceOf[Boolean]
        case ("maxNGramLeng", x) => flags.maxNGramLeng = x.asInstanceOf[Int]
        case ("usePrev", x) => flags.usePrev = x.asInstanceOf[Boolean]
        case ("useNext", x) => flags.useNext = x.asInstanceOf[Boolean]
        case ("useDisjunctive", x) => flags.useDisjunctive = x.asInstanceOf[Boolean]
        case ("useSequences", x) => flags.useSequences = x.asInstanceOf[Boolean]
        case ("usePrevSequences", x) => flags.usePrevSequences = x.asInstanceOf[Boolean]
        case ("useTypeSeqs", x) => flags.useTypeSeqs = x.asInstanceOf[Boolean]
        case ("useTypeSeqs2", x) => flags.useTypeSeqs2 = x.asInstanceOf[Boolean]
        case ("useTypeySequences", x) => flags.useTypeySequences = x.asInstanceOf[Boolean]
      }
    }

    flags
  }

  def trainClassifier(trainingDataLocation: String, modelSaveLocation: String,
                      features: IndexedSeq[(String, Any)] = FeatureConfig.DEFAULT, gazLocation: String = "") = {

    val nerClassifier = new CRFClassifier(setFlags(trainingDataLocation, modelSaveLocation, features, gazLocation))

    nerClassifier.train()
    nerClassifier.serializeClassifier(modelSaveLocation)
  }

  def testClassifier(testFileLocation: String, classifierLocation: String): Map[String, Performance] = {

    val goldLabels = Source.fromFile(testFileLocation).getLines().map(x => x.split("\t").last).toList

    //Get total number of unique entity types
    val labels = goldLabels.distinct

    var performanceCounts = labels.map(x => x -> PerformanceCounter(0.0, 0.0, 0.0)).toMap
    val testString = Source.fromFile(testFileLocation).getLines().map(x => x.split("\t").head).toIndexedSeq

    val ap = new ApplyModel(classifierLocation)
    val classifierResults = ap.runNER(testString)

    for ((res, idx) <- classifierResults.zipWithIndex) {

      if (res.label == goldLabels(idx)) {
        performanceCounts(res.label).tp += 1
      }
      else {
        performanceCounts(res.label).fn += 1
        performanceCounts(goldLabels(idx)).fp += 1
      }
    }

    performanceCounts.map(x => x._1 -> {
      val p = x._2.tp / (x._2.tp + x._2.fp)
      val r = x._2.tp / (x._2.tp + x._2.fn)
      val f1 = 2 * (p * r) / (p + r)
      Performance(p, r, f1)
    })
  }

}
