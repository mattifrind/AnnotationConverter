package annotationconverter;

public class Triangulation{
	private static final int CAM_WIDTH=640;
	private static final int CAM_HEIGHT=480;
	private static final double CAM_FOV=1.03;
	private static final double f=CAM_WIDTH/2./Math.tan(CAM_FOV/2);
	private static final double BOT_HEIGHT=0.4694;//Optimizer.getParam("botHeight",0.35,0.55);
        //ball_Size = 70.0
	private static final double BALL_SIZE=320.0;//Optimizer.getParam("ballSize",22,38);//ballradius in px, wenn 1 Meter entfernt
	private static final double SIZE_OFFSET=0;//Optimizer.getParam("ballSizeOffset",-3,3);//ballradiusoffset in px, wenn 1 Meter entfernt
	
	private static double rollRad = 0.0811610147356987;
	private static double pitchRad = 0.4268553853034973;

	public static double getRobotHeight(int x, int y, double[] metadata){
            rollRad = metadata[1];
            pitchRad = metadata[0];
            Coord rel=projectPoint(x,y);
            double dist=Math.sqrt(rel.x*rel.x+rel.y*rel.y);
            if(dist==0)return 10;
            double distCam=Math.sqrt(dist*dist+BOT_HEIGHT*BOT_HEIGHT);
            return Math.max(7,BALL_SIZE/distCam+SIZE_OFFSET);//ballradius in px, wenn 1 Meter entfernt
	}
	
	public static Coord projectPoint(double x, double y){
            Coord p=new Coord(x,y);
            double sRoll=Math.sin(-rollRad); //kein -
            double cRoll=Math.cos(-rollRad); //kein -
            double sPitch=Math.sin(-pitchRad);
            double cPitch=Math.cos(-pitchRad);
            p=roll(p,sRoll,cRoll);
            p=pitch(p,sPitch, cPitch);
            return p;
	}
	
	private static Coord roll(Coord p, double sRoll, double cRoll) {
		return new Coord((p.x-CAM_WIDTH/2)*cRoll-(p.y-CAM_HEIGHT/2)*sRoll+CAM_WIDTH/2,(p.x-CAM_WIDTH/2)*sRoll+(p.y-CAM_HEIGHT/2)*cRoll+CAM_HEIGHT/2);
	}

	private static Coord pitch(Coord p, double sPitch,double cPitch) {
		Coord pr=new Coord(p.x-CAM_WIDTH/2,f,-(p.y-CAM_HEIGHT/2));
		Coord pRot=new Coord(pr.x,cPitch*pr.y-sPitch*pr.z,sPitch*pr.y+cPitch*pr.z);
		if(pRot.z>=0){
			return new Coord(0,0,0);
		}
		double fac=BOT_HEIGHT/pRot.z;
		return new Coord(pRot.x*fac,pRot.y*fac,0);
	}
}

class Coord {
    double x, y, z;
    public Coord(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Coord(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}