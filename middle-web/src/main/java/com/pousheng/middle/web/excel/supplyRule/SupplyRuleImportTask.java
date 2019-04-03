package com.pousheng.middle.web.excel.supplyRule;

import com.google.common.base.Preconditions;
import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.web.excel.AbstractSimpleTask;
import com.pousheng.middle.web.excel.TaskMetaDTO;
import com.pousheng.middle.web.excel.supplyRule.factory.ExcelParserFactory;
import io.terminus.common.utils.BeanMapper;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-08 16:10<br/>
 */
@Slf4j
@Data
@ToString(callSuper = true)
public class SupplyRuleImportTask extends AbstractSimpleTask {
    private static final long serialVersionUID = 4243453968256088972L;

    private final SupplyRuleExcelParser supplyRuleExcelParser;

    public SupplyRuleImportTask(TaskDTO taskDTO) {
        // 构建任务 meta
        Map<String, Object> content = taskDTO.getContent();
        if (CollectionUtils.isEmpty(content)) {
            log.error("task({}) content empty, task meta build fail.", taskDTO);
            throw new IllegalArgumentException("task content empty");
        }

        // construct meta info
        TaskMetaDTO taskMetaDTO = new TaskMetaDTO();

        String filePath = (String) content.get("filePath");
        String fileName = (String) content.get("fileName");
        Boolean delta = (Boolean) content.getOrDefault("delta", false);
        Preconditions.checkArgument(StringUtils.hasText(filePath), "file path blank");
        Preconditions.checkArgument(StringUtils.hasText(fileName), "file name blank");
        taskMetaDTO.setDelta(delta);
        taskMetaDTO.setFilePath(filePath);
        taskMetaDTO.setFileName(fileName);
        taskMetaDTO.setId(taskDTO.getId());
        taskMetaDTO.setManualStop(0);
        taskMetaDTO.setTimeout(0);
        taskMetaDTO.setCreatedAt(taskDTO.getCreatedAt());
        taskMetaDTO.setUpdatedAt(null);
        try {
            setMetaInfo(BeanMapper.convertObjectToMap(taskMetaDTO));
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        supplyRuleExcelParser = ExcelParserFactory.get(taskDTO.getId(), filePath, delta);
    }

    @Override
    public boolean equalsTo(Object other) {
        return Objects.equals(getTaskId(), other);
    }

    @Override
    public Object getTaskKey() {
        return getMetaInfo().get("id");
    }

    @Override
    public Long getTaskId() {
        return ((Number) getTaskKey()).longValue();
    }

    @Override
    public void preStart() {
        supplyRuleExcelParser.preStart();
    }

    @Override
    public void start() {
        supplyRuleExcelParser.start();
    }

    @Override
    public void postStop() {
        this.supplyRuleExcelParser.postStop();
    }

    @Override
    public void onStop() {
        this.supplyRuleExcelParser.onStop();
    }

    @Override
    public void onError() {
        this.supplyRuleExcelParser.onError();
    }
}
