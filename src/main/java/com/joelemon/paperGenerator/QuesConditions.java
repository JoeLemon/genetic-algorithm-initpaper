package com.joelemon.paperGenerator;

import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * 限制条件类
 *
 * @author HJY
 * @date 2020/2/19
 */
@Data
public class QuesConditions {

    /**
     * testType [10001,10002,10003]
     * lessoncategory [2342343,234542354,3454353]
     */
    private Map<String, List<ConstraintItem>> groupMap;

    /**
     * testType10001   233
     * testType10002   108
     */
    private Map<String, Float> quantityMap;

    private Map<String, Float> weightnessMap;

    /**
     * ["testType10001", "testType10002", ...]
     */
    private Set<String> conditions;

    private int total;

    // 除 priorityCondition 外所有条件的权重求和
    private float diffSummation;

    // data排序首要字段
    private String priorityCondition;

    private List<String> priorityGroupItemKeys;

    public QuesConditions(List<ConstraintItem> constraintItems, String priorityCondition) {
        // 初始化groupMap
        this.groupMap = constraintItems.stream()
                .collect(groupingBy(ConstraintItem::getGroupName));
        // 首要字段限制条件按照自然排序排序
        this.groupMap.put(priorityCondition,
                this.groupMap.get(priorityCondition).stream()
                .sorted(Comparator.comparing(ConstraintItem::getCode)).collect(Collectors.toList()));
        this.weightnessMap = constraintItems.stream()
                .collect(Collectors.toMap(ConstraintItem::toString, ConstraintItem::getWeightness, (v1, v2) -> Math.max(v1,v2)));
        // 根据首要条件统计总题量
        this.total = (int) this.groupMap.get(priorityCondition).stream()
                .mapToDouble(ConstraintItem::getQuantity).sum();
        // 初始化比率条件对应的数量，并汇总所有单一条件
        this.conditions = constraintItems.stream().map(item -> {
            if (item.getRate() != 1F) {
                item.setQuantity(item.getRate() * this.total);
            }
            return item;
        }).map(ConstraintItem::toString).collect(Collectors.toSet());
        this.quantityMap = constraintItems.stream()
                .collect(Collectors.toMap(ConstraintItem::toString, ConstraintItem::getQuantity, (v1, v2) -> v1 + v2));
        this.diffSummation = (float) constraintItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getWeightness()).sum();
        this.priorityCondition = priorityCondition;
    }

    public List<ConstraintItem> getGroupConstraints(String groupName) {
        if (!groupMap.containsKey(groupName)) {
            throw new RuntimeException("can not find groupName : " + groupName);
        }
        return groupMap.get(groupName);
    }

    public List<ConstraintItem> getPriorityGroupConstraints() {
        return this.getGroupConstraints(this.priorityCondition);
    }

    public float getConditionQuantity(String conditionKey) {
        if (this.quantityMap.containsKey(conditionKey)) {
            return quantityMap.get(conditionKey);
        } else {
            throw new RuntimeException("quantityMap can not find conditionKey : " + conditionKey);
        }
    }

    public float getConditionWeightness(String conditionKey) {
        if (this.weightnessMap.containsKey(conditionKey)) {
            return weightnessMap.get(conditionKey);
        } else {
            throw new RuntimeException("weightnessMap can not find conditionKey : " + conditionKey);
        }
    }

}
