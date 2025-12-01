import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

class Fleet {
    public static void main(String[] args) {
        Fleet fleet = new Fleet();

        int target = 10;
        int[] position = {0, 4, 2};
        int[] speed = {2, 1, 3};
        int result = fleet.carFleet(target, position, speed);
        System.out.println("Number of car fleets: " + result); // Expected output: 2
    }
    
    public int carFleet(int target, int[] position, int[] speed) {
        Map<Integer, Integer> posSpeed = new TreeMap<>(Collections.reverseOrder());
        for (int i = 0; i < position.length; i++) {
            posSpeed.put(position[i], speed[i]);
        }

        double[] times = new double[position.length];
        int i = 0;

        System.out.println("Position sorted: " + posSpeed);
        for (int pos: posSpeed.keySet()) {
            times[i++] = ((double) (target - pos) / posSpeed.get(pos));
        }

        System.out.println("Times to reach target: " + Arrays.toString(times));

        int fleets = 1;

        for (i = 0; i < times.length - 1; i++) {
            System.out.println("Comparing times: " + times[i] + " and " + times[i + 1]);
            if (times[i + 1] > times[i]) fleets++;
            times[i + 1] = Math.max(times[i + 1], times[i]);
        }

        return fleets;
    }
}
