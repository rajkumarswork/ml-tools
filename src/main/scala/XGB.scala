package ml.tools

import ml.dmlc.xgboost4j.scala.{ XGBoost, DMatrix, Booster }
import ml.dmlc.xgboost4j.LabeledPoint
import io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import java.io._

case class ModelInfo(fields: Map[String, Boolean], label: String, hashSize: Int)

class XGB(fields: Map[String, Boolean], label: String, hashSize: Int) extends Model {
  def this() = this(Map(), "", 0)

  // - traits -
  def fields(): Map[String, Boolean] = fields
  def label(): String = label
  def hashSize(): Int = hashSize

  def trained(): Boolean = model != null
  def train(lps: Iterator[LabeledPoint]): Unit = model = XGB.train(lps)
  def predict(m: Map[String, String]): Float = XGB.predict(model, toLabeledPoint(m))
  def save(path: String): Unit = {
    saveModel(IO.oStream(path))
    saveInfo(IO.oStream(XGB.infoPath(path)))
  }
  def load(path: String): Boolean = {
    val m = loadModel(IO.iStream(path))
    val i = loadInfo(IO.iStream(XGB.infoPath(path)))
    if( m != null && i.isDefined){
      model = model
      info = i.get
      encoder = new SimpleEncoder(info.hashSize)
      true
    }
    else false
  }

  // - api --
  def saveModel(os: OutputStream): Unit =
    if (model != null) {
      XGB.saveModel(model, os)
      os.close
    }
  def saveInfo(os: OutputStream): Unit = {
    os.write(info.asJson.noSpaces.getBytes)
    os.close
  }
  def loadModel(in: InputStream): Booster = {
    val model = XGB.loadModel(in)
    in.close
    model
  }
  def loadInfo(in: InputStream): Option[ModelInfo] = {
    val json = scala.io.Source.fromInputStream(in).mkString
    in.close
    decode[ModelInfo](json) match {
      case Right(modelinfo) => Some(modelinfo)
      case _                => None
    }
  }
  def trainFile(path: String): Unit = model = XGB.trainFile(path)

  // -----
  var model: Booster = null
  var info = ModelInfo(fields, label, hashSize)
  private var encoder = new SimpleEncoder(hashSize)
  private def toLabeledPoint(m: Map[String, String]): LabeledPoint =
    HashTranslate.toLabeledPoint(m, fields, label, encoder)
}

// - Companion Object -
object XGB {

  val Eta = 1
  val MaxDepth = 6
  val Iterations = 2
  val Folds = 5
  val Objective = "binary:logistic"
  val EvalMetric = "auc"

  val params = Map[String, Any]("eta" -> Eta, "max_depth" -> MaxDepth, "objective" -> Objective,
    "eval_metric" -> EvalMetric)

  // training
  def train(lps: Iterator[LabeledPoint]): Booster =
    XGBoost.train(new DMatrix(lps), params, Iterations)

  def trainFile(file: String): Booster =
    XGBoost.train(new DMatrix(file), params, Iterations)

  // prediction
  def predict(m: Booster, lp: LabeledPoint): Float =
    m.predict(new DMatrix(Iterator(lp))).head.head

  def predictFile(m: Booster, svmfile: String): Iterator[Float] =
    m.predict(new DMatrix(svmfile)).toIterator.map(_.head)

  // evaluation
  def crossValidate(lps: Iterator[LabeledPoint]): String =
    XGBoost.crossValidation(new DMatrix(lps), params, Iterations, Folds, null, null, null)
      .mkString("\n")

  def crossValidateFile(svmfile: String): String =
    XGBoost.crossValidation(new DMatrix(svmfile), params, Iterations, Folds, null, null, null)
      .mkString("\n")

  def trainEvaluate(rlps: Iterator[LabeledPoint], tlps: Iterator[LabeledPoint]): Booster = {
    val rdm = new DMatrix(rlps)
    XGBoost.train(rdm, params, Iterations, Map("train" -> rdm, "test" -> new DMatrix(tlps)))
  }
  def auc(m: Booster, rlps: Iterator[LabeledPoint]): Float = {
    val preds = rlps.map(lp ⇒ (predict(m, lp), lp.label >= 0.5))
    Calcs.auc(preds, 1000000)
  }

  // io
  def saveModel(m: Booster, os: OutputStream) = m.saveModel(os)
  def loadModel(is: InputStream) = XGBoost.loadModel(is)
  def loadSamples(is: InputStream): Iterator[LabeledPoint] =
    scala.io.Source.fromInputStream(is).getLines.map(l => HashTranslate.svmToLP(l))

  def infoPath(path: String): String = s"$path.json"
}
