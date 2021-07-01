package com.joelemon.paperGenerator;

import java.util.function.Function;

/**
 * @author HJY
 * @date 2020/2/21
 */
public class FunctionSet {

    // 适应度函数转换 目标为将[1,0.5]中间的差异拉大
    public static Function<Float, Float> INVERSE_FUNC = x -> 1/(15 - 14 * x);
    public static Function<Float, Float> POWER_FUNC = x -> (float) Math.pow(64, x - 1);
}
