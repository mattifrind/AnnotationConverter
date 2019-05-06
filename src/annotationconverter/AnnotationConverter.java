package annotationconverter;

import org.json.JSONObject;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONTokener;

public class AnnotationConverter {
    
    /**
     * Anteil der Trainingsbeispiele, die in das Validierungsset verschoben
     * werden sollen.
     *
     */
    public static final double VALIDATION_SET_SHARE = 0.25;
    public static final int ANNOTATION_VERSION = 3;
    public static final boolean VISUALIZE = true;
    public static final int COUNT_VIS = 100;
    
    public static final int CATID1 = 37; //std: 37
    public static final int CATID2 = 77; //std: 37
    public static final int MIN_BOUND_IMAGE_IDS = 581930;
    public static final String IMAGE_DIRECTORY = "../large_robot/large_robot/";
    public static final String VIS_DIRECTORY = "vis_output/";
    
    public static int lsCount = 0;
    
    public static int maxImageID = 0;

    private static int findMaxId(JSONObject aRoot) {
        JSONArray tempImages = aRoot.getJSONArray("images");
        Stream<Object> tempJSONStream = tempImages.toList().stream();
        return tempJSONStream.mapToInt(s -> {
            return ((int)(Double.parseDouble("" + ((HashMap)s).get("id"))));
        }).max().getAsInt();
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String annotationsFile = "large_robot00133682.csv";
        String jsonOutputTrainFile = "large_robot_train2.json";
        String jsonOutputValFile = "large_robot_val2.json";
        
        System.out.println("Read COCO Dataset");
        JSONObject cocoTrain = readJSONFile("instances_train2014-2.json");
        maxImageID = findMaxId(cocoTrain);
        System.out.println("Train: MaxImageID: " + maxImageID);
        JSONObject cocoVal = readJSONFile("instances_val2014-2.json");
        int tempMax = findMaxId(cocoVal);
        System.out.println("Val: MaxImageID: " + tempMax);
        if (tempMax > maxImageID) maxImageID = tempMax;
        System.out.println(maxImageID);
        //ArrayList<Integer> imageIDs = readImageIDs(cocoVal);
        //System.out.println("COUNT_VIS image ids: " + imageIDs.size());
        //System.out.println("Low bound: " + Collections.min(imageIDs));
        //System.out.println("High bound: " + Collections.max(imageIDs));
        System.out.println("Read annotation csv");
        ArrayList<String> files = new ArrayList<>(3152);
        ArrayList<Annotation> annotationData = new ArrayList<>(7722);
        readAnnotationFile(annotationsFile, files, annotationData);
        if (VISUALIZE) {
            visualizeAnnotations(files, annotationData, maxImageID);
        }
        System.out.println("number of images: " + files.size());
        System.out.println("number of annotations: " + annotationData.size());
        Pair<JSONObject, List<JSONObject>> obj = generateJSON(files, annotationData);
        Pair<JSONObject, JSONObject> ls = splitJSON(obj);
        fetchImages(ls.getKey());
        fetchImages(ls.getValue());
        JSONObject train = mergeJSON(ls.getKey(), cocoTrain);
        JSONObject val = mergeJSON(ls.getValue(), cocoVal);
        int consistencytrain = checkConsistency(train);
        int consistencyval = checkConsistency(val);
        System.out.println("Train-ID-Duplicates: " + consistencytrain);
        System.out.println("Val-ID-Duplicates: " + consistencyval);
        System.out.println("#############");
        System.out.println("train");
        printStats(train);
        System.out.println("#############");
        System.out.println("val");
        printStats(val);
        System.out.println("Saving...");
        saveJSON(ls.getKey(), jsonOutputTrainFile); //train 
        saveJSON(ls.getValue(), jsonOutputValFile); //val
        System.out.println("Saved.");
    }

