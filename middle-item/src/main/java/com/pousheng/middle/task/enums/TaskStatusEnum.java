package com.pousheng.middle.task.enums;


/**
 * @author zhaoxw
 * @date 2018/5/3
 */
public enum TaskStatusEnum {

    INIT(0, "准备执行"),
    EXECUTING(1, "执行中"),
    FINISH(2, "执行完毕"),
    ERROR(3, "执行异常"),
    STOPPED(4, "手动终止");

    private final Integer value;
    private final String desc;

    TaskStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return desc;
    }


}
