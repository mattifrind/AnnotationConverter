/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package annotationconverter;

import java.io.File;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author matti
 */
public class ReadMetadata {
    public static int errors;
    
    public static double[] readCamAngles(String filename) {
        double pitch=Double.NaN;
        double roll=Double.NaN;
        try {
            File file = new File(AnnotationConverter.IMAGE_DIRECTORY + filename);
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            try {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis, true);
                    IIOMetadata metadata = reader.getImageMetadata(0);
                    String[] names = metadata.getMetadataFormatNames();
                    int length = names.length;
                    for (int i = 0; i < length; i++) {
                        Node root = metadata.getAsTree(names[i]);
                        NamedNodeMap map = root.getAttributes();
                        double pitchRead=displayMetadata(root,"pitch");
                        double rollRead=displayMetadata(root,"roll");
                        if(pitchRead==pitchRead){
                            pitch=pitchRead;
                        }
                        if(rollRead==rollRead){
                            roll=rollRead;
                        }
                    }
                }
                iis.close();
            } catch (IllegalArgumentException ex) {
                errors++;
                System.out.println(filename);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if(pitch==pitch&&roll==roll){
            //camAnglesAvailable=true;
        }else{
            pitch=0;
            roll=0;
        }
        return new double[]{pitch,roll};
    }
    
    
    private static double displayMetadata(Node root, String parameterName) {
        return displayMetadata(root, 0, parameterName);
    }

    private static double displayMetadata(Node node, int level, String parameterName) {
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
        	String value="";
        	boolean valueFound=false;
        	boolean nameFound=false;
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                if(attr.getNodeName().equals("keyword")&&attr.getNodeValue().equals(parameterName)){
                	nameFound=true;
                }
                if(attr.getNodeName().equals("value")){
                	valueFound=true;
                	value=attr.getNodeValue();
                }
            }
            if(valueFound&&nameFound){
            	return Double.parseDouble(value);
            }
        }

        Node child = node.getFirstChild();
        if (child == null) {
            return Double.NaN;
        }
        while (child != null) {
            double result=displayMetadata(child, level + 1,parameterName);
            if(result==result){
            	return result;
            }
            child = child.getNextSibling();
        }
        return Double.NaN;
    }
}
