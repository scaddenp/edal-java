<%@tag description="Displays a layer in the format expected by getCapabilities v1.3.0.  By defining it in a taglib, nested layers are easy." pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/Capabilities" prefix="cap"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%@attribute name="layer" required="true" type="uk.ac.rdg.resc.ncwms.wms.CapabilitiesLayer" %>

<Layer <c:if test="${layer.queryable}">queryable="1"</c:if>>
    <c:if test="${not empty layer.name}">
        <Name>${layer.name}</Name>
    </c:if>
    <c:if test="${not empty layer.title}">
        <Title>${layer.title}</Title>
    </c:if>
    <c:if test="${not empty layer.description}">
        <Abstract>${layer.description}</Abstract>
    </c:if>
    <c:if test="${not empty layer.bbox}">
        <EX_GeographicBoundingBox>
            <westBoundLongitude>${layer.bbox.minX}</westBoundLongitude>
            <eastBoundLongitude>${layer.bbox.maxX}</eastBoundLongitude>
            <southBoundLatitude>${layer.bbox.minY}</southBoundLatitude>
            <northBoundLatitude>${layer.bbox.maxY}</northBoundLatitude>
        </EX_GeographicBoundingBox>
	    <BoundingBox CRS="CRS:84" minx="${layer.bbox.minX}" maxx="${layer.bbox.maxX}" miny="${layer.bbox.minY}" maxy="${layer.bbox.maxY}" />
    </c:if>
    <c:choose>
	    <c:when test="${layer.continuous}">
		    <c:if test="${not empty layer.tExtent}">
		      <c:if test="${not empty layer.tExtent.low}">
	            <Dimension name="time" units="${utils:getTimeAxisUnits(layer.tExtent.low.calendarSystem)}" multipleValues="true" current="true" default="${utils:dateTimeToISO8601(layer.tExtent.high)}">
	                  <c:out value="${utils:dateTimeToISO8601(layer.tExtent.low)}"/>/<c:out value="${utils:dateTimeToISO8601(layer.tExtent.high)}"/>
	            </Dimension>
		      </c:if>
		    </c:if>
		    <c:if test="${not empty layer.vExtent}">
			    <Dimension name="elevation" units="${layer.vExtent.low.coordinateReferenceSystem.units.unitString}" default="${layer.vAxis.high}">
                    <c:out value="${layer.vExtent.low}"/>/<c:out value="${layer.vExtent.high}"/>
			    </Dimension>
		    </c:if>
	    </c:when>
	    <c:otherwise>
		    <c:if test="${not empty layer.tAxis}">
	            <c:set var="tvalues" value="${layer.tAxis.coordinateValues}" />
	            <Dimension name="time" units="${utils:getTimeAxisUnits(layer.tAxis.calendarSystem)}" multipleValues="true" current="true" default="${utils:dateTimeToISO8601(utils:getDefaultTime(tvalues))}">
		             <c:choose>
		                 <c:when test="${verboseTimes}">
		                     <%-- Use the verbose version of the time string --%>
		                     <c:forEach var="tval" items="${tvalues}" varStatus="status">
		                         <c:if test="${status.index > 0}">,</c:if>
		                         <c:out value="${utils:dateTimeToISO8601(tval)}"/>
		                     </c:forEach>
		                 </c:when>
		              <c:otherwise>
		                  <%-- Use the most concise version of the time string --%>
		                  <c:out value="${utils:getTimeStringForCapabilities(tvalues)}" />
		              </c:otherwise>
		             </c:choose>
	            </Dimension>
		    </c:if>
		    <c:if test="${not empty layer.vAxis}">
			    <Dimension name="elevation" units="${layer.vAxis.verticalCrs.units.unitString}" default="${utils:getDefaultElevation(layer.vAxis)}">
			            <%-- Print out the dimension values, comma separated, making sure
			                             that there is no comma at the start or end of the list.  Note that
			                             we can't use ${fn:join} because the z values are an array of doubles,
			                             not strings. --%>
			        <c:forEach var="zval" items="${layer.vAxis.coordinateValues}" varStatus="status">
			            <c:if test="${status.index > 0}">,</c:if>${zval}
			        </c:forEach>
			    </Dimension>
		    </c:if>
	    </c:otherwise>
    </c:choose>
    <c:if test="${not empty layer.styles}">
	    <c:forEach var="style" items="${layer.styles}">
	        <Style>
	            <Name>${style}</Name>
	            <Title>${style}</Title>
	            <Abstract>${style.stylename} style<c:if test="${not empty style.palettename}">, using the ${style.palettename} palette</c:if></Abstract>
	            <c:if test="${not empty style.palettename}">
	                <LegendURL width="${legendWidth}" height="${legendHeight}">
	                    <Format>image/png</Format>
	                    <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}?REQUEST=GetLegendGraphic&amp;PALETTE=${style.palettename}&amp;COLORBARONLY=true&amp;WIDTH=${legendWidth}&amp;HEIGHT=${legendHeight}"/>
	                </LegendURL>
	            </c:if>
	        </Style>
	    </c:forEach>
    </c:if>
    <c:if test="${not empty layer.childLayers}">
        <c:forEach var="childLayer" items="${layer.childLayers}">
            <cap:layer layer="${childLayer}"/>
        </c:forEach>
    </c:if>
</Layer>