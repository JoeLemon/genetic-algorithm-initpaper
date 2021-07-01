package com.joelemon.paperGenerator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 试题条件组合
 * groupMap 表示对应条件
 * quantity 表示该条件组合对应的试题数量
 * @author HJY
 * @date 2020/2/17
 */
@Data
public class QuesDBItem {
    private static final String DELIMITER = ",";
    private static final String EQUAL = ":";

    public static String EMPTY = "";

    private String fingerPrint;

    /**
     * key: groupkey  value: groupValue
     */
    private Map<String, Long> groupMap;

    private int quantity;

    public String getFingerPrint() {
        if (StringUtils.isEmpty(fingerPrint)) {
            this.setFingerPrint();
        }
        return this.fingerPrint;
    }

    /**
     * 设置指纹 方便进行数据项对比
     * @return
     */
    public QuesDBItem setFingerPrint() {
        if (groupMap != null && groupMap.size() > 0) {
            this.fingerPrint = groupMap.entrySet().stream().map(item -> item.getKey() + EQUAL + item.getValue())
                    .sorted().collect(Collectors.joining(DELIMITER));
        } else {
            this.fingerPrint = EMPTY;
        }
        return this;
    }

    public boolean equals(QuesDBItem quesDBItem) {
        return this.getFingerPrint().equals(quesDBItem.getFingerPrint());
    }
}
