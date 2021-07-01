package com.joelemon.paperGenerator;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author HJY
 * @date 2020/2/21
 */
public class PaperGeneticAlgorithm {

    private static final int EVOLUTIONTIMES_DEFAULT = 500;
    private static final int GROUPSIZE_DEFAULT = 100;
    private static final float TARGET_DEFAULT = 1F;
    private static final float CROSSOVER_RATE_DEFAULT = 0.3F;
    private static final float MUTATION_RATE_DEFAULT = 0.3F;

    // 试题题库数量数据
    private QuesDB quesDB;

    // 试题约束条件
    private QuesConditions quesConditions;

    // 种群
    private List<Paper> paperList;

    // 种群规模
    private int groupSize;

    // 种群迭代次数
    private int evolutionTimes;

    // 适应度目标
    private float target;

    // 交叉个体占总种群比例
    private float crossoverRate;

    // 变异概率
    private float mutationRate;

    private Random random = new Random();

    // constructor
    public PaperGeneticAlgorithm(QuesDB quesDB, QuesConditions quesConditions, int groupSize,
                 int evolutionTimes, float target, float crossoverRate, float mutationRate) {
        this.quesDB = quesDB;
        this.quesConditions = quesConditions;
        this.groupSize = groupSize;
        this.evolutionTimes = evolutionTimes;
        if (target <= 1 && target > 0) {
            this.target = target;
        } else {
            this.target = TARGET_DEFAULT;
        }
        if (crossoverRate <= 1 && crossoverRate > 0) {
            this.crossoverRate = crossoverRate;
        } else {
            this.crossoverRate = CROSSOVER_RATE_DEFAULT;
        }
        if (mutationRate <= 1 && mutationRate > 0) {
            this.mutationRate = mutationRate;
        } else {
            this.mutationRate = MUTATION_RATE_DEFAULT;
        }
    }

    public PaperGeneticAlgorithm(QuesDB quesDB, QuesConditions quesConditions) {
        this(quesDB, quesConditions, GROUPSIZE_DEFAULT, EVOLUTIONTIMES_DEFAULT, TARGET_DEFAULT, CROSSOVER_RATE_DEFAULT, MUTATION_RATE_DEFAULT);
    }

    /**
     * 进化方法
     *
     * @return
     */
    public Paper evolution() {
        int evolutionLoop = this.evolutionTimes;
        while (evolutionLoop >= 0) {
            // 按照适应度排序
            if (this.paperList.get(0).getFitness() == this.target) {
                break;
            }
            // 交叉 根据交叉率确定种群迭代一次执行的交叉次数
            int crossoverTimes = Math.round(crossoverRate * this.paperList.size());
            while (crossoverTimes > 0) {
                int index1 = this.selector();
                int index2 = this.selector();
                // 随机另一个交叉下标
                for (int i = 0; i < 10 && index1 == index2; i++) {
                    index2 = this.selector();
                }
                // 10次都随机同一下标
                if (index1 == index2) {
                    throw new RuntimeException("group size can not do crossover operation");
                }
                this.crossover(index1, index2);
                crossoverTimes--;
            }
            System.out.print((this.evolutionTimes - evolutionLoop + 1) + ". [crossover]: ");
            this.analyzePaperList();
            System.out.print(" [mutation]: ");
            // 变异
            this.mutation();
            this.analyzePaperList();

            // 淘汰
            this.eliminate();

            System.out.println("");

            evolutionLoop--;
        }
        return this.paperList.get(0);
    }

    /**
     * 初始化种群
     */
    public void initGroup() {
        Supplier<Paper> paperSupplier = () -> new Paper(quesDB, quesConditions);
        this.paperList = Stream.generate(paperSupplier)
                .limit(this.groupSize).map(paper -> paper.init())
                .sorted(Comparator.comparing(Paper::getFitness).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 通过轮盘赌的方式选择个体
     * @return 随机的下标
     */
    public int selector() {
        float randomFloat = random.nextFloat() * paperList.size();
        // 获取随机对象下标
        int paperListIndex = 0;
        while (randomFloat > 0) {
            if (paperListIndex >= paperList.size()) {
                paperListIndex = 0;
            }
            randomFloat -= paperList.get(paperListIndex).getFitness();
            paperListIndex++;
        }
        if (paperListIndex >= paperList.size()) {
            paperListIndex = 0;
        }
        return paperListIndex;
    }

    /**
     * 将 paperList 中以 paperIndex0, paperIndex1 为下标的两个体进行交叉操作
     *
     * @param paperIndex0
     * @param paperIndex1
     */
    public void crossover(int paperIndex0, int paperIndex1) {
        List<ConstraintItem> priorityGroupConstraints = this.quesConditions.getPriorityGroupConstraints();
        int endIndex = this.quesConditions.getTotal() - 1;
        int beginIndex;
        int quantity;
        int[] beginIndexArray = new int[priorityGroupConstraints.size()];
        int[] endIndexArray = new int[priorityGroupConstraints.size()];
        for (int i = priorityGroupConstraints.size() - 1; i >= 0; i--) {
            quantity = (int) priorityGroupConstraints.get(i).getQuantity();
            beginIndex = endIndex - random.nextInt(quantity);
            // 交换两个个体 [beginIndex, endIndex] 之间的元素
            beginIndexArray[i] = beginIndex;
            endIndexArray[i] = endIndex;
            // 更新endIndex
            endIndex -= quantity;
        }
        List<Paper> crossoverPapers = this.paperList.get(paperIndex0)
                .crossover(this.paperList.get(paperIndex1), beginIndexArray, endIndexArray);
        this.paperList.addAll(crossoverPapers);
    }

    /**
     * 根据变异率对所有个体进行变异
     */
    public void mutation() {
        List<Paper> mutationPaperList = new ArrayList<>();
        for (Paper paper : this.paperList) {
            Paper mutationPaper = paper.mutation(this.mutationRate);
            if (mutationPaper != null) {
                mutationPaperList.add(mutationPaper);
            }
        }
        if (mutationPaperList.size() > 0) {
            this.paperList.addAll(mutationPaperList);
            this.paperList.sort(Comparator.comparingDouble(Paper::getFitness).reversed());
        }
    }

    public void eliminate(){
        // 开启精英模式 淘汰适应度末尾的个体
        int removeNum = this.paperList.size() - this.groupSize;
        for (int i = this.paperList.size() - 1; removeNum > 0; i--) {
            this.paperList.remove(i);
            removeNum--;
        }
    }

    public void analyzePaperList(){
        double max = this.paperList.stream().mapToDouble(Paper::getFitness).max().getAsDouble();
        double average = this.paperList.stream().mapToDouble(Paper::getFitness).average().getAsDouble();
        System.out.print("max: " + String.format("%.2f", max) + ", average: " + String.format("%.2f", average));
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < this.paperList.size(); i++) {
            stringBuffer.append("paper" + i + " :");
            stringBuffer.append(Arrays.stream(this.paperList.get(i).getQuesData())
                    .mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }

    // getter setter
    public int getGroupSize() {
        return groupSize;
    }

    public PaperGeneticAlgorithm setGroupSize(int groupSize) {
        this.groupSize = groupSize;
        return this;
    }

    public int getEvolutionTimes() {
        return evolutionTimes;
    }

    public PaperGeneticAlgorithm setEvolutionTimes(int evolutionTimes) {
        this.evolutionTimes = evolutionTimes;
        return this;
    }
}