    private static void visualizeAnnotations(ArrayList<String> files, ArrayList<Annotation> annotationData, int tempMax) {
        System.out.println("Visualize BoundingBoxes");
            for (File file: new File(VIS_DIRECTORY).listFiles()) if (!file.isDirectory()) file.delete();
            Random rdm = new Random();
            ArrayList<Integer> visualized = new ArrayList<>();
            while (visualized.size() < COUNT_VIS) {
                if (visualized.size() % 10 == 0) {
                    System.out.println("Visualized-Images: " + visualized.size());
                }
                int index = rdm.nextInt(files.size());
                if (!visualized.contains(index)) {
                    visualized.add(index);
                    ArrayList<BoundingBox> bboxes = new ArrayList<>();
                    for (int j = 0; j < annotationData.size(); j++) {
                        if (annotationData.get(j).fileID == index + maxImageID + 1) {
                            if (annotationData.get(j).bbox.size != null) {
                                bboxes.add(new BoundingBox(annotationData.get(j).bbox.bbox, annotationData.get(j).bboxType, annotationData.get(j).bbox.size, annotationData.get(j).bbox.x1, annotationData.get(j).bbox.y1, annotationData.get(j).bbox.x1, annotationData.get(j).bbox.y2));
                            } else {
                                bboxes.add(new BoundingBox(annotationData.get(j).bbox.bbox, annotationData.get(j).bboxType));
                            }
                        }
                    }
                    Visualize.visualizeBBox(files.get(index), bboxes);
                }
            }
            System.out.println("Visualizing done.");
    }
    
    //files - Liste aller Dateien
    //annotationData - Liste aller Annotationen
    protected static Pair<JSONObject, List<JSONObject>> generateJSON(List<String> files, List<Annotation> annotationData) {
        JSONObject obj = new JSONObject();

        JSONObject info = new JSONObject();
        info.put("description", "Roboterdatenset");
        info.put("url", "...");
        info.put("version", "1.0");
        info.put("year", "2018");
        info.put("contributor", "HTWK Leipzig");
        info.put("date_created", "2018-02-07");
        obj.put("info", info);

        obj.put("type", "instances");

        JSONObject licenses = new JSONObject();
        licenses.put("url", "...");
        licenses.put("id", 1);
        licenses.put("name", "...");
        obj.put("licenses", licenses);

        JSONArray images = new JSONArray(IntStream.range(0, files.size())
                .parallel()
                .mapToObj(i -> newImage(files.get(i), 640, 480, i))
                .collect(Collectors.toList()));
        obj.put("images", images);

        List<JSONObject> annotations = IntStream.range(0, annotationData.size())
                .parallel()
                .mapToObj(i -> newAnnotation(i, annotationData.get(i)))
                .collect(Collectors.toList());

        JSONArray categories = new JSONArray();
        categories.put(newCategory("ball", CATID1)); //37
        categories.put(newCategory("foot", CATID2)); //77
        obj.put("categories", categories);
        return new Pair<>(obj, annotations);
    }

