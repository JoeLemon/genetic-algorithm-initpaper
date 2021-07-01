package com.joelemon.paperGenerator;

import com.sun.istack.internal.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库数量信息
 *
 * @author HJY
 * @date 2020/2/16
 */
public class QuesDB {

    /**
     * [{tyep:10001, lessoncategory:3214324, quantity:23}, ...]
     */
    private List<QuesDBItem> data;

    /**
     * ["type10001", "type10002", ...]
     */
    private Set<String> conditions;

    // 单一限制条件对应的下标集合
    private Map<String, List<Integer>> conditionIndexes;

    // 试题数据量
    private int[] numArr;

    // 单个约束条件的bitmap 方便统计试卷单个约束条件试题总量
    private Map<String, Bitmap> conditionBitmap;

    // data排序首要字段
    private String priorityCondition;

    private Random random = new Random();

    public QuesDB(List<QuesDBItem> dataList, Set<String> conditions, String priorityCondition) {
        this.initCheck(dataList, priorityCondition);
        this.priorityCondition = priorityCondition;
        this.setData(dataList);
        this.initConditionBitmap(conditions);
    }

    public int getQuantityArray(int index) {
        return this.numArr[index];
    }

    public QuesDBItem getQuesDBItem(int index) {
        return this.data.get(index);
    }

    /**
     * 返回指定priority condition code 下随机一元素下标
     * @param priorityCode
     * @return
     */
    public int getRandomIndexByPriorityCode (long priorityCode) {
        String bitmapKey = this.priorityCondition + priorityCode;
        Bitmap bitmap = this.conditionBitmap.get(bitmapKey);
        int randomIndex = random.nextInt(bitmap.getTotal()) + 1;
        int index = 0;
        while (randomIndex > 0) {
            if (bitmap.isValid(index)) {
                randomIndex--;
            }
            index++;
        }
        return --index;
    }


    /**
     * 根据条件返回下标数组
     *
     * @param conditionName
     * @return
     */
    public List<Integer> getConditionIndexes(String conditionName) {
        return this.conditionIndexes.get(conditionName);
    }

    /**
     * 根据传递单个约束条件，生成指定数量的初始化种群
     *
     * @param conditionName
     * @param quantity
     * @param num
     * @return
     */
    public int[] getInitPriorityConditionArray(String conditionName, int quantity, int num) {
        return new int[0];
    }

    /**
     * 通过一个特定的条件，统计抽题解中该项的数量
     * 例： 获取所有单选题的数量；
     *
     * @param condition
     * @param dataIndexArray
     * @return
     */
    public int getPaperConstraintSum(String condition, int[] dataIndexArray) {
        int result = 0;
        if (this.conditionBitmap.containsKey(condition)) {
            Bitmap bitmap = this.conditionBitmap.get(condition);
            for (int i = 0; i < dataIndexArray.length; i++) {
                result +=  bitmap.get(dataIndexArray[i]);
            }
        }
        return result;
    }

    public List<QuesDBItem> getData() {
        return data;
    }

    /**
     * 根据一组特定的限制条件返回相应试题数量
     *
     * @param quesDBItem
     * @return
     */
    public Long getQuesNum(@NotNull QuesDBItem quesDBItem) {
        return data.stream().filter(item -> item.equals(quesDBItem))
                .mapToLong(QuesDBItem::getQuantity).sum();
    }

    /**
     * 校验构造参数
     *
     * @param dataList
     * @param priorityCondition
     */
    private void initCheck(List<QuesDBItem> dataList, String priorityCondition) {

    }

    /**
     * 设置题库信息
     * 题库信息按照
     *
     * @param data
     * @return
     */
    private QuesDB setData(List<QuesDBItem> data) {
        this.data = data.stream()
                .sorted(Comparator.comparing(item -> item.getGroupMap().get(priorityCondition)))
                .collect(Collectors.toList());
        this.numArr = this.data.stream()
                .mapToInt(QuesDBItem::getQuantity).toArray();
        return this;
    }

    private QuesDB initConditionBitmap(Set<String> conditions) {
        this.conditions = conditions;
        this.conditionBitmap = new HashMap<>(this.conditions.size());
        this.conditionIndexes = new HashMap<>();
        this.conditions.forEach(conditionStr -> {
            // 构建conditionStr标识的单个条件的bitmap
            this.conditionBitmap.put(conditionStr, new Bitmap(this.data.size()));
            this.conditionIndexes.put(conditionStr, new ArrayList<>());
        });
        // 统计data并将相应的数据记录到对应的bitmap中
        for (int i = 0; i < this.data.size(); i++) {
            // QuesDBItem 遍历
            for (Map.Entry<String, Long> entry : this.data.get(i).getGroupMap().entrySet()) {
                String key = entry.getKey() + entry.getValue();
                if (this.conditionBitmap.containsKey(key)) {
                    this.conditionBitmap.get(key).set(i);
                }
                if (this.conditionIndexes.containsKey(key)) {
                    this.conditionIndexes.get(key).add(i);
                }
            }
        }
        return this;
    }
}
