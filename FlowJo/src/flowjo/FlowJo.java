/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flowjo;

import com.treestar.flowjo.engine.FEML;
import com.treestar.flowjo.engine.Query;
import com.treestar.flowjo.engine.SimpleQueryCallback;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.IOException;
import java.io.BufferedWriter;

import java.awt.Point;

import javax.swing.Icon;

import com.treestar.flowjo.engine.utility.ClusterPrompter;

import com.treestar.lib.PluginHelper;
import com.treestar.lib.core.ExportFileTypes;
import com.treestar.lib.core.WorkspacePluginInterface;
import com.treestar.lib.xml.SElement;
import com.treestar.lib.xml.XMLUtil;
import com.treestar.lib.core.PopulationPluginInterface;
import com.treestar.lib.core.ExportFileTypes;
import com.treestar.lib.fjml.FJML;
import com.treestar.lib.fjml.types.FileTypes;
import com.treestar.lib.xml.SElement;
import com.treestar.lib.xml.GatingML;
import com.treestar.lib.core.ExternalAlgorithmResults;
import com.treestar.lib.prefs.HomeEnv;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;



/**
 *  Dragon Hacks 2017 FlowJo Experimental Plug In
 *
 * @author Erick Weigel
 */
public class FlowJo implements PopulationPluginInterface {

   
   private List<String> fParameters = new ArrayList<String>(); // the list of $PnN pa
   private SElement fOptions;
   
   
    private File getOutputFolder(SElement workspaceElement) {
        File file = PluginHelper.getWorkspaceAnalysisFolder(workspaceElement);
        if (file != null && file.exists()){
            return file;
        }
        String home = HomeEnv.getInstance().getUserHomeFolder();
        File homeFolder = new File(home);
        if (homeFolder.exists()){
            return homeFolder;
        }
        return null;
    }

   
   public boolean promptForOptions(SElement fcmlQueryElement, List<String> parameterNames){
    
        SElement algorithmElement = getElement();
        ClusterPrompter prompter = new ClusterPrompter(algorithmElement);
    
        if (!prompter.promptForOptions(null, parameterNames, true)) {
            return false;
        }
    
        algorithmElement = prompter.getElement();
        setElement(algorithmElement);
        return true;
    
    }
    
    @Override
    public void setElement(SElement elem){
        
        fOptions = elem.getChild("Option");
        fParameters.clear();
        for (SElement child : elem.getChildren(FJML.Parameter)){
            fParameters.add(child.getString(FJML.name));
        }
    }
    
    @Override
    public SElement getElement (){
    
        SElement result = new SElement(getClass().getSimpleName());
        if(fOptions != null){
            result.addContent(new SElement(fOptions));
        }
        
        for (String pName : fParameters){
            SElement pElem = new SElement(FJML.Parameter);
            pElem.setString(FJML.name, pName);
            result.addContent(pElem);
        }
        return result;
    }
   
        @Override
    public ExternalAlgorithmResults invokeAlgorithm(SElement fcmlElem, File sampleFile, File   
    outputFolder) {
        ExternalAlgorithmResults result = new ExternalAlgorithmResults();

        // 1. Create a CSV file that contains random cluster numbers
        // (CSV file is created in the given output folder)
        File outFile = new File(outputFolder, "Random." + sampleFile.getName() + FileTypes.CSV_SUFFIX);
        Writer output;
        // get the number of clusters from the options XML element
        int numClusters = fOptions == null ? 2 : fOptions.getInt("cluster");
        try {
            // get the number of total events in the sample file, using a a plugin helper method
            int num = PluginHelper.getNumExportedEvents(fcmlElem);
            if (num <= 0)
                return null;
            // now write CSV file with the correct number of rows,
            // each row containing a random cluster number
            output = new BufferedWriter(new FileWriter(outFile));
            for (int i = 0; i < num; i++) {
                output.write("" + fParameters.get(0) + "," + fParameters.get(1));
                output.write("\n");
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2. Set the CSV file to be used by FlowJo to create a derived parameter that is then auto-gated
        result.setCSVFile(outFile);

        // 3. Set the number of clusters, displayed in the workspace //window as the stat value
        result.setStatValue(numClusters);

        // 4. Set the workspace description string, displayed in the workspace window
        result.setWorkspaceString("" + numClusters + " clusters");

        // 5. Create a 2-dimensional table of values, with a header row
        double[][] table = new double[numClusters][numClusters];
        String[] headers = new String[numClusters];
        for (int i = 0; i < numClusters; i++)
        {
            headers[i] = "Column " + i;
            for (int j = 0; j < numClusters; j++)
                table[i][j] = (i+1) * (j+1);
        }
        // 6. Set the table header and the table values in the result object
        result.setTableHeaders(headers);
        result.setValuesTable(table);

        // 7. Create a new formula that describes a derived parameter
        if (fParameters.size() > 1)
        {
            // the formula is the first parameter's value divided by the second parameter's value
            String formula = fParameters.get(0) + " / " + fParameters.get(1);
            // add the formula to the result object (can add more than one)
            result.addDerivedParameterFormula(fParameters.get(0) + fParameters.get(1) + "Ratio", formula);
        }

        // 9. Create a Gating-ML XML element that describes a gate
        // create the XML elements for a 1-D range gate
        SElement gate = new SElement("gating:Gating-ML");
        SElement rectGateElem = new SElement("gating:RectangleGate");
        rectGateElem.setString("gating:id", "TestGate");
        gate.addContent(rectGateElem);
        // create the dimension XML element
        SElement dimElem = new SElement("gating:dimension");
        dimElem.setInt("gating:min", 50000);
        dimElem.setInt("gating:max", 100000);
        rectGateElem.addContent(dimElem);
        // create the parameter name XML element
        SElement fcsDimElem = new SElement("data-type:fcs-dimension");
        fcsDimElem.setString("data-type:name", fParameters.get(0));
        dimElem.addContent(fcsDimElem);
        
        // 10. Set the Gating-ML element in the result
        result.setGatingML(gate.toString());
        
        File outFile2 = new File(outputFolder, "Debug." + sampleFile.getName() + FileTypes.CSV_SUFFIX);
        Writer output2;
        
        
        List<List<Point>> contourPolygons = UtilitityClasses.getContourPolygons(fcmlElem, fParameters.get(0), fParameters.get(1), "%2");
        SimpleRegression linearRegression = new SimpleRegression();  
        try{
            output2 = new BufferedWriter(new FileWriter(outFile2));
            for(int x = 0; x < contourPolygons.size(); x++){
                List<Point> currentContour = contourPolygons.get(x);
                for(int y = 0; y < currentContour.size(); y++){
                    output2.write((int)currentContour.get(y).getX() + "," + (int)currentContour.get(y).getY() + "\n");
                    linearRegression.addData(currentContour.get(y).getX(), currentContour.get(y).getY());
                }
            }
            double regressionSlope = linearRegression.getSlope();
            double regressionIntercept = linearRegression.getIntercept();
            
            output2.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        return result;
    }

   
    @Override
    public String getVersion(){
        return "0.1";
    }

    @Override
    public String getName(){
        return "DragonHacksFlowJo";
    }

     @Override
     public ExportFileTypes useExportFileType() {
         return ExportFileTypes.CSV_SCALE;
     }

     @Override
     public List<String> getParameters(){
         return fParameters;
     }

     @Override
     public Icon getIcon() {
         return null;
     }
}
