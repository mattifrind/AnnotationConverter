/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package annotationconverter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author matti
 */
public class AnnotationConverterTest {
    
    int lsCount;
    
    @Test
    public void testCreateAnnotation() {
        Annotation tempAnnotation = new Annotation();
        tempAnnotation.category = 1;
        tempAnnotation.fileID = 12;
        BoundingBox tempBox = new BoundingBox(new Rectangle(12, 10, 8, 5), BoundingBoxType.BALL);
        
        tempAnnotation.bbox = tempBox;
        
        JSONObject tempResult = AnnotationConverter.newAnnotation(12, tempAnnotation);
        assertEquals("{\"area\":10,\"category_id\":1,\"bbox\":[12,10,8,5],\"iscrowd\":0,\"segmentation\":[],\"id\":12,\"image_id\":12}", tempResult.toString());
    }
    
    //@Test
    public void testDatasetJSON() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            files.add("file" + i);
        }
        
        ArrayList<Annotation> annotationData = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Annotation tmpAnn = new Annotation();
            tmpAnn.bbox.bbox = new Rectangle(0, 0, 0, 0);
            annotationData.add(tmpAnn);
        }
        
        Pair<JSONObject, List<JSONObject>> obj = AnnotationConverter.generateJSON(files, annotationData);
        
        JSONObject dataset = obj.getKey();
        System.out.println("######info");
        System.out.println(dataset.get("info"));
        System.out.println("######images");
        System.out.println(dataset.get("images"));
        System.out.println("######categories");
        System.out.println(dataset.get("categories"));
    }
    
    @Test
    public void testConsistencyCheck() {
        JSONObject ls = new JSONObject();
        JSONArray images = new JSONArray();
        for (int i = 0; i < 100; i++) {
            images.put(new JSONObject().put("id", i));
        }
        ls.put("images", images);
        ls.put("annotations", images);
        assertEquals(0, AnnotationConverter.checkConsistency(ls));
        
        ls = new JSONObject();
        images = new JSONArray();
        for (int i = 0; i < 100; i++) {
            images.put(new JSONObject().put("id", 2));
        }
        ls.put("images", images);
        ls.put("annotations", images);
        assertEquals(198, AnnotationConverter.checkConsistency(ls));
    }
}
