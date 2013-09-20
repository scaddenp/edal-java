package uk.ac.rdg.resc.edal.graphics.style;

import java.awt.Color;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourMap;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourScale;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.FlatOpacity;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ImageLayer;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.InterpolateColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.PaletteColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.RasterLayer;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ThresholdColourScheme;
import static uk.ac.rdg.resc.edal.graphics.style.StyleSLDParser.decodeColour;

public class SLDRasterSymbolizer implements SLDSymbolizer {

	private String layerName;
	private Node symbolizerNode;
	private ImageLayer imageLayer;
	
	public SLDRasterSymbolizer(String layerName, Node symbolizerNode) throws SLDException {
		try {
			this.layerName = layerName;
			this.symbolizerNode = symbolizerNode;
			imageLayer = parseSymbolizer();
		} catch (Exception e) {
			throw new SLDException(e);
		}
	}

	@Override
	public String getLayerName() {
		return layerName;
	}

	@Override
	public Node getSymbolizerNode() {
		return symbolizerNode;
	}

	@Override
	public ImageLayer getImageLayer() {
		return imageLayer;
	}

	/*
	 * Parse symbolizer using XPath
	 */
	private ImageLayer parseSymbolizer() throws XPathExpressionException, NumberFormatException, SLDException {
		// make sure layer is not null and an element node
		if (symbolizerNode == null || symbolizerNode.getNodeType() != Node.ELEMENT_NODE) {
			throw new SLDException("The symbolizer node cannot be null and must be an element node.");
		}
				
		XPath xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(new SLDNamespaceResolver());
		
		// get opacity element if it exists
		Node opacityNode = (Node) xPath.evaluate(
				"./se:Opacity", symbolizerNode, XPathConstants.NODE);
		String opacity = null;
		if (opacityNode != null) {
			opacity = opacityNode.getTextContent();
		}
		
		// get the function defining the colour map
		Node function = (Node) xPath.evaluate(
				"./se:ColorMap/*", symbolizerNode, XPathConstants.NODE);
		if (function == null || function.getNodeType() != Node.ELEMENT_NODE) {
			throw new SLDException("The raster symbolizer must contain a function.");
		}
		
		// get fall back value
		String fallbackValue = (String) xPath.evaluate(
				"./@fallbackValue", function, XPathConstants.STRING);
		Color noDataColour;
		if (fallbackValue == null) {
			noDataColour = null;
		} else {
			noDataColour = decodeColour(fallbackValue);
		}
		
		// parse function specific parts of XML for colour scheme
		ColourScheme colourScheme;
		if (function.getLocalName().equals("Categorize")) {
			colourScheme = parseCategorize(xPath, function, noDataColour);
		} else if (function.getLocalName().equals("Interpolate")) {
			colourScheme = parseInterpolate(xPath, function, noDataColour);			
		} else if (function.getLocalName().equals("Palette")) {
			colourScheme = parsePalette(xPath, function, noDataColour);
		} else {
			throw new SLDException("The function must be one of Categorize, Interpolate or Palette.");
		}
		
		// instantiate a new raster layer and add it to the image
		RasterLayer rasterLayer = new RasterLayer(layerName, colourScheme);
		if (!(opacity == null)) {
			rasterLayer.setOpacityTransform(new FlatOpacity(Float.parseFloat(opacity)));
		}
		return rasterLayer;
	}

	private ColourScheme parseCategorize(XPath xPath, Node function,
			Color noDataColour) throws XPathExpressionException, NumberFormatException, SLDException {
		ColourScheme colourScheme;

		// get list of colours
		NodeList colourNodes = (NodeList) xPath.evaluate(
				"./se:Value", function, XPathConstants.NODESET);
		if (colourNodes == null) {
			throw new SLDException("The categorize function must contain a list ov values.");
		}
		
		// transform to list of Color objects
		ArrayList<Color> colours = new ArrayList<Color>();
		for (int j = 0; j < colourNodes.getLength(); j++) {
			Node colourNode = colourNodes.item(j);
			colours.add(decodeColour(colourNode.getTextContent()));
		}
		
		//get list of thresholds
		NodeList thresholdNodes = (NodeList) xPath.evaluate(
				"./se:Threshold", function, XPathConstants.NODESET);
		if (thresholdNodes == null) {
			throw new SLDException("The categorize function must contain a list of thresholds.");
		}
		
		// transform to list of Floats
		ArrayList<Float> thresholds = new ArrayList<Float>();
		for (int j = 0; j < thresholdNodes.getLength(); j++) {
			Node thresholdNode = thresholdNodes.item(j);
			thresholds.add(Float.parseFloat(thresholdNode.getTextContent()));
		}
		
		colourScheme = new ThresholdColourScheme(thresholds, colours, noDataColour);
		return colourScheme;
	}

