package org.attentionmeter.facedetect;

import org.attentionmeter.enumerator.*;
import org.opencv.core.*;

public class Face {
	
	int age = 0;
	int width;
	int height;
	int xpt;
	int ypt;
	int life;
	
	FaceMovementEnum state;
	FaceMovementEnum lastState;
	int alternations;
	int faceStill;
	
	int others;
	int stills;
    int lefts;
    int rights;
    int ups;
    int downs;
    
    FaceMovementEnum turnDir;
    
    int eyeTopline;
    int eyeBotline;    
    Point eyeLeft1;
    Point eyeLeft2;
    Point eyeRight1;
    Point eyeRight2;
    
    int mouthTopline;
    int mouthBotline;
    Point mouthTopLeft;
    Point mouthBotRight;
    
    //### Face detection constants
    //#Face movement constants
    int CAPTURING = 1; //set 1 to enable saving to JPGs
    int FACE_MIN_SIZE = 70; //the bigger, the more fps
    public static final int FACE_MAX_MOVEMENT = 40;
    int FACE_MAX_LIFE = 1;
    int FACE_LR_MOVE_THRESH = 2;
    int FACE_UD_MOVE_THRESH = 1;
    int FACE_LR_STATE_CHANGE_THRESH = 1;
    int FACE_UD_STATE_CHANGE_THRESH = 1;
    int FACE_ALTERNATION_THRESH = 2;
    int FACE_ONE_DIMENSION_THRESH = 2;
    int FACE_STILL_THRESHOLD = 3;
    int FACE_ALTERNATIONS_EXPIRE = 6;
	
	Face(int age, int width,int height,int xpt, int ypt, int life){
		
		this.age = age;
		this.width = width;
		this.height = height;
		this.xpt = xpt;
		this.ypt = ypt;
		this.life = life;
		
		updateEyes();
        updateMouth();
        
        this.state = FaceMovementEnum.OTHER;
        this.lastState = this.state;
        this.alternations = 0;
        this.faceStill = 0;
        
        this.stills = 0;
        this.lefts = 0;
        this.rights = 0;
        this.ups = 0;
        this.downs = 0;
		
	}
	
	void updateFace(int width, int height, int xpt, int ypt){
		
        this.turnDir = getTurnDir(this.xpt, xpt, this.ypt, ypt, this.width, width, this.height, height);
        updateMoveState(turnDir);
        //print turnDir
        
        this.age = this.age + 1;
        this.width = width;
        this.height = height;
        this.xpt = xpt;
        this.ypt = ypt;
        this.life = 0;
        updateEyes();
        updateMouth();
        
	}
        
    void updateEyes(){
    	
        this.eyeTopline = this.ypt + ((this.height*1)/3);
        this.eyeBotline = this.ypt + ((this.height*1)/2);
        
        this.eyeLeft1 = new Point(this.xpt + (this.width/5), this.eyeTopline);
        this.eyeLeft2 = new Point(this.xpt + ((this.width*3)/8), this.eyeBotline);
        this.eyeRight1 = new Point(this.xpt + ((this.width*5)/8),this.eyeTopline);
        this.eyeRight2 = new Point(this.xpt + ((this.width*4)/5),this.eyeBotline);
        
    }
    
    void updateMouth(){
    	
        this.mouthTopline = this.ypt + ((this.height*2)/3);
        this.mouthBotline = this.ypt + this.height;

        this.mouthTopLeft = new Point(this.xpt + this.width/5, this.mouthTopline);
        this.mouthBotRight = new Point(this.xpt + (this.width*4)/5, this.mouthBotline);
        
    }
    
    boolean isShaking(){
    	
        if (this.alternations < FACE_ALTERNATION_THRESH){
            return false;
        }
        else{
            if ((this.state == FaceMovementEnum.LEFT) || (this.state == FaceMovementEnum.RIGHT)){
                return true;
            }
            else{
                return false;
            }
        }
        
    }
    
    boolean isNodding(){
    	
        if (this.alternations < FACE_ALTERNATION_THRESH){
            return false;
        }
        else{
            if ((this.state == FaceMovementEnum.UP) || (this.state == FaceMovementEnum.DOWN)){
                return true;
            }
            else{
                return false;
            }
        }
        
     }
    
    boolean isStill(){
        return (this.faceStill < FACE_STILL_THRESHOLD);
    }
    
