/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package annotationconverter;

import static annotationconverter.AnnotationConverter.ANNOTATION_VERSION;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 *
 * @author matti
 */
public class Parser {
    
    private static final int MIN_LENGTH = 8;
    
    
    public static BoundingBox[] readBBox(String[] split, double[] metadata) {
        if (!split[3].equals("Ball")) {
            switch (ANNOTATION_VERSION) {
                case 1: 
                    //easiest version. rough bounding box around everything. joined foot box
                    return readBBox1(split);
                case 2:
                    //separated foots. size according to their rotation
                    return readBBox2(split);
                case 3:
                    //seperated foots. size according to the position in the frame with triangulation
                    return readBBox3(split, metadata);
                case 4:
                    //debug triangulation
                    return readBBoxDebug(split, metadata);
                default:
                    System.out.println("Wrong annotation version");
                    System.exit(-1);
                    return null;
            }
        } else {
            return readBall(split);
        }
    }
    
    /**
     * Parse BBox. Version 1 - both foots
     * @param split paramater from csv
     * @return Bounding Box
     */
    private static BoundingBox[] readBBox1(String[] split) {
        //split[4]: x
        //split[5]: y
        //split[6]: width
        //split[7]: height
        int x1 = Integer.parseInt(split[4]),
                y1 = Integer.parseInt(split[5]),
                x2 = Integer.parseInt(split[6]),
                y2 = Integer.parseInt(split[7]),
                dx = x2 - x1, //xEntfernung
                dy = y2 - y1, //yEntfernung
                d = (int) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)); //Entfernung per Satz des Phytagoras
        Rectangle rect = new Rectangle(x1 + (dx - d) / 2, y1 + (dy - d) / 2, d, d);
        /*if (!"Ball".equals(split[3]))
        printBBox(rect, split[0]);*/
        BoundingBox bbox = new BoundingBox();
        bbox.bbox = rect;
        bbox.type = BoundingBoxType.HORIZONTAL;
        return new BoundingBox[]{bbox};
    }
    
     /**
     * Parse BBox. Version 2 - seperated foots
     * @param split paramater from csv
     * @return Bounding Box
     */
    private static BoundingBox[] readBBox2(String[] split) {
        int x1 = Integer.parseInt(split[4]),
                y1 = Integer.parseInt(split[5]),
                x2 = Integer.parseInt(split[6]),
                y2 = Integer.parseInt(split[7]),
                dx = x2 - x1, //xEntfernung
                dy = y2 - y1, //yEntfernung
                d = (int) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)); //Entfernung per Satz des Phytagoras
        Rectangle rect1 = new Rectangle(), rect2 = new Rectangle();
        BoundingBoxType type = BoundingBoxType.HORIZONTAL;
        if ((double) Math.abs(dy) < Math.abs((double) (0.3*((double) d)))) {
            int length = (int) ((6.0/8.0)*d);
            if (length < MIN_LENGTH) {
                rect1 = minBox(x1, y1);
                rect2 = minBox(x2, y2);
                type = BoundingBoxType.DIAGONAL;
            } else {
                rect1 = new Rectangle((int) (x1 - 0.5*length), (int) (y1 - 0.5*length), length, length);
                rect2 = new Rectangle((int) (x2 - 0.5*length), (int) (y2 - 0.5*length), length, length);
                type = BoundingBoxType.HORIZONTAL;
            }
        } else if (Math.abs(0.3*d) < Math.abs(dy) && Math.abs(dy) < Math.abs(0.5*d)) {
            if (Math.abs(dx) < MIN_LENGTH || d < MIN_LENGTH) {
                rect1 = minBox(x1, y1);
                rect2 = minBox(x2, y2);
                type = BoundingBoxType.DIAGONAL;
            } else {
                rect1 = new Rectangle((int) (x1 - Math.abs(0.5*dx)), (int)(y1 - Math.abs(0.5*d)), Math.abs(dx), d);
                rect2 = new Rectangle((int) (x2 - Math.abs(0.5*dx)), (int) (y2 - Math.abs(0.5*d)), Math.abs(dx), d);
                type = BoundingBoxType.LIGHT_DIAGONAL;
            }
        } else if (Math.abs(dy) > Math.abs(0.5*d)) {
            int length = (int) Math.abs(d);
            if (length < MIN_LENGTH) {
                rect1 = minBox(x1, y1);
                rect2 = minBox(x2, y2);
                type = BoundingBoxType.DIAGONAL;
            } else {
                rect1 = new Rectangle((int) (x1 - 0.5*length), (int) (y1 - 0.5*length), length, length);
                rect2 = new Rectangle((int) (x2 - 0.5*length), (int) (y2 - 0.5*length), length, length);
                type = BoundingBoxType.HORIZONTAL;
            }
        }
        //printBBox(rect1, split[0]);
        //printBBox(rect2, split[0]);
        BoundingBox bbox1 = new BoundingBox();
        bbox1.bbox = rect1;
        bbox1.type = type;
        
        BoundingBox bbox2 = new BoundingBox();
        bbox2.bbox = rect2;
        bbox2.type = type;
        return new BoundingBox[]{bbox1, bbox2};
    }
    
    public static Rectangle minBox(int x, int y) {
        return new Rectangle((int) (x-(((double) MIN_LENGTH)/2.0)), (int) (y-(((double) MIN_LENGTH)/2.0)), MIN_LENGTH, MIN_LENGTH);
    }
    
    /**
     * Parse BBox. Version 3 - complete Robot
     * @param split paramater from csv
     * @param metadata
     * @return Bounding Box
     */
    public static BoundingBox[] readBBox3(String[] split, double[] metadata) {
        int x1 = Integer.parseInt(split[4]),
                y1 = Integer.parseInt(split[5]),
                x2 = Integer.parseInt(split[6]),
                y2 = Integer.parseInt(split[7]);
        
        int xpos = (x1 + x2)/2;
        int ypos = (y1 + y2)/2;
        int size = (int) Triangulation.getRobotHeight(xpos, ypos, metadata);
        Rectangle rect = new Rectangle(xpos-size/4, ypos-size, size/2, (int) 1.7 * size);
        
        BoundingBox bbox1 = new BoundingBox();
        bbox1.bbox = rect;
        bbox1.type = BoundingBoxType.HORIZONTAL;
        bbox1.size = size;
        bbox1.x1 = x1;
        bbox1.y1 = y1;
        bbox1.x2 = x2;
        bbox1.y2 = y2;
        return new BoundingBox[]{bbox1};
    }
    
    public static BoundingBox[] readBBoxDebug(String[] split, double[] metadata) {
        int x1 = Integer.parseInt(split[4]),
                y1 = Integer.parseInt(split[5]),
                x2 = Integer.parseInt(split[6]),
                y2 = Integer.parseInt(split[7]);
       
        ArrayList<BoundingBox> bboxes = new ArrayList<>();
        for (int i = 0; i < 600; i+=200) {
            for (int j = 0; j < 600; j+=200) {
                x1 = i;
                y1 = j;
                x2 = i+40;
                y2 = j+40;
                int xpos = i;
                int ypos = j;
                int size = (int) Triangulation.getRobotHeight(xpos, ypos, metadata);
                Rectangle rect = new Rectangle(x1-size/4, y1-size, size/2, size);
                BoundingBox bbox1 = new BoundingBox();
                bbox1.bbox = rect;
                bbox1.type = BoundingBoxType.HORIZONTAL;
                bbox1.size = size;
                bbox1.x1 = x1;
                bbox1.y1 = y1;
                bbox1.x2 = x2;
                bbox1.y2 = y2;
                bboxes.add(bbox1);
            }
        }
        BoundingBox[] bbarray = new BoundingBox[bboxes.size()];
        for (int i = 0; i < bboxes.size(); i++) {
            bbarray[i] = bboxes.get(i);
        }
        return bbarray;
    }
    
      
    public static BoundingBox[] readBall(String[] split) {
        return readBBox1(split);
    }
    
    public static void printBBox(Rectangle rect, String image) {
        System.out.println("Image: " + image);
        System.out.println("X: " + rect.getX());
        System.out.println("Y: " + rect.getY());
        System.out.println("Width: " + rect.getWidth());
        System.out.println("Height: " + rect.getHeight());
    }
}