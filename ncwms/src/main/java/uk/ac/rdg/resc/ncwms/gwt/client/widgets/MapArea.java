package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import org.gwtopenmaps.openlayers.client.Bounds;
import org.gwtopenmaps.openlayers.client.LonLat;
import org.gwtopenmaps.openlayers.client.Map;
import org.gwtopenmaps.openlayers.client.MapOptions;
import org.gwtopenmaps.openlayers.client.MapWidget;
import org.gwtopenmaps.openlayers.client.Pixel;
import org.gwtopenmaps.openlayers.client.Projection;
import org.gwtopenmaps.openlayers.client.control.EditingToolbar;
import org.gwtopenmaps.openlayers.client.control.LayerSwitcher;
import org.gwtopenmaps.openlayers.client.control.MousePosition;
import org.gwtopenmaps.openlayers.client.control.WMSGetFeatureInfo;
import org.gwtopenmaps.openlayers.client.control.WMSGetFeatureInfoOptions;
import org.gwtopenmaps.openlayers.client.event.EventHandler;
import org.gwtopenmaps.openlayers.client.event.EventObject;
import org.gwtopenmaps.openlayers.client.event.GetFeatureInfoListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadCancelListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadEndListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadStartListener;
import org.gwtopenmaps.openlayers.client.event.MapBaseLayerChangedListener;
import org.gwtopenmaps.openlayers.client.feature.VectorFeature;
import org.gwtopenmaps.openlayers.client.geometry.LineString;
import org.gwtopenmaps.openlayers.client.geometry.Point;
import org.gwtopenmaps.openlayers.client.layer.Image;
import org.gwtopenmaps.openlayers.client.layer.ImageOptions;
import org.gwtopenmaps.openlayers.client.layer.TransitionEffect;
import org.gwtopenmaps.openlayers.client.layer.Vector;
import org.gwtopenmaps.openlayers.client.layer.WMS;
import org.gwtopenmaps.openlayers.client.layer.WMSOptions;
import org.gwtopenmaps.openlayers.client.layer.WMSParams;
import org.gwtopenmaps.openlayers.client.popup.Popup;
import org.gwtopenmaps.openlayers.client.util.JSObject;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.GodivaActionsHandler;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;

public class MapArea extends MapWidget {

    private static final Projection EPSG4326 = new Projection("EPSG:4326");

    private Map map;
    private WMS wmsLayer;
    private Image animLayer;
    private String currentProjection;
    private String baseUrl;

    private WMSOptions wmsPolarOptions;
    private WMSOptions wmsStandardOptions;

    private LayerLoadStartListener loadStartListener;
    private LayerLoadCancelListener loadCancelListener;
    private LayerLoadEndListener loadEndListener;

    private GodivaActionsHandler widgetDisabler;

    private WMSParams params = null;

    private String baseUrlForExport;

    private boolean singleTile = false;
    private boolean lastMapWasSingleTile = false;

    public MapArea(String baseUrl, int width, int height,
            final GodivaActionsHandler godivaListener, String baseMapUrl, String baseMapLayer) {
        super(width + "px", height + "px", getDefaultMapOptions());
        this.baseUrl = baseUrl;
        loadStartListener = new LayerLoadStartListener() {
            @Override
            public void onLoadStart(LoadStartEvent eventObject) {
                godivaListener.setLoading(true);
            }
        };
        loadCancelListener = new LayerLoadCancelListener() {
            @Override
            public void onLoadCancel(LoadCancelEvent eventObject) {
                godivaListener.setLoading(false);
            }
        };
        loadEndListener = new LayerLoadEndListener() {
            @Override
            public void onLoadEnd(LoadEndEvent eventObject) {
                godivaListener.setLoading(false);
            }
        };
        this.widgetDisabler = godivaListener;
        init(baseMapUrl, baseMapLayer);
        map.addMapMoveListener(godivaListener);
        map.addMapZoomListener(godivaListener);

        wmsStandardOptions = new WMSOptions();
        wmsStandardOptions.setWrapDateLine(true);
    }

