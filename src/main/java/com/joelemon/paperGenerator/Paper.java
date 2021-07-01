package com.joelemon.paperGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 试卷
 *
 * @author HJY
 * @date 2020/2/15
 */
public class Paper {

    // 试题题库数量数据
    private QuesDB quesDB;

    // 试题约束条件
    private QuesConditions quesConditions;

    // 试卷DNA 表示quesDB.data中的下标
    private int[] quesData;

    // DNA变化则需要该标志位置为true
    private boolean needUpdateFitness = true;

    private float fitness;

    // Constructor
    public Paper(QuesDB quesDB, QuesConditions quesConditions) {
        this.quesDB = quesDB;
        this.quesConditions = quesConditions;
    }

    // Constructor
    public Paper(QuesDB quesDB, QuesConditions quesConditions, int[] quesData) {
        this.quesDB = quesDB;
        this.quesConditions = quesConditions;
        this.quesData = quesData;
    }

    /**
     * 获取该个体的适应度
     *
     * @return
     */
    public float getFitness() {
        if (!needUpdateFitness) {
            return fitness;
        }
        // 试卷差异度
        float paperDiffSummation = this.getDifference();
        // 计算适应度 真实适应度为 originFitness
        float originFitness = 1 - paperDiffSummation / quesConditions.getDiffSummation();
        this.fitness = FunctionSet.POWER_FUNC.apply(originFitness);
        this.needUpdateFitness = false;
        return this.fitness;
    }

    // 计算限制条件下的差异绝对值之和
    private float getDifference() {
        return (float) quesConditions.getConditions().stream()
                .mapToDouble(conditionKey ->
                        quesConditions.getConditionWeightness(conditionKey) * Math.abs(quesDB.getPaperConstraintSum(conditionKey, quesData)
                                - quesConditions.getConditionQuantity(conditionKey))).sum();
    }

    /**
     * 根据首要限制条件初始化
     *
     * @return
     */
    public Paper init() {
        this.quesData = new int[quesConditions.getTotal()];
        List<ConstraintItem> priorityGroupConstraints = quesConditions.getPriorityGroupConstraints();
        int quesDataIndex = 0;
        int quesDBIndex = 0;
        for (ConstraintItem constraintItem : priorityGroupConstraints) {
            String conditionKey = constraintItem.toString();
            List<Integer> conditionIndexes = this.quesDB.getConditionIndexes(conditionKey);
            float quantity = constraintItem.getQuantity();
            List<Integer> numList = conditionIndexes.stream()
                    .map(index -> this.quesDB.getQuantityArray(index)).collect(Collectors.toList());
            Random random = new Random();
            for (int i = 0; i < quantity; ) {
                // 随机取 numList 中的某一个非零元素下标，
                int index = random.nextInt(conditionIndexes.size());
                if (numList.get(index) > 0) {
                    this.quesData[quesDataIndex + i] = quesDBIndex + index;
                    numList.set(index, numList.get(index) - 1);
                    i++;
                }
            }
            quesDataIndex += quantity;
            quesDBIndex += conditionIndexes.size();
        }
        return this;
    }

    /**
     * 交换遗传编码
     *
     * @param paper           与之交换的个体
     * @param beginIndexArray 交换开始的下标位置
     * @param endIndexArray   交换结束的下标位置
     * @return
     */
    public List<Paper> crossover(Paper paper, int[] beginIndexArray, int[] endIndexArray) {
        if (beginIndexArray.length != endIndexArray.length) {
            throw new RuntimeException("crossover option error, cause index error");
        }
        Paper selfClone = this.clone();
        Paper paperClone = paper.clone();
        for (int i = 0; i < beginIndexArray.length; i++) {
            if (!(0 <= beginIndexArray[i] && beginIndexArray[i] <= endIndexArray[i] && endIndexArray[i] < this.getQuesData().length)) {
                throw new IndexOutOfBoundsException("crossover option error");
            }
            int cache;
            int[] paperQuesData = paperClone.getQuesData();
            while (endIndexArray[i] >= beginIndexArray[i]) {
                cache = paperQuesData[endIndexArray[i]];
                paperQuesData[endIndexArray[i]] = selfClone.quesData[endIndexArray[i]];
                selfClone.quesData[endIndexArray[i]] = cache;
                endIndexArray[i]--;
            }
        }
        ArrayList<Paper> papers = new ArrayList<>(2);
        selfClone.adjustAndRepair();
        papers.add(selfClone);
        paperClone.adjustAndRepair();
        papers.add(paperClone);
        return papers;
    }

    @Override
    protected Paper clone() {
        int[] copyQuesData = new int[this.quesData.length];
        for (int i = 0; i < copyQuesData.length; i++) {
            copyQuesData[i] = quesData[i];
        }
        return new Paper(this.quesDB, this.quesConditions, copyQuesData);
    }

