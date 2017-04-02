/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flowjo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.treestar.flowjo.engine.FEML;
import com.treestar.flowjo.engine.Query;
import com.treestar.flowjo.engine.SimpleQueryCallback;
import com.treestar.flowjo.engine.utility.EPluginHelper;
import com.treestar.lib.PluginHelper;
import com.treestar.lib.fcs.ParameterUtil;
import com.treestar.lib.fjml.FJML;
import com.treestar.lib.fjml.types.DisplayType;
import com.treestar.lib.xml.GatingML;
import com.treestar.lib.xml.SElement;

/**
 *
 * @author Erick
 */
public class UtilitityClasses {
    
    
    public static List<List<Point>> getContourPolygons(SElement fcmlElem, String paramXName, String paramYName, String level)
	    {
	        paramXName = ParameterUtil.stripStainName(paramXName);
	        paramYName = ParameterUtil.stripStainName(paramYName);
	        SElement queryElem = new SElement(fcmlElem);
	        SElement fcmlQueryElem = queryElem.getChild(FEML.FcmlQuery);
	        if (fcmlQueryElem == null) return null;
	        fcmlQueryElem.removeChild(FJML.ExternalPopNode);
	        
	        SElement graphElem = new SElement(FJML.Graph);
	        fcmlQueryElem.addContent(graphElem);
	        graphElem.setBool(FJML.smoothing, true);
	        graphElem.setBool("fast", true);
	        graphElem.setString(FJML.type, DisplayType.Contour.toString());

	        SElement axis = new SElement(FJML.Axis);
	        graphElem.addContent(axis);
	        axis.setString(FJML.dimension, FJML.x);
	        axis.setString(FJML.name, paramXName);
	        axis = new SElement(FJML.Axis);
	        graphElem.addContent(axis);
	        axis.setString(FJML.dimension, FJML.y);
	        axis.setString(FJML.name, paramYName);

	        SElement settings = new SElement(FJML.GraphSettings);
	        graphElem.addContent(settings);
	        settings.setString(FJML.level, level);

	        SimpleQueryCallback callback = new SimpleQueryCallback();
	        Query query = new Query(queryElem, callback);
	        query.executeQuery();
	        SElement queryResult = callback.getResultElement();
	        fcmlQueryElem = queryResult.getChild(FEML.FcmlQuery);
	        if (fcmlQueryElem == null) return null;
	        graphElem = fcmlQueryElem.getChild(FJML.Graph);
	        if (graphElem == null) return null;
	        graphElem = graphElem.getChild(FJML.svg);
	        if (graphElem == null) return null;
	        List<List<Point>> result = new ArrayList<List<Point>>();
	        for (SElement pathElem : graphElem.getChildren(FJML.path))
	        {
	        	String pts = pathElem.getString(FJML.d);
		        if (pts.isEmpty())
		           continue;
		        List<Point> polyPts = new ArrayList<Point>();
		        result.add(polyPts);
		        StringTokenizer tokenizer = new StringTokenizer(pts);
		        while (tokenizer.hasMoreTokens())
	            {
	                String token = tokenizer.nextToken();
	                if ("M".equals(token) || ("L".equals(token)))
	                {
	                    String xVal = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
	                    String yVal = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
	                    if (xVal.isEmpty() || yVal.isEmpty())
	                        continue;
	                    polyPts.add(new Point(Integer.parseInt(xVal), Integer.parseInt(yVal)));
	                }
	            }
	        }
	        return result;
	    }
    
}
