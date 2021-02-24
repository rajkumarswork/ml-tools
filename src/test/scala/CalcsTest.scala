package ml.tools

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CalcsTest extends AnyFlatSpec with Matchers {

  def fixture = new {
    val predictionLabels = Iterator[(Float, Boolean)](
      (0.1f, false),
      (0.9f, true),
      (0.2f, false),
      (0.8f, true),
      (0.3f, false),
      (0.7f, false),
      (0.4f, false),
      (0.6f, true),
      (0.5f, false),
      (0.5f, true))
  }

  "auc()" should "compute correctly" in {
    val f = fixture
    val auc = Calcs.auc(f.predictionLabels)
    auc should equal(0.9166667f)
  }

  it should "use compute auc() on a subset when requested" in {
    val f = fixture
    val auc = Calcs.auc(f.predictionLabels, 8, 1)
    auc should equal(0.93333334f)
  }
}