    /**
     * 校验个体遗传因子是否符合题库题数，不符合的部分会进行修复操作
     */
    public boolean adjustAndRepair(){
        // 统计题目数量
        //(效率较差，故舍弃) Arrays.stream(this.quesData).mapToObj(Integer::new).collect(groupingBy(Function.identity(),Collectors.counting()));
        Map<Integer, Integer> countMap = this.getCountMap();
        Map<Integer, Integer> excessCountMap = new HashMap<>();
        countMap.forEach((index, num) -> {
            if (!(num <= this.quesDB.getQuantityArray(index))) {
                excessCountMap.put(index, countMap.get(index) - this.quesDB.getQuantityArray(index));
            }
        });
        if (excessCountMap.size() == 0) {
//            System.out.println("crossover success!");
            return true;
        } else {
//            System.out.println("begin adjust myself (" + this.getQuesData()[0] + " " + this.getQuesData()[1] + "...)");
//            excessCountMap.forEach((index, num) -> {
//                System.out.println("下标 " + index + ", 超出" + num);
//            });
            // 调整 index 为调整数量的下标，num为调整的数量
            excessCountMap.forEach((index, num) -> {
                // 获取下标对应的首要条件
                long priorityValue = this.quesDB.getQuesDBItem(index).getGroupMap()
                        .get(this.quesConditions.getPriorityCondition());
                int[] replaceIndexArray = new int[num];
                num--;
                while (num >= 0) {
                    int randomIndex = this.quesDB.getRandomIndexByPriorityCode(priorityValue);
                    if (randomIndex != index) {
                        replaceIndexArray[num] = randomIndex;
                        num--;
                    }
                }
                // 将不满足的下标进行替换
                for (int i = this.quesData.length - 1, replaceIndex = replaceIndexArray.length - 1;
                     i >= 0 && replaceIndex >= 0; i--) {
                    if (this.quesData[i] == index) {
//                        System.out.println("将第" + i + "位, 由" + this.quesData[i] + "调整为" + replaceIndexArray[replaceIndex]);
                        this.quesData[i] = replaceIndexArray[replaceIndex];
                        replaceIndex--;
                    }
                }
            });
            // 再次自我校验一遍
            return this.adjustAndRepair();
        }
    }

    /**
     * 变异函数
     * @param mutationRate 变异概率
     */
    public Paper mutation(float mutationRate) {
        Random random = new Random();
        // 决定是否变异
        if (random.nextFloat() > mutationRate) {
            return null;
        }
        // 变异控制
        List<ConstraintItem> priorityGroupConstraints = this.quesConditions.getPriorityGroupConstraints();
        Paper clone = this.clone();

        // 获取变异点index
        int endIndex = this.quesConditions.getTotal() - 1;
        int beginIndex;
        int quantity;
        for (int i = priorityGroupConstraints.size() -1; i >= 0; i--) {
            quantity = (int) priorityGroupConstraints.get(i).getQuantity();
            beginIndex = endIndex - random.nextInt(quantity);
            // 替换beginIndex
            clone.getQuesData()[beginIndex] = this.quesDB.getRandomIndexByPriorityCode(priorityGroupConstraints.get(i).getCode());
            // 更新endIndex
            endIndex -= quantity;
        }

        clone.adjustAndRepair();
        // 返回变异体
        return clone;
    }

    /**
     * 获取遗传数据
     *
     * @return
     */
    public int[] getQuesData() {
        return this.quesData;
    }

    /**
     * 统计试题各个下标对应的数量
     * @return
     */
    public Map<Integer, Integer> getCountMap(){
        Map<Integer, Integer> countMap = new HashMap<>();
        for (int i = this.quesData.length - 1; i>= 0; i--) {
            if (countMap.containsKey(this.quesData[i])) {
                countMap.put(this.quesData[i], countMap.get(this.quesData[i]) + 1);
            } else {
                countMap.put(this.quesData[i], 1);
            }
        }
        return countMap;
    }

    /**
     * 获取该试卷抽题详细数据
     *  注：该方法调用时会影响QuesDB中的数据 Non-idempotent
     * @return
     */
    public List<QuesDBItem> getQuesDBList(){
        Map<Integer, Integer> countMap = this.getCountMap();
        List<QuesDBItem> result = new ArrayList<>(countMap.size());
        countMap.forEach((index, num) -> {
            QuesDBItem quesDBItem = this.quesDB.getData().get(index);
            quesDBItem.setQuantity(num);
            result.add(quesDBItem);
        });
        return result;
    }

    /**
     * 返回遗传信息 Non-idempotent
     * @return
     */
    @Override
    public String toString() {
        return this.getQuesDBList().stream()
                .map(item -> item.getFingerPrint() + " [" + item.getQuantity() + "]")
                .collect(Collectors.joining(", "));
    }
}
