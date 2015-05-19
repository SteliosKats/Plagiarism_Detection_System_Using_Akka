package com.scalaner.training

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.process.{CoreLabelTokenFactory, PTBTokenizer}
import java.io.StringReader
import scala.collection.JavaConversions._
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel}
import scala.collection.JavaConversions

case class LabeledToken(token: String, label: String)

class ApplyModel(modelLocation: String) {

  val nerModel = CRFClassifier.getClassifier(modelLocation)

  def createCoreLabel(token: String): CoreLabel = {

    val cl = new CoreLabel()
    cl.setWord(token)
    cl
  }

  def runNER(text: String): IndexedSeq[LabeledToken] = {

    val tokens = new PTBTokenizer(new StringReader(text), new CoreLabelTokenFactory(), "").tokenize.map(_.word()).toList

    val coreLabels = JavaConversions.seqAsJavaList(tokens.map(createCoreLabel(_)))

    nerModel.classifySentence(coreLabels)
      .asInstanceOf[java.util.List[CoreLabel]]
      .map(w => LabeledToken(w.word(), w.get[String](classOf[CoreAnnotations.AnswerAnnotation])))  //,CoreAnnotations.AnswerAnnotation
      .toIndexedSeq
  }

  def runNER(tokens: IndexedSeq[String]) = {

    val coreLabels = JavaConversions.seqAsJavaList(tokens.map(createCoreLabel(_)))

    nerModel.classifySentence(coreLabels)
      .asInstanceOf[java.util.List[CoreLabel]]
      .map(w => LabeledToken(w.word(), w.get[String](classOf[CoreAnnotations.AnswerAnnotation])))  //,CoreAnnotations.AnswerAnnotation
      .toIndexedSeq
  }

}