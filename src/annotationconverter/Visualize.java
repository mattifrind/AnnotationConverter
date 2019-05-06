/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package annotationconverter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author matti
 */
public class Visualize {
    
    public static void visualizeBBox(String image, ArrayList<BoundingBox> bbox) {
        BufferedImage bimage = new BufferedImage(640, 480, BufferedImage.TYPE_BYTE_INDEXED);
        try {
            //System.out.println(image);
            bimage = ImageIO.read(new File(AnnotationConverter.IMAGE_DIRECTORY + image));
        } catch (IOException ex) {
            Logger.getLogger(Visualize.class.getName()).log(Level.SEVERE, null, ex);
        }

        Graphics2D g2d = bimage.createGraphics();
        bbox.forEach(bb -> {
            switch (bb.type) {
                case BALL:
                    g2d.setColor(Color.red);
                    break;
                case DIAGONAL:
                    g2d.setColor(Color.blue);
                    break;
                case HORIZONTAL:
                    g2d.setColor(Color.green);
                    break;
                case LIGHT_DIAGONAL:
                    g2d.setColor(Color.yellow);
                    break;
                case SMALL:
                    g2d.setColor(Color.black);
                    break;
            }
            g2d.drawRect(bb.bbox.x, bb.bbox.y, bb.bbox.width, bb.bbox.height);
            if (bb.size != null) {
                g2d.setFont(new Font("Roboto Light", 0, 40));
                g2d.drawString(bb.size + "", bb.bbox.x, bb.bbox.y-20);
            }
            if (bb.x1 != null) {
                g2d.drawLine(bb.x1, bb.y1, bb.x2, bb.y2);
            }
            //g2d.drawOval(bb.bbox.x, bb.bbox.y, bb.bbox.width, bb.bbox.height);
        });
        g2d.dispose();
        
        File outputfile = new File(AnnotationConverter.VIS_DIRECTORY + image);
        try {
            ImageIO.write(bimage, "jpg", outputfile);
        } catch (IOException ex) {
            Logger.getLogger(Visualize.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