    public void addAnimationLayer(String layerId, String timeList, String currentElevation,
            String palette, String style, String scaleRange, int nColorBands, boolean logScale) {
        StringBuilder url = new StringBuilder(baseUrl + "?service=WMS&request=GetMap&version=1.1.1");
        url.append("&format=image/gif" + "&transparent=true" + "&styles="+style+"/" + palette
                + "&layers=" + layerId + "&time=" + timeList + "&logscale=" + logScale + "&srs="
                + currentProjection + "&bbox=" + map.getExtent().toBBox(6) + "&width="
                + ((int) map.getSize().getWidth()) + "&height=" + ((int) map.getSize().getHeight()));
        if (scaleRange != null)
            url.append("&colorscalerange=" + scaleRange);
        if (currentElevation != null)
            url.append("&elevation=" + currentElevation.toString());
        if (nColorBands > 0)
            url.append("&numcolorbands=" + nColorBands);
        ImageOptions opts = new ImageOptions();
        opts.setAlwaysInRange(true);
        animLayer = new Image("Animation Layer", url.toString(), map.getExtent(), map.getSize(),
                opts);
        animLayer.addLayerLoadStartListener(loadStartListener);
        animLayer.addLayerLoadCancelListener(new LayerLoadCancelListener() {
            @Override
            public void onLoadCancel(LoadCancelEvent eventObject) {
                stopAnimation();
                loadCancelListener.onLoadCancel(eventObject);
            }
        });
        animLayer.addLayerLoadEndListener(loadEndListener);
        animLayer.setIsBaseLayer(false);
        animLayer.setDisplayInLayerSwitcher(false);
        if (wmsLayer != null)
            wmsLayer.setIsVisible(false);
        map.addLayer(animLayer);
        widgetDisabler.disableWidgets();
    }

    public void stopAnimation() {
        // This stops and removes the animation. We may want a pause method...
        widgetDisabler.enableWidgets();
        if (animLayer != null) {
            map.removeLayer(animLayer);
            animLayer = null;
        }
        wmsLayer.setIsVisible(true);
    }

    public void changeLayer(String layerId, String currentTime, String currentElevation,
            String style, String palette, String scaleRange, int nColorBands, boolean logScale) {
        params = new WMSParams();
        params.setFormat("image/png");
        params.setTransparent(true);
        params.setStyles(style + "/" + palette);
        params.setLayers(layerId);
        if (currentTime != null)
            params.setParameter("TIME", currentTime);
        if (currentElevation != null)
            params.setParameter("ELEVATION", currentElevation.toString());
        if (scaleRange != null)
            params.setParameter("COLORSCALERANGE", scaleRange);
        if (nColorBands > 0)
            params.setParameter("NUMCOLORBANDS", nColorBands + "");
        params.setParameter("LOGSCALE", logScale + "");

        singleTile = (style != null && style.equalsIgnoreCase("vector"));
        if (singleTile != lastMapWasSingleTile && wmsLayer != null) {
            map.removeLayer(wmsLayer);
            wmsLayer = null;
        }
        lastMapWasSingleTile = singleTile;

        WMSOptions options = getOptionsForCurrentProjection();
        changeLayer(params, options);
    }

    private void changeLayer(WMSParams params, WMSOptions options) {
        if (wmsLayer == null) {
            wmsLayer = new WMS("WMS Layer", baseUrl, params, options);
            wmsLayer.addLayerLoadStartListener(loadStartListener);
            wmsLayer.addLayerLoadCancelListener(loadCancelListener);
            wmsLayer.addLayerLoadEndListener(loadEndListener);
            // wmsLayer.setDisplayInLayerSwitcher(false);
            if (animLayer != null)
                animLayer.setIsVisible(false);
            map.addLayer(wmsLayer);
            addGetFeatureInfoLayer();
        } else {
            wmsLayer.mergeNewParams(params);
            wmsLayer.addOptions(options);
            if (animLayer != null)
                animLayer.setIsVisible(false);
            wmsLayer.redraw();
        }
    }

    public void zoomToExtents(String extents) throws Exception {
        if (currentProjection.equalsIgnoreCase("EPSG:32661")
                || currentProjection.equalsIgnoreCase("EPSG:32761")) {
            /*
             * If we have a polar projection, the extents will be wrong. In this
             * case, just ignore the zoom to extents.
             * 
             * This is acceptable behaviour, since if we are looking at polar
             * data, we never want to zoom to the extents of the data
             */
            return;
        }
        try {
            String[] bboxStr = extents.split(",");
            double lowerLeftX = Double.parseDouble(bboxStr[0]);
            double lowerLeftY = Double.parseDouble(bboxStr[1]);
            double upperRightX = Double.parseDouble(bboxStr[2]);
            double upperRightY = Double.parseDouble(bboxStr[3]);
            map.zoomToExtent(new Bounds(lowerLeftX, lowerLeftY, upperRightX, upperRightY));
        } catch (Exception e) {
            throw e;
        }
    }

