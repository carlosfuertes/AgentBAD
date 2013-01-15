package mmaker.gui

import org.scalatest.FunSuite

/**
 * User: Antonio Garrote
 * Date: 15/01/2013
 * Time: 09:29
 */
class PlotterViewSuite extends FunSuite {

  test("It should be possible to build and update a plotting chart for the bid/ask spread") {
    val plotter = PlotterView.buildPlotterView("test chart", Array[String]("a","b"))

    plotter.newPoint(1.0f,"a")
    Thread.sleep(1000)
    plotter.newPoint(2.0f,"b")
    Thread.sleep(1000)
    plotter.newPoint(3.0f,"a")
    Thread.sleep(1000)
    plotter.newPoint(4.0f,"b")
  }

}
