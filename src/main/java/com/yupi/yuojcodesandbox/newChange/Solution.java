package com.yupi.yuojcodesandbox.newChange;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/22
 * @description
 */
public class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> hashtable = new HashMap<Integer, Integer>();
        for (int i = 0; i < nums.length; ++i) {
            if (hashtable.containsKey(target - nums[i])) {
                return new int[]{hashtable.get(target - nums[i]), i};
            }
            hashtable.put(nums[i], i);
        }
        return new int[0];
    }
}
