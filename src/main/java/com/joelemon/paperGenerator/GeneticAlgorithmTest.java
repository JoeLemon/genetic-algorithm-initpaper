package com.joelemon.paperGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author HJY
 * @date 2020/2/15
 */
public class GeneticAlgorithmTest {

    public static final String TYPE = "testType";
    public static final String DIFFI = "difficulty";
    public static final String LESS = "lessoncategory";

    public static final String PRIOR_CON = GeneticAlgorithmTest.TYPE;

    public static long[] testTypes = new long[]{10001L, 10002L, 10003L};
    public static long[] lessoncategories = new long[]{1234567L, 2345678L, 3456789L, 4567890L, 5678901L};
    public static long[] difficulties = new long[]{1L, 2L, 3L};

    /**
     * 生成测试数据
     */
    public static List<QuesDBItem> initData() {
        // 三个试题类型，五个知识点，三个难度
        // 生成题库数据 每种分类组合100题，共有4500道题
        List<QuesDBItem> groupData = new ArrayList<>();
        for (int i = 0; i < testTypes.length; i++) {
            for (int i1 = 0; i1 < difficulties.length; i1++) {
                for (int i2 = 0; i2 < lessoncategories.length; i2++) {
                    QuesDBItem quesDBItem = new QuesDBItem();
                    Map<String, Long> itemGroupMap = new HashMap<>();
                    itemGroupMap.put(TYPE, testTypes[i]);
                    itemGroupMap.put(DIFFI, difficulties[i1]);
                    itemGroupMap.put(LESS, lessoncategories[i2]);
                    quesDBItem.setGroupMap(itemGroupMap);
                    quesDBItem.setQuantity(300);
                    groupData.add(quesDBItem);
                }
            }
        }
        return groupData;
    }

    private static List<ConstraintItem> addConditions(List<ConstraintItem> conditions,
                                                      String groupName, long[] codeArr, int[] numArr) {
        for (int i = 0; i < numArr.length; i++) {
            conditions.add(ConstraintItem.builder()
                    .groupName(groupName)
                    .code(codeArr[i])
                    .quantity(numArr[i])
                    .build()
            );
        }
        return conditions;
    }
    private static List<ConstraintItem> addConditionsByRates(List<ConstraintItem> conditions,
                                                      String groupName, long[] codeArr, float[] rateArr) {
        for (int i = 0; i < rateArr.length; i++) {
            conditions.add(ConstraintItem.builder()
                    .groupName(groupName)
                    .code(codeArr[i])
                    .rate(rateArr[i])
                    .build()
            );
        }
        return conditions;
    }

    public static QuesConditions initQuesConditions() {
        List<ConstraintItem> conditions = new ArrayList<>();

        int[] testTypesConditions = new int[]{50, 50, 100};
        int[] lessoncategoriesConditions = new int[]{60, 20, 40, 40, 40};
        float[] lessoncategoriesConditionsRate = new float[]{0.33F, 0.07F, 0.2F, 0.2F, 0.2F};
        int[] difficultiesConditions = new int[]{60, 70, 70};

        addConditions(conditions, TYPE, testTypes, testTypesConditions);
        addConditionsByRates(conditions, LESS, lessoncategories, lessoncategoriesConditionsRate);
//        addConditions(conditions, LESS, lessoncategories, lessoncategoriesConditions);
        addConditions(conditions, DIFFI, difficulties, difficultiesConditions);

        QuesConditions quesConditions = new QuesConditions(conditions, PRIOR_CON);
        return quesConditions;
    }

    public static void main(String[] args) {
        long beginTime = System.currentTimeMillis();
        List<QuesDBItem> quesDB = GeneticAlgorithmTest.initData();
        QuesConditions quesConditions = GeneticAlgorithmTest.initQuesConditions();
        QuesDB db = new QuesDB(quesDB, quesConditions.getConditions(), PRIOR_CON);
        PaperGeneticAlgorithm paperGeneticAlgorithm = new PaperGeneticAlgorithm(db, quesConditions);
        paperGeneticAlgorithm.initGroup();
        Paper target = paperGeneticAlgorithm.evolution();
        System.out.println("Fitness: " + target.getFitness());
        System.out.println("Paper DNA: " + target.toString());
        System.out.println("down, take " + (System.currentTimeMillis() - beginTime));
    }

}