    private static void readAnnotationFile(String annotationsFile, List<String> files, List<Annotation> annotationData) {
        int lines = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(annotationsFile));
            while (in.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Lines: " + lines);
        try {
            BufferedReader in = new BufferedReader(new FileReader(annotationsFile));
            String zeile;
            int counter = 0;
            while ((zeile = in.readLine()) != null) {
                counter++;
                if (counter % 2000 == 0) {
                    System.out.println("Reading CSV: " + Math.round(((double) counter/(double) lines)*100) + "%");
                }
                //System.out.println(zeile);
                String[] split = zeile.split(";");
                String filename = split[0];
                int type = -1;

                switch(split[3].toLowerCase()) {
                    case "ball": type = CATID1; break;
                    case "robot": case "foot": case "feet": type = CATID2; break;
                }
                if (type == CATID1 || type == CATID2) {
                    BoundingBox[] bbox = Parser.readBBox(split, ReadMetadata.readCamAngles(filename));
                    int correspondingIndex = -1;
                    if (!files.contains(filename)) {
                        correspondingIndex = files.size() + maxImageID + 1;
                        files.add(filename);
                    } else {
                        //if more than one label for one file:
                        correspondingIndex = files.indexOf(filename) + maxImageID + 1;
                    }
                    switch (bbox.length) {
                        case 1:
                            writeAnnotation(bbox[0], correspondingIndex, type, annotationData);
                            break;
                        case 2:
                            writeAnnotation(bbox[0], correspondingIndex, type, annotationData);
                            writeAnnotation(bbox[1], correspondingIndex, type, annotationData);
                            break;
                        default:
                            //throw new Error("Wrong Bounding Box");
                            //Debug option
                            for (int i = 0; i < bbox.length; i++) {
                                writeAnnotation(bbox[i], correspondingIndex, type, annotationData);
                            }
                    }
                }
            }
            System.out.println("Errors: " + ReadMetadata.errors);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void writeAnnotation(BoundingBox bbox, int correspondingIndex, int type, List<Annotation> annotationData) {
        Annotation annotation = new Annotation();
        annotation.bbox = bbox;
        annotation.fileID = correspondingIndex;
        annotation.category = type;
        annotation.bboxType = bbox.type;
        annotationData.add(annotation);
    }
    
    private static Object newCategory(String name, int id) {
        JSONObject category = new JSONObject();
        category.put("id", id);
        category.put("name", name);
        category.put("supercategory", "object");
        return category;
    }

    protected static JSONObject newAnnotation(int id, Annotation ann) {
        JSONObject annotation = new JSONObject();
        annotation.put("id", id);
        annotation.put("image_id", ann.fileID);
        
        annotation.put("category_id", ann.category);
        
        JSONArray bbox = new JSONArray(new int[]{ann.bbox.bbox.x, ann.bbox.bbox.y, ann.bbox.bbox.width, ann.bbox.bbox.height});
        annotation.put("bbox", bbox);
        annotation.put("segmentation", new JSONArray());
        annotation.put("area", 10.0);
        annotation.put("iscrowd", 0);
        return annotation;
    }

    private static JSONObject newImage(String filename, int width, int height, int id) {
        JSONObject image = new JSONObject();
        image.put("license", 1);
        image.put("url", "...");
        image.put("file_name", convertFilename(filename));
        image.put("width", width);
        image.put("height", height);
        image.put("date_captured", "...");
        image.put("id", id);
        return image;
    }
    
    private static String convertFilename(String filename) {
        return filename.replace(".png", ".jpg");
    }

    private static void saveJSON(JSONObject obj, String filename) throws IOException {
        Files.write(new File(filename).toPath(), obj.toString().getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private static JSONArray cloneJSONArray(JSONArray original) {
        return new JSONArray(original.toList());
    }
    
    private static Pair<JSONObject, JSONObject> splitJSON(Pair<JSONObject, List<JSONObject>> obj) {
        String[] keys = obj.getKey().keySet().stream().toArray(String[]::new);
        System.out.println(Arrays.toString(keys));
        JSONObject objtrain = new JSONObject(obj.getKey(), keys), objval = new JSONObject(obj.getKey(), keys);
        objval.put("images", cloneJSONArray(objtrain.getJSONArray("images")));
        objval.put("categories", cloneJSONArray(objtrain.getJSONArray("categories")));
        
        List<JSONObject> annotations = obj.getValue();
        Collections.shuffle(annotations);
        int valIndex = (int) (annotations.size() * VALIDATION_SET_SHARE);
        JSONArray annotationsVal = new JSONArray(annotations.subList(0, valIndex)),
                annotationsTrain = new JSONArray(annotations.subList(valIndex, annotations.size()));
        System.out.println(annotationsTrain.length());
        System.out.println(annotationsVal.length());
        objtrain.put("annotations", annotationsTrain);
        objval.put("annotations", annotationsVal);
        return new Pair<>(objtrain, objval);
    }
    
    private static JSONObject readJSONFile(String file) throws FileNotFoundException {
        return new JSONObject(new JSONTokener(new FileReader(file)));
    }
    
    private static JSONObject mergeJSON(JSONObject obj1, JSONObject obj2) {
        JSONArray obj2a = obj1.getJSONArray("annotations");
        obj2.getJSONArray("annotations").toList().forEach(o -> obj2a.put(o));
        JSONArray obj2i = obj1.getJSONArray("images");
        obj2.getJSONArray("images").toList().forEach(o -> obj2i.put(o));
        
        JSONArray obj2c = obj1.getJSONArray("categories");
        obj2.getJSONArray("categories").toList().forEach(o -> obj2c.put(o));
        return obj1;
    }

    protected static int checkConsistency(JSONObject ls) {
        JSONArray images = (JSONArray) ls.get("images");
        lsCount = images.length() - (int) images.toList().stream().mapToInt(obj -> (int) Double.parseDouble(((HashMap) obj).get("id").toString())).distinct().count();
        JSONArray ann = (JSONArray) ls.get("annotations");
        lsCount += ann.length() - (int) ann.toList().stream().mapToInt(obj -> (int) Double.parseDouble(((HashMap) obj).get("id").toString())).distinct().count();
        return lsCount;
    }
    
    @Deprecated
    public static void addMissingCategories(JSONObject obj, JSONObject jsonCategories) {
        JSONArray categories = obj.getJSONArray("categories");
        jsonCategories.getJSONArray("categories").forEach(e -> {
            System.out.println(e);
            if (categories.toList().contains(e)) {
                System.out.println("ATTENTION");
            } else {
                categories.put(e);
            }
        });
    }
    
    private static String objStr = "";
    
    public static void searchForString(JSONObject obj, String str) {
        System.out.println(objStr.equals(objStr = obj.toString()));
        System.out.println(objStr.substring(0, 100) + "..." + objStr.length());
        System.out.println("found: " + str + " " + objStr.contains(str));
    }
    
    /**
     * Fetches the images according to the annotations. needed because of the splitting in train and val
     * @param obj
     */
    public static void fetchImages(JSONObject obj) {
        JSONArray annotations = obj.getJSONArray("annotations"),
                images = obj.getJSONArray("images");
        
        List<Integer> indices = IntStream.range(0, annotations.length())
                .map(i -> annotations.getJSONObject(i).getInt("image_id")).mapToObj(Integer::valueOf).collect(Collectors.toList());
        List<JSONObject> objectsToKeep = IntStream.range(0, images.length())
                .mapToObj(images::getJSONObject)
                .filter(jobj -> indices.contains(jobj.getInt("id")))
                .collect(Collectors.toList());
        JSONArray newImages = new JSONArray(objectsToKeep);
        obj.put("images", newImages);
    }
    
    public static ArrayList<Integer> readImageIDs(JSONObject jo) {
        ArrayList<Integer> ids = new ArrayList<>();
        JSONArray images = jo.getJSONArray("images");
        images.forEach(e -> {
            ids.add(((JSONObject) e).getInt("id"));
        });
        return ids;
    }
    
    public static void printStats(JSONObject obj) {
        System.out.println("Images: " + obj.getJSONArray("images").length());
        System.out.println("Annotations: " + obj.getJSONArray("annotations").length());
        System.out.println("Categories: " + obj.getJSONArray("categories").length());
    }
}

class Annotation {

    public BoundingBox bbox;
    public int category, fileID;
    public BoundingBoxType bboxType;

}

enum BoundingBoxType {
    BALL,
    HORIZONTAL,
    LIGHT_DIAGONAL,
    DIAGONAL,
    SMALL
}

class BoundingBox {
    public Rectangle bbox;
    public BoundingBoxType type;
    //x1, x2 usw stellen die asu den Annotationen gelesenen Punkte dar, die gezeichnet werden. Das Rectangle die berechnete BoundingBox
    public Integer size, x1, y1, x2, y2;
    
    public BoundingBox(Rectangle bbox, BoundingBoxType type) {
        this.bbox = bbox;
        this.type = type;
    }
    
    public BoundingBox(Rectangle bbox, BoundingBoxType type, int size, int x1, int y1, int x2, int y2) {
        this.bbox = bbox;
        this.type = type;
        this.size = size;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public BoundingBox() {
        
    }
}