    void updateMoveState(FaceMovementEnum turnDir){
        if (turnDir == FaceMovementEnum.OTHER){
            this.faceStill += 1;
            this.state = FaceMovementEnum.OTHER;
        }
        else if (turnDir == FaceMovementEnum.STILL){
            if (this.state != FaceMovementEnum.STILL){
                this.lastState = this.state;
            }
            else{
                this.faceStill = 0;
            }
            this.state = FaceMovementEnum.STILL;
            this.stills += 1;
            if (this.stills > FACE_ALTERNATIONS_EXPIRE){
                this.alternations = 0;
                this.stills = 0;
            }
        }
        else if (turnDir == FaceMovementEnum.RIGHT){
            this.faceStill += 1;
            if (this.state == FaceMovementEnum.OTHER){
                this.rights += 1;
                if (this.rights > FACE_LR_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.RIGHT;
                }
            }
            else if (this.state == FaceMovementEnum.RIGHT){
                this.rights += 1;
            }
            else if (this.state == FaceMovementEnum.LEFT){
                this.rights += 1;
                if (this.rights > FACE_LR_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.RIGHT;
                    this.resetNonAltCounts();
                    this.alternations += 1;
                }
            }
            else if ((this.state == FaceMovementEnum.UP) || (this.state == FaceMovementEnum.DOWN)){
                this.state = FaceMovementEnum.OTHER;
                this.resetCounts();
            }
            else if(this.state == FaceMovementEnum.STILL){
                if (this.lastState == FaceMovementEnum.LEFT){
                    this.alternations += 1;
                }
                this.state = FaceMovementEnum.RIGHT;
            }
        }
        else if (turnDir == FaceMovementEnum.LEFT){
            this.faceStill += 1;
            if (this.state == FaceMovementEnum.OTHER){
                this.lefts += 1;
                if (this.lefts > FACE_LR_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.LEFT;
                }
            }
            else if (this.state == FaceMovementEnum.RIGHT){
                this.lefts += 1;
                if(this.lefts > FACE_LR_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.LEFT;
                    this.resetNonAltCounts();
                    this.alternations += 1;
                }
            }
            else if (this.state == FaceMovementEnum.LEFT){
                this.lefts += 1;
            }
            else if ((this.state == FaceMovementEnum.UP) || (this.state == FaceMovementEnum.DOWN)){
                this.state = FaceMovementEnum.OTHER;
                this.resetCounts();
            }
            else if (this.state == FaceMovementEnum.STILL){
                if (this.lastState == FaceMovementEnum.RIGHT){
                    this.alternations += 1;
                }
                this.state = FaceMovementEnum.LEFT;
            }
        }
        else if (turnDir == FaceMovementEnum.UP){
            this.faceStill += 1;
            if (this.state == FaceMovementEnum.OTHER){
                this.ups += 1;
                if (this.ups > FACE_UD_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.UP;
                }
            }
            else if (this.state == FaceMovementEnum.DOWN){
                this.ups += 1;
                if (this.ups > FACE_UD_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.UP;
                    this.resetNonAltCounts();
                    this.alternations += 1;
                }
            }
            else if (this.state == FaceMovementEnum.UP){
                this.ups += 1;
            }
            else if ((this.state == FaceMovementEnum.LEFT) || (this.state == FaceMovementEnum.RIGHT)){
                this.state = FaceMovementEnum.OTHER;
                this.resetCounts();
            }
            else if (this.state == FaceMovementEnum.STILL){
                if (this.lastState == FaceMovementEnum.DOWN){
                    this.alternations += 1;
                }
                this.state = FaceMovementEnum.UP;
            }
        }
        else if (turnDir == FaceMovementEnum.DOWN){
            this.faceStill += 1;
            if (this.state == FaceMovementEnum.OTHER){
                this.downs += 1;
                if (this.downs > FACE_UD_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.DOWN;
                }
            }
            else if (this.state == FaceMovementEnum.UP){
                this.downs += 1;
                if (this.downs > FACE_UD_STATE_CHANGE_THRESH){
                    this.state = FaceMovementEnum.DOWN;
                    this.resetNonAltCounts();
                    this.alternations += 1;
                }
            }
            else if (this.state == FaceMovementEnum.DOWN){
                this.downs += 1;
            }
            else if ((this.state == FaceMovementEnum.LEFT) || (this.state == FaceMovementEnum.RIGHT)){
                this.state = FaceMovementEnum.OTHER;
                this.resetCounts();
            }
            else if (this.state == FaceMovementEnum.STILL){
                if (this.lastState == FaceMovementEnum.UP){
                    this.alternations += 1;
                }
                this.state = FaceMovementEnum.DOWN;
            }
        }
    }
    
    void resetCounts(){
        this.others = 0;
        this.stills = 0;
        this.rights = 0;
        this.lefts = 0;
        this.ups = 0;
        this.downs = 0;
        this.alternations = 0;
    }
    
    void resetNonAltCounts(){
        this.others = 0;
        this.stills = 0;
        this.rights = 0;
        this.lefts = 0;
        this.ups = 0;
        this.downs = 0;
    }
    
    FaceMovementEnum getTurnDir(int old_xpt, int new_xpt, int old_ypt, int new_ypt, int old_width, int new_width, int old_height, int new_height){
    	
        int old_x = old_xpt + (old_width/2);
        int new_x = new_xpt + (new_width/2);
        int old_y = old_ypt + (old_height/2);
        int new_y = new_ypt + (new_height/2);

        FaceMovementEnum xdir = FaceMovementEnum.STILL;
        FaceMovementEnum ydir = FaceMovementEnum.STILL;
        
        if (new_x - old_x > FACE_LR_MOVE_THRESH){
            xdir = FaceMovementEnum.RIGHT;
        }
        
        if (new_x - old_x < -FACE_LR_MOVE_THRESH){
            xdir = FaceMovementEnum.LEFT;
        }
        
        if (new_y - old_y > FACE_UD_MOVE_THRESH){
            ydir = FaceMovementEnum.DOWN;
        }
        
        if (new_y - old_y < -FACE_UD_MOVE_THRESH){
            ydir = FaceMovementEnum.UP;
        }
        
        if (ydir == xdir){
            return FaceMovementEnum.STILL;
        }
        else{
            if ((ydir != FaceMovementEnum.STILL) && (xdir != FaceMovementEnum.STILL)){
                if ((Math.abs(new_x - old_x)) > (Math.abs(new_y - old_y)/2)){
                    return xdir;
                }
                else{
                    if (((Math.abs(new_y - old_y)) - (Math.abs(new_x - old_x))) > FACE_ONE_DIMENSION_THRESH){
                        return ydir;
                    }
                    else{
                        return FaceMovementEnum.OTHER;
                    }
                }
            }
            else{
                if (xdir == FaceMovementEnum.STILL){
                    return ydir;
                }
                else{
                    return xdir;
                }
            }
        }
    }
    
    boolean isTooOld(){
        if (this.life > FACE_MAX_LIFE){
            return true;
        }
        else{
            return false;
        }
    }
    
    int updateLife(){
        this.life = this.life+1;
        return this.life;
    }
       
	
}


