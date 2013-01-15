package mmaker.gui

import org.jfree.ui.{RefineryUtilities, ApplicationFrame}
import org.jfree.data.time.{Second, DynamicTimeSeriesCollection}
import org.jfree.chart.{ChartPanel, ChartFactory, JFreeChart}
import java.awt.BorderLayout
import scala.collection.mutable.Map

/**
 * User: Antonio Garrote
 * Date: 15/01/2013
 * Time: 08:44
 */
class PlotterView(title:String,series:Array[String]) extends ApplicationFrame(title) {

  // Dataset to collect time series data points
  val dataset:DynamicTimeSeriesCollection= {
    val ds = new DynamicTimeSeriesCollection(series.size, 30, new Second())
    val baseTime:Second = new Second(new java.util.Date)
    ds.setTimeBase(baseTime)

    for(i <- 0 until series.size) { ds.addSeries(Array[Float](), i, series(i)) }

    ds
  }


  // The graphical chart
  val chart:JFreeChart = {
    val result = ChartFactory.createTimeSeriesChart(title, "time", "price", dataset, true, true, false)
    val xyplot = result.getXYPlot
    xyplot.getDomainAxis.setAutoRange(true)
    xyplot.getRangeAxis.setAutoRange(true)

    this.add(new ChartPanel(result), BorderLayout.CENTER)

    result
  }

  var counters:Map[String,Int] = series.foldLeft(Map[String,Int]()){ (m,s) => m.put(s,0); m }


  def newPoint(value:Float, label:String) {
    dataset.advanceTime()
    val num = series.indexWhere(_.equalsIgnoreCase(label))

    val counter = counters(label)


    try {
      dataset.addValue(num,counter,value)
    } catch {
      case e:Exception => {
        println("EXCEPTION ADDING "+num+":"+counter+":"+value)
        println(e.getMessage)
        e.printStackTrace()
      }
    }


    counters(label) = counter+1
  }

}


object PlotterView {
  def buildPlotterView(title:String, series:Array[String]):PlotterView = {
    val plotter:PlotterView = new PlotterView(title, series)
    plotter.pack
    RefineryUtilities.centerFrameOnScreen(plotter)
    plotter.setVisible(true)

    plotter
  }
}