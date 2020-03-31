package com.pousheng.middle.schedule.jobs;

import io.terminus.common.model.PageInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.item.database.AbstractPagingItemReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.util.ClassUtils.getShortName;

/**
 * Mybatis 分页读取
 * @param <T>
 */
public class MyBatisPagingItemReader<T> extends AbstractPagingItemReader<T> {

    private String queryId;
    private SqlSessionFactory sqlSessionFactory;
    @Getter
    private SqlSessionTemplate sqlSessionTemplate;
    private Map<String, Object> parameterValues;

    @Setter
    private ExecutorType executorType = ExecutorType.SIMPLE;


    public MyBatisPagingItemReader(ExecutorType executorType) {
        this.executorType = executorType;
        setName(getShortName(org.mybatis.spring.batch.MyBatisPagingItemReader.class));
    }

    public MyBatisPagingItemReader() {
        setName(getShortName(org.mybatis.spring.batch.MyBatisPagingItemReader.class));
    }

    /**
     * Public setter for {@link SqlSessionFactory} for injection purposes.
     */
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Public setter for {@link SqlSessionTemplate} for injection purpose
     */
    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * Public setter for the statement id identifying the statement in the SqlMap
     * configuration file.
     *
     * @param queryId the id for the statement
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * The parameter values to be used for the query execution.
     *
     * @param parameterValues the values keyed by the parameter named used in
     *                        the query string.
     */
    public void setParameterValues(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
    }

    protected Map<String, Object> getParameterValues() {
        return parameterValues;
    }

    /**
     * Check mandatory properties.
     *
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        notNull(sqlSessionFactory, "sqlSessionFactory can not be null");
        notNull(queryId, "queryId can not be null");


        if (Objects.isNull(sqlSessionTemplate)) {
            if (nonNull(executorType)) {
                sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, executorType);
            } else {
                sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
            }
        }
    }

    @Override
    protected void doReadPage() {
        Map<String, Object> parameters = new HashMap<>();
        if (parameterValues != null) {
            parameters.putAll(parameterValues);
        }


        PageInfo pageInfo = new PageInfo(getPage() + 1, getPageSize());
        parameters.put("offset", pageInfo.getOffset());
        parameters.put("limit", pageInfo.getLimit());

        if (results == null) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
        results.addAll(sqlSessionTemplate.selectList(queryId, parameters));
    }

    @Override
    protected void doJumpToPage(int itemIndex) {
        // Not Implemented
    }

}