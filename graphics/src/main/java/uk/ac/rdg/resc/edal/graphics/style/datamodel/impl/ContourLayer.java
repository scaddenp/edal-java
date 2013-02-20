package uk.ac.rdg.resc.edal.graphics.style.datamodel.impl;

import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SimpleGrid;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Range2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import uk.ac.rdg.resc.edal.graphics.style.PlottingDatum;
import uk.ac.rdg.resc.edal.graphics.style.DataReadingTypes.PlotType;
import uk.ac.rdg.resc.edal.graphics.style.StyleXMLParser.ColorAdapter;

@XmlType(namespace = Image.NAMESPACE, name = "ContourLayerType")
public class ContourLayer extends ImageLayer {
	
	public enum ContourLineStyle {
		SOLID, DASHED, HEAVY, HIGHLIGHT, MARK, MARK_LINE, STROKE
	}

    @XmlElement(name = "DataFieldName", required = true)
    private String dataFieldName;
    @XmlElement(name = "ScaleMin")
    private Double scaleMin = -50.0;
    @XmlElement(name = "ScaleMax")
    private Double scaleMax = 50.0;
    @XmlElement(name = "AutoscaleEnabled")
    private Boolean autoscaleEnabled = true;
    @XmlElement(name = "NumberOfContours")
    private Double numberOfContours = 10.0;
    @XmlElement(name = "ContourLineColour")
    @XmlJavaTypeAdapter(ColorAdapter.class)
    private Color contourLineColour = Color.BLACK;
    @XmlElement(name = "ContourLineWidth")
    private Integer contourLineWidth = 1;
    @XmlElement(name = "ContourLineStyle")
    private ContourLineStyle contourLineStyle = ContourLineStyle.DASHED;
    @XmlElement(name = "LabelEnabled")
    private Boolean labelEnabled = true;

    private int width = 1000;
    private int height = 1000;
    
    private ContourLayer() {
		super(PlotType.RASTER);
	}
    
    public ContourLayer(String dataFieldName, double scaleMin, double scaleMax, boolean autoscaleEnabled, 
    		double numberOfContours, Color contourLineColour, int contourLineWidth, ContourLineStyle contourLineStyle, boolean labelEnabled) {
    	super(PlotType.RASTER);
    	this.dataFieldName = dataFieldName;
    	this.scaleMin = scaleMin;
    	this.scaleMax = scaleMax;
    	this.autoscaleEnabled = autoscaleEnabled;
    	this.numberOfContours = numberOfContours;
    	this.contourLineColour = contourLineColour;
    	this.contourLineWidth = contourLineWidth;
    	this.contourLineStyle = contourLineStyle;
    	this.labelEnabled = labelEnabled;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    public double getScaleMin() {
		return scaleMin;
	}

	public double getScaleMax() {
		return scaleMax;
	}
	
	public boolean isAutoscaleEnabled() {
		return autoscaleEnabled;
	}

	public double getNumberOfContours() {
		return numberOfContours;
	}
	
	public Color getContourLineColour() {
		return contourLineColour;
	}
	
	public int getContourLineWidth() {
		return contourLineWidth;
	}

	public ContourLineStyle getContourLineStyle() {
		return contourLineStyle;
	}

	public boolean isLabelEnabled() {
		return labelEnabled;
	}
	
	private int getLineStyleInteger() {
		switch (contourLineStyle) {
		case SOLID:
			return ContourLineAttribute.SOLID;
		case DASHED:
			return ContourLineAttribute.DASHED;
		case HEAVY:
			return ContourLineAttribute.HEAVY;
		case HIGHLIGHT:
			return ContourLineAttribute.HIGHLIGHT;
		case MARK:
			return ContourLineAttribute.MARK;
		case MARK_LINE:
			return ContourLineAttribute.MARK_LINE;
		case STROKE:
			return ContourLineAttribute.STROKE;
		default:
			return ContourLineAttribute.DASHED;
		}
	}

	@Override
	protected void drawIntoImage(BufferedImage image, DataReader dataReader) {
		width = image.getWidth();
		height = image.getHeight();
		double[] values = new double[width * height];
        double[] xAxis = new double[width];
        double[] yAxis = new double[height];

        int count = 0;
        for (int i = 0; i < width; i++) {
            xAxis[i] = i;
            for (int j = 0; j < height; j++) {
                yAxis[j] = height - j - 1;
                values[count] = Double.NaN;
                count++;
            }
        }
        
        if (autoscaleEnabled) {
        	scaleMin = Double.MAX_VALUE;
        	scaleMax = -Double.MAX_VALUE;
        }
        
		for (PlottingDatum datum : dataReader.getDataForLayerName(dataFieldName)) {
			double val = datum.getValue().doubleValue();
			values[(height * datum.getGridCoords().getXIndex()) + datum.getGridCoords().getYIndex()]
					= val;
    		if (autoscaleEnabled) {
    			if (val < scaleMin) scaleMin = val;
    			if (val > scaleMax) scaleMax = val;
    		}
	     }
		
		SGTGrid sgtGrid = new SimpleGrid(values, xAxis, yAxis, null);

        CartesianGraph cg = getCartesianGraph(sgtGrid);

        double contourSpacing = (scaleMax - scaleMin) / numberOfContours;

        Range2D contourValues = new Range2D(scaleMin, scaleMax, contourSpacing);

        ContourLevels clevels = ContourLevels.getDefault(contourValues);

        DefaultContourLineAttribute defAttr = new DefaultContourLineAttribute();

        defAttr.setColor(contourLineColour);
        defAttr.setStyle(getLineStyleInteger());
        defAttr.setWidth(contourLineWidth);
        defAttr.setLabelEnabled(labelEnabled);
        clevels.setDefaultContourLineAttribute(defAttr);

        GridAttribute attr = new GridAttribute(clevels);
        attr.setStyle(GridAttribute.CONTOUR);

        CartesianRenderer renderer = CartesianRenderer.getRenderer(cg, sgtGrid, attr);

        Graphics g = image.getGraphics();
        renderer.draw(g);

	}

    private CartesianGraph getCartesianGraph(SGTData data) {
        /*
         * To get fixed size labels we need to set a physical size much smaller
         * than the pixel size (since pixels can't represent physical size).
         * Since the SGT code is so heavily tied into the display mechanism, and
         * a factor of around 100 seems to produce decent results, it's almost
         * certainly measured in inches (96dpi being a fairly reasonable monitor
         * resolution).
         * 
         * Anyway, setting the physical size as a constant factor of the pixel
         * size gives good results.
         * 
         * Font size seems to be ignored.
         */
        double factor = 96;
        double physWidth = width / factor;
        double physHeight = height / factor;

        Layer layer = new Layer("", new Dimension2D(physWidth, physHeight));
        JPane pane = new JPane("id", new Dimension(width, height));
        layer.setPane(pane);
        layer.setBounds(0, 0, width, height);

        CartesianGraph graph = new CartesianGraph();
        // Create Ranges representing the size of the image
        Range2D physXRange = new Range2D(0, physWidth);
        Range2D physYRange = new Range2D(0, physHeight);
        // These transforms convert x and y coordinates to pixel indices
        LinearTransform xt = new LinearTransform(physXRange, data.getXRange());
        LinearTransform yt = new LinearTransform(physYRange, data.getYRange());
        graph.setXTransform(xt);
        graph.setYTransform(yt);
        layer.setGraph(graph);
        return graph;
    }

}
