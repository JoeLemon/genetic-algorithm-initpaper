package com.joelemon.paperGenerator;

import lombok.Builder;
import lombok.Data;

/**
 * @author HJY
 * @date 2020/2/16
 */
@Data
@Builder
public class ConstraintItem {

    // 限制条件名称
    private String name;

    // 限制条件识别码
    private Long code;

    // 限制条件归属分组名称
    private String groupName;

    // 该限制条件权重
    @Builder.Default
    private float weightness = 1F;

    // 该节点占用比例
    @Builder.Default
    private float rate = 1F;

    // 约束条件数量
    private float quantity;

    @Override
    public String toString(){
        return this.groupName + this.code;
    }
}
