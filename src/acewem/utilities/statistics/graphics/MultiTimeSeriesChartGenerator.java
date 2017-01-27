package acewem.utilities.statistics.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import sim.util.media.chart.ChartGenerator;
import sim.util.media.chart.TimeSeriesAttributes;

public class MultiTimeSeriesChartGenerator extends ChartGenerator {

	protected ArrayList<XYSeriesCollection> datasetCollection;

	protected XYSeriesCollection datasets;

	protected ArrayList stoppables = new ArrayList();

	DatasetChangeEvent updateEvent;

	public int addSeries(final XYSeries series,
			final org.jfree.data.general.SeriesChangeListener stopper,
			final int axis) {

		datasets.addSeries(series);

		if (axis + 1 > datasetCollection.size()) {

			datasetCollection.add(new XYSeriesCollection());
			chart.getXYPlot().setDataset(axis, datasetCollection.get(axis));
			final NumberAxis tempAxis = new NumberAxis(series.getDescription());
			chart.getXYPlot().setRangeAxis(axis, tempAxis);
			chart.getXYPlot().setRangeAxisLocation(axis,
					AxisLocation.BOTTOM_OR_RIGHT);
			chart.getXYPlot().mapDatasetToRangeAxis(axis, new Integer(axis));

		}

		final TimeSeriesAttributes csa = new TimeSeriesAttributes(this, series,
				datasets.getSeriesCount(), stopper);
		datasetCollection.get(axis).addSeries(series);
		chart.getXYPlot().setRenderer(axis, new StandardXYItemRenderer());
		seriesAttributes.add(csa);

		chart.getXYPlot()
				.getRenderer(axis)
				.setSeriesPaint(
						datasetCollection.get(axis).getSeriesCount() - 1,
						csa.getStrokeColor());
		chart.getXYPlot()
				.getRenderer(axis)
				.setSeriesStroke(
						datasetCollection.get(axis).getSeriesCount() - 1,
						new BasicStroke((float) csa.getThickness(),
								BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		chart.getXYPlot().getRangeAxis(axis)
				.setLabelPaint(csa.getStrokeColor());
		chart.getXYPlot().getRangeAxis(axis)
				.setTickLabelPaint(csa.getStrokeColor());
		chart.getXYPlot().getRangeAxis(axis).setLabel(csa.getSeriesName());

		stoppables.add(stopper);
		revalidate();

		return datasets.getSeriesCount();
	}

	/**
	 * Adds a series, plus a (possibly null) SeriesChangeListener which will
	 * receive a <i>single</i> event if/when the series is deleted from the
	 * chart by the user. The series should have a key in the form of a String.
	 * Returns the series index number.
	 */

	public int addSeries(final XYSeries series,
			final org.jfree.data.general.SeriesChangeListener stopper,
			final int axis, final Color color) {

		datasets.addSeries(series);

		if (axis + 1 > datasetCollection.size()) {

			datasetCollection.add(new XYSeriesCollection());
			chart.getXYPlot().setDataset(axis, datasetCollection.get(axis));
			final NumberAxis tempAxis = new NumberAxis(series.getDescription());
			chart.getXYPlot().setRangeAxis(axis, tempAxis);
			chart.getXYPlot().setRangeAxisLocation(axis,
					AxisLocation.BOTTOM_OR_RIGHT);
			chart.getXYPlot().mapDatasetToRangeAxis(axis, new Integer(axis));

		}

		final TimeSeriesAttributes csa = new TimeSeriesAttributes(this, series,
				datasets.getSeriesCount(), stopper);
		datasetCollection.get(axis).addSeries(series);
		chart.getXYPlot().setRenderer(axis, new StandardXYItemRenderer());
		seriesAttributes.add(csa);

		csa.setStrokeColor(color);
		chart.getXYPlot()
				.getRenderer(axis)
				.setSeriesPaint(
						datasetCollection.get(axis).getSeriesCount() - 1,
						csa.getStrokeColor());
		chart.getXYPlot()
				.getRenderer(axis)
				.setSeriesStroke(
						datasetCollection.get(axis).getSeriesCount() - 1,
						new BasicStroke((float) csa.getThickness(),
								BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		chart.getXYPlot().getRangeAxis(axis)
				.setLabelPaint(csa.getStrokeColor());
		chart.getXYPlot().getRangeAxis(axis)
				.setTickLabelPaint(csa.getStrokeColor());

		chart.getXYPlot().getRangeAxis(axis)
				.setLabelPaint(csa.getStrokeColor());
		chart.getXYPlot().getRangeAxis(axis)
				.setTickLabelPaint(csa.getStrokeColor());

		chart.getXYPlot().getRangeAxis(axis).setLabel(csa.getSeriesName());

		stoppables.add(stopper);
		revalidate();

		return datasets.getSeriesCount();
	}

	protected void buildChart() {

		datasetCollection = new ArrayList<XYSeriesCollection>();
		datasetCollection.add(new XYSeriesCollection());

		datasets = new XYSeriesCollection();

		chart = ChartFactory.createXYLineChart("Untitled Chart",
				"Untitled X Axis", "Untitled Y Axis", datasetCollection.get(0),
				PlotOrientation.VERTICAL, false, true, false);
		((XYLineAndShapeRenderer) (((XYPlot) (chart.getPlot())).getRenderer()))
				.setDrawSeriesLineAsPath(true);

		chart.setAntiAlias(false);

		chartPanel = new ChartPanel(chart, true);
		chartPanel.setPreferredSize(new java.awt.Dimension(380, 300));
		chartPanel.setMinimumDrawHeight(10);
		chartPanel.setMaximumDrawHeight(2000);
		chartPanel.setMinimumDrawWidth(20);
		chartPanel.setMaximumDrawWidth(2000);
		chartHolder.getViewport().setView(chartPanel);

	}

	public XYPlot getPlot() {
		return chart.getXYPlot();
	}

	public XYDataset getSeriesDataset() {
		return datasets;
	}

	public void moveSeries(final int index, final boolean up) {

	}

	public void removeSeries(final int index) {

	}

	public void update() {
		if (updateEvent == null) {
			updateEvent = new DatasetChangeEvent(chart.getPlot(), null);
		}
		chart.getPlot().datasetChanged(updateEvent);
	}

}