	private ColourScheme parseInterpolate(XPath xPath, Node function,
			Color noDataColour) throws XPathExpressionException, NumberFormatException, SLDException {
		ColourScheme colourScheme;

		// get list of data points
		NodeList pointNodes = (NodeList) xPath.evaluate(
				"./se:InterpolationPoint", function, XPathConstants.NODESET);
		if (pointNodes == null) {
			throw new SLDException("The interpolate function must contain a list of interpolation points.");
		}
		
		// parse into an ArrayList
		ArrayList<InterpolateColourScheme.InterpolationPoint> points =
				new ArrayList<InterpolateColourScheme.InterpolationPoint>();
		for (int j = 0; j < pointNodes.getLength(); j++) {
			Node pointNode = pointNodes.item(j);
			Node dataNode = (Node) xPath.evaluate(
					"./se:Data", pointNode, XPathConstants.NODE);
			if (dataNode == null) {
				throw new SLDException("Each interpolation point must contain a data element.");
			}
			Node valueNode = (Node) xPath.evaluate(
					"./se:Value", pointNode, XPathConstants.NODE);
			if (valueNode == null) {
				throw new SLDException("Each interpolation point must contain a value element.");
			}
			points.add(new InterpolateColourScheme.InterpolationPoint(
					Float.parseFloat(dataNode.getTextContent()),
					decodeColour(valueNode.getTextContent())));
		}

		// create a new InterpolateColourScheme object
		colourScheme = new InterpolateColourScheme(points, noDataColour);
		return colourScheme;
	}

	private ColourScheme parsePalette(XPath xPath, Node function,
			Color noDataColour) throws XPathExpressionException, NumberFormatException, SLDException {
		ColourScheme colourScheme;

		// Create the colour map
		String paletteDefinition = (String) xPath.evaluate(
				"./resc:PaletteDefinition", function, XPathConstants.STRING);
		if (paletteDefinition == null || paletteDefinition.equals("")) {
			throw new SLDException("The palette function must contain a palette definition.");
		}
		String nColourBandsText = (String) xPath.evaluate(
				"./resc:NumberOfColorBands", function, XPathConstants.STRING);
		Integer nColourBands;
		if (nColourBandsText == null || nColourBandsText.equals("")) {
			nColourBands = 254;
		} else {
			nColourBands = Integer.parseInt(nColourBandsText);
		}
		String belowMinColourText = (String) xPath.evaluate(
				"./resc:BelowMinColor", function, XPathConstants.STRING);
		Color belowMinColour = decodeColour(belowMinColourText);
		String aboveMaxColourText = (String) xPath.evaluate(
				"./resc:AboveMaxColor", function, XPathConstants.STRING);
		Color aboveMaxColour = decodeColour(aboveMaxColourText);
		ColourMap colourMap = new ColourMap(belowMinColour, aboveMaxColour, noDataColour, paletteDefinition, nColourBands);
		
		// Create the colour scale
		String scaleMinText = (String) xPath.evaluate(
				"./resc:ColorScale/resc:ScaleMin", function, XPathConstants.STRING);
		if (scaleMinText == null || scaleMinText.equals("")) {
			throw new SLDException("The scale minimum must be specified in a colour scale.");
		}
		Float scaleMin = Float.parseFloat(scaleMinText);
		String scaleMaxText = (String) xPath.evaluate(
				"./resc:ColorScale/resc:ScaleMax", function, XPathConstants.STRING);
		if (scaleMaxText == null || scaleMaxText.equals("")) {
			throw new SLDException("The scale maximum must be specified in a colour scale.");
		}
		Float scaleMax = Float.parseFloat(scaleMaxText);
		String logarithmicText = (String) xPath.evaluate(
				"./resc:ColorScale/resc:Logarithmic", function, XPathConstants.STRING);
		Boolean logarithmic = Boolean.parseBoolean(logarithmicText);
		ColourScale colourScale = new ColourScale(scaleMin, scaleMax, logarithmic);
		
		// Create the colour scheme
		colourScheme = new PaletteColourScheme(colourScale, colourMap);
		return colourScheme;
	}

}