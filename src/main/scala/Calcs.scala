package ml.tools

object Calcs {

  // Binary classification AUC from prediction-probability, label
  // Elements are sorted in calculation. Limit elements to limit memory use
  def auc(preds: Iterator[(Float, Boolean)], maxElements: Int = 0, seed: Int = 0): Float = {
    if( seed != 0) util.Random.setSeed( seed )
    val samples = if (maxElements == 0) preds
    else util.Random.shuffle(preds).take(maxElements)
    val labelsByPrediction = samples.toSeq.sortBy(_._1).map(_._2)
    val x = labelsByPrediction.scanLeft((0, 0))((res, e) ⇒ (res._1 +
      (if (e) 1 else 0), res._2 + (if (!e) 1 else 0))).tail
    val trueCount = x.last._1.toFloat
    val falseCount = x.last._2.toFloat

    val points = x.map { case (a, b) ⇒ ((falseCount - b) / falseCount, (trueCount - a) / trueCount) }
      .sortBy { case (a, b) ⇒ (a, b) }

    // Calculate area
    // add boundary points in case they don't already
    val allPoints = Seq((0.0f, 0.0f), (1.0f, 1.0f)) ++ points
    // If a x value has multiple y values take the max
    val maxPoints = allPoints.groupBy(_._1).map { case (x, ps) ⇒ (x, ps.map(_._2).max) }
      .toSeq.sortBy(_._1)
    val whs = maxPoints.zip(maxPoints.tail.map(_._1)).map { case ((x1, y), x2) ⇒ (x2 - x1, y) }
    val area = whs.foldLeft(0.0)((sum, x) ⇒ sum + (x._1 * x._2))
    area.toFloat
  }
}
