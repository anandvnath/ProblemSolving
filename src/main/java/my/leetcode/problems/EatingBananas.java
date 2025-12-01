class EatingBananas {
    public static void main(String[] args) {
        EatingBananas eatingBananas = new EatingBananas();
        int[] piles = {1,1,1,999999999};
        int h = 10;
        int result = eatingBananas.minEatingSpeed(piles, h);
        System.out.println("Minimum eating speed: " + result); // Expected output: 25
    }

    public int minEatingSpeed(int[] piles, int h) {
        int left = 1;
        int right = findMax(piles);
        System.out.println("Max pile size: " + right);
        int minRate = 0;

        while (left <= right) {
            int rate = left + (right - left) / 2;

            int timeTaken = calculateTime(piles, rate);
            System.out.println("Time taken at rate " + rate + ": " + timeTaken);

            if (h < timeTaken) {
                System.out.println("Have less time, try faster rate");
                left = rate + 1;
            } else if (h >= timeTaken) {
                System.out.println("Have more time, try slower rate");
                // since we were able to finish in time, record rate
                minRate = rate;
                right = rate - 1;
            }
        }

        return minRate;
    }

    private int calculateTime(int[] piles, int rate) {
        int time = 0;
        for (int sz : piles) {
            time += Math.ceil((double) sz / rate);
        }
        return time;
    }

    private int findMax(int[] piles) {
        int max = piles[0];

        for (int i = 1; i < piles.length; i++) {
            max = Math.max(max, piles[i]);
        }

        return max;
    }
}