    public String getKMZUrl() {
        String url;
        if (params != null && wmsLayer != null) {
            url = wmsLayer.getFullRequestString(params, null);
            url = url + "&height=" + (int) map.getSize().getHeight() + "&width="
                    + (int) map.getSize().getWidth() + "&bbox=" + map.getExtent().toBBox(6);
            url = url.replaceAll("image/png", "application/vnd.google-earth.kmz");
            url = url.replaceAll("image%2Fpng", "application%2Fvnd.google-earth.kmz");
        } else {
            url = null;
        }
        return url;
    }

    private static MapOptions getDefaultMapOptions() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.setProjection("EPSG:4326");
        mapOptions.setUnits("m");
        mapOptions.setDisplayProjection(EPSG4326);
        return mapOptions;
    }

    private void init(String baseMapUrl, String baseMapLayer) {
        this.setStylePrimaryName("mapStyle");
        map = this.getMap();

        // WMS baseLayer;
        // WMSOptions wmsOptions = new WMSOptions();
        // wmsOptions.setWrapDateLine(true);
        //
        // WMSParams wmsParams = new WMSParams();
        // wmsParams.setLayers(baseMapLayer);
        // baseLayer = new WMS("Base Map", baseMapUrl, wmsParams, wmsOptions);
        // baseLayer.addLayerLoadStartListener(loadStartListener);
        // baseLayer.addLayerLoadEndListener(loadEndListener);
        // baseLayer.setIsBaseLayer(true);
        //
        // map.addLayer(baseLayer);
        // map.setBaseLayer(baseLayer);
        addBaseLayers();
        baseUrlForExport = baseMapUrl + "&LAYERS=" + baseMapLayer;

        currentProjection = map.getProjection();

        map.addMapBaseLayerChangedListener(new MapBaseLayerChangedListener() {
            @Override
            public void onBaseLayerChanged(MapBaseLayerChangedEvent eventObject) {
                String url = eventObject.getLayer().getJSObject().getPropertyAsString("url");
                String layers = eventObject.getLayer().getJSObject().getPropertyAsArray("params")[0]
                        .getPropertyAsString("LAYERS");
                baseUrlForExport = url + (url.contains("?") ? "&" : "?") + "LAYERS="
                        + URL.encode(layers);
                if (!map.getProjection().equals(currentProjection)) {
                    map.zoomToMaxExtent();
                    currentProjection = map.getProjection();
                }
            }
        });

        map.addControl(new LayerSwitcher());
        /*
         * TODO use CSS to remove the unwanted icons and implement the drawing
         * layer better
         */
//        map.addControl(new EditingToolbar(new Vector("Drawing")));
        addDrawingLayer();
        map.addControl(new MousePosition());
        map.setCenter(new LonLat(0.0, 0.0), 2);
        map.setMaxExtent(new Bounds(-180, -360, 180, 360));
    }

    public void setOpacity(float opacity) {
        wmsLayer.setOpacity(opacity);
    }

    public String getBaseLayerUrl() {
        return baseUrlForExport;
    }

    private void addBaseLayers() {
        WMS openLayers;
        WMS bluemarbleDemis;
        WMS demis;
        WMS plurel;
        WMS weather;
        WMS srtmDem;
        WMS northPole;
        WMS southPole;

        WMSParams wmsParams;
        WMSOptions wmsOptions;
        wmsOptions = new WMSOptions();
        wmsOptions.setWrapDateLine(true);
        wmsParams = new WMSParams();
        wmsParams.setLayers("basic");
        openLayers = new WMS("OpenLayers WMS", "http://labs.metacarta.com/wms-c/Basic.py?",
                wmsParams, wmsOptions);
        openLayers.addLayerLoadStartListener(loadStartListener);
        openLayers.addLayerLoadEndListener(loadEndListener);
        openLayers.setIsBaseLayer(true);
        wmsParams = new WMSParams();
        wmsParams.setLayers("Earth Image");
        bluemarbleDemis = new WMS("Demis Blue Marble",
                "http://www2.demis.nl/wms/wms.ashx?WMS=BlueMarble", wmsParams, wmsOptions);
        bluemarbleDemis.setIsBaseLayer(true);
        bluemarbleDemis.addLayerLoadStartListener(loadStartListener);
        bluemarbleDemis.addLayerLoadEndListener(loadEndListener);
        wmsParams = new WMSParams();
        wmsParams
                .setLayers("Countries,Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,"
                        + "Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports");
        wmsParams.setFormat("image/png");
        demis = new WMS("Demis WMS", "http://www2.demis.nl/wms/wms.ashx?WMS=WorldMap", wmsParams,
                wmsOptions);
        demis.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("0,2,3,4,5,8,9,10,40");
        plurel = new WMS("PLUREL_WMS",
                "http://plurel.jrc.ec.europa.eu/ArcGIS/services/worldwithEGM/mapserver/wmsserver?",
                wmsParams, wmsOptions);
        plurel.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("base,global_ir_satellite_10km,radar_precip_mode");
        weather = new WMS("Latest Clouds",
                "http://maps.customweather.com/image?client=ucl_test&client_password=t3mp",
                wmsParams, wmsOptions);
        weather.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("bluemarble,srtm30");
        srtmDem = new WMS("SRTM DEM", "http://iceds.ge.ucl.ac.uk/cgi-bin/icedswms?", wmsParams,
                wmsOptions);
        srtmDem.setIsBaseLayer(true);

        Bounds polarMaxExtent = new Bounds(-10700000, -10700000, 14700000, 14700000);
        double halfSideLength = (polarMaxExtent.getUpperRightY() - polarMaxExtent.getLowerLeftY())
                / (4 * 2);
        double centre = ((polarMaxExtent.getUpperRightY() - polarMaxExtent.getLowerLeftY()) / 2)
                + polarMaxExtent.getLowerLeftY();
        double low = centre - halfSideLength;
        double high = centre + halfSideLength;
        float polarMaxResolution = (float) ((high - low) / 256.0);
        double windowLow = centre - 2 * halfSideLength;
        double windowHigh = centre + 2 * halfSideLength;
        Bounds polarBounds = new Bounds(windowLow, windowLow, windowHigh, windowHigh);

        wmsParams = new WMSParams();
        wmsParams.setLayers("bluemarble_file");
        wmsParams.setFormat("image/jpeg");

        wmsPolarOptions = new WMSOptions();
        wmsPolarOptions.setProjection("EPSG:32661");
        wmsPolarOptions.setMaxExtent(polarBounds);
        wmsPolarOptions.setMaxResolution(polarMaxResolution);
        wmsPolarOptions.setTransitionEffect(TransitionEffect.RESIZE);
        wmsPolarOptions.setWrapDateLine(false);
        northPole = new WMS("North polar stereographic", "http://wms-basemaps.appspot.com/wms",
                wmsParams, wmsPolarOptions);

        wmsPolarOptions.setProjection("EPSG:32761");
        southPole = new WMS("South polar stereographic", "http://wms-basemaps.appspot.com/wms",
                wmsParams, wmsPolarOptions);

        map.addLayer(openLayers);
        map.addLayer(bluemarbleDemis);
        map.addLayer(demis);
        map.addLayer(plurel);
        map.addLayer(weather);
        map.addLayer(srtmDem);
        map.addLayer(northPole);
        map.addLayer(southPole);
        map.setBaseLayer(openLayers);
        map.setBaseLayer(bluemarbleDemis);
        currentProjection = map.getProjection();
        map.addMapBaseLayerChangedListener(new MapBaseLayerChangedListener() {
            @Override
            public void onBaseLayerChanged(MapBaseLayerChangedEvent eventObject) {
                String url = eventObject.getLayer().getJSObject().getPropertyAsString("url");
                String layers = eventObject.getLayer().getJSObject().getPropertyAsArray("params")[0]
                        .getPropertyAsString("LAYERS");
                baseUrlForExport = url + (url.contains("?") ? "&" : "?") + "LAYERS="
                        + URL.encode(layers);
                if (!map.getProjection().equals(currentProjection)) {
                    currentProjection = map.getProjection();
                    if(wmsLayer != null){
                        map.removeLayer(wmsLayer);
                        wmsLayer = null;
                    }
                    if(params != null)
                        changeLayer(params, getOptionsForCurrentProjection());
                    map.zoomToMaxExtent();
                }
            }
        });
    }

    private WMSOptions getOptionsForCurrentProjection() {
        if (currentProjection.equalsIgnoreCase("EPSG:32661")
                || currentProjection.equalsIgnoreCase("EPSG:32761")) {
            wmsPolarOptions.setSingleTile(singleTile);
            return wmsPolarOptions;
        } else {
            wmsStandardOptions.setSingleTile(singleTile);
            return wmsStandardOptions;
        }
    }
    
    private void addGetFeatureInfoLayer(){
        WMSGetFeatureInfoOptions getFeatureInfoOptions = new WMSGetFeatureInfoOptions();
        getFeatureInfoOptions.setQueryVisible(true);
        getFeatureInfoOptions.setInfoFormat("text/xml");

        WMS[] layers = new WMS[]{wmsLayer};
        getFeatureInfoOptions.setLayers(layers);

        WMSGetFeatureInfo getFeatureInfo = new WMSGetFeatureInfo(getFeatureInfoOptions);

        getFeatureInfo.addGetFeatureListener(new GetFeatureInfoListener() {
            @Override
            public void onGetFeatureInfo(GetFeatureInfoEvent eventObject) {
                String pixels[] = eventObject.getJSObject().getProperty("xy").toString().split(",");
                LonLat lonLat = MapArea.this.map.getLonLatFromPixel(
                        new Pixel(
                                Integer.parseInt(pixels[0].substring(2)),
                                Integer.parseInt(pixels[1].substring(2))));
                String message = processFeatureInfo(eventObject.getText());
                Popup popup = new Popup("info_popup", lonLat, null, message, true);
                popup.setAutoSize(true);
                popup.setBackgroundColor("cornsilk");
                popup.setBorder("1px solid");
                MapArea.this.map.addPopupExclusive(popup);
            }
        });
        map.addControl(getFeatureInfo);
        getFeatureInfo.activate();
    }

    private String processFeatureInfo(String text) {
        Document featureInfo = XMLParser.parse(text);
        String lon = featureInfo.getElementsByTagName("longitude").item(0).getChildNodes().item(0)
                .getNodeValue();
        String lat = featureInfo.getElementsByTagName("latitude").item(0).getChildNodes().item(0)
                .getNodeValue();
        String val = featureInfo.getElementsByTagName("value").item(0).getChildNodes().item(0)
                .getNodeValue();
        String html = "<b>Longitude:</b> " + lon + "<br>" + "<b>Latitude: </b> " + lat + "<br>"
                + "<b>Value:    </b> " + val;
        return html;
    }
    
    private void addDrawingLayer() {
        Vector drawingLayer = new Vector("Drawing");
        drawingLayer.setDisplayInLayerSwitcher(false);
        drawingLayer.getEvents().register("featureadded", drawingLayer, new EventHandler() {
            @Override
            public void onHandle(EventObject eventObject) {
                JSObject featureJs = eventObject.getJSObject().getProperty("feature");
                JSObject lineStringJs = VectorFeature.narrowToVectorFeature(featureJs).getGeometry().getJSObject();
                LineString line = LineString.narrowToLineString(lineStringJs);
                Point[] points = line.getComponents();
                StringBuilder lineStringBuilder = new StringBuilder();
                for (int i = 0; i < points.length - 1; i++) {
                    lineStringBuilder.append(points[i].getX() + " " + points[i].getY() + ", ");
                }
                lineStringBuilder.append(points[points.length - 1].getX() + " " + points[points.length - 1].getY());
                String transectUrl = "wms" + 
                                     "?REQUEST=GetTransect" + 
                                     "&LAYER=" + wmsLayer.getParams().getLayers() + 
                                     "&CRS=" + currentProjection +
//                                      "&ELEVATION=" + wmsLayer.getParams() +
                                     // "&TIME=" + time.isoTValue +
                                     "&LINESTRING=" + lineStringBuilder + 
                                     "&FORMAT=image/png";
                String elevation = wmsLayer.getParams().getJSObject().getPropertyAsString("ELEVATION");
                String time = wmsLayer.getParams().getJSObject().getPropertyAsString("TIME");
                if(elevation != null && !elevation.equals("")){
                    transectUrl += "&ELEVATION="+elevation;
                }
                if(time != null && !time.equals("")){
                    transectUrl += "&TIME="+time;
                }
                Window.open(transectUrl, "_blank", null);
            }
        });
        map.addControl(new EditingToolbar(drawingLayer));
    }
}