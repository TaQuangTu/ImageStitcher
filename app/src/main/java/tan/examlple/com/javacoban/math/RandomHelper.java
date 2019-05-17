package tan.examlple.com.javacoban.math;

import java.util.Random;

public class RandomHelper {
    private static RandomHelper instance ;
    public static RandomHelper getInstance(){
        if(instance==null) instance = new RandomHelper();
        return instance;
    }
    private Random random = new Random();
    public int nextIntInRange(int start, int end){
       return start + random.nextInt(end-start);
    }
}
