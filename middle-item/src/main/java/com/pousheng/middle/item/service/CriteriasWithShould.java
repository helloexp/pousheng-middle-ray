package com.pousheng.middle.item.service;

import io.terminus.search.api.query.Criterias;
import io.terminus.search.api.query.CriteriasBuilder;
import io.terminus.search.api.query.Range;
import io.terminus.search.api.query.Terms;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/7/17
 */
public class CriteriasWithShould extends Criterias {

    private boolean hasShould;

    private List<Terms> shouldTerms;

    private boolean hasShouldNot;

    private List<Terms> shouldNotTerms;

    private boolean hasShouldRange;

    private List<Range> shouldRanges;

    public CriteriasWithShould(CriteriasBuilder cb) {
        super(cb);
    }

    public boolean isHasShould() {
        return hasShould;
    }

    public CriteriasWithShould hasShould(boolean hasShould) {
        this.hasShould = hasShould;
        return this;
    }

    public List<Terms> getShouldTerms() {
        return shouldTerms;
    }

    public CriteriasWithShould shouldTerms(List<Terms> shouldTerms) {
        this.shouldTerms = shouldTerms;
        return this;
    }

    public boolean isHasShouldNot() {
        return hasShouldNot;
    }

    public CriteriasWithShould hasShouldNot(boolean hasShouldNot) {
        this.hasShouldNot = hasShouldNot;
        return this;
    }

    public List<Terms> getShouldNotTerms() {
        return shouldNotTerms;
    }

    public CriteriasWithShould shouldNotTerms(List<Terms> shouldNotTerms) {
        this.shouldNotTerms = shouldNotTerms;
        return this;
    }

    public boolean isHasShouldRange() {
        return hasShouldRange;
    }

    public CriteriasWithShould hasShouldRange(boolean hasShouldRange) {
        this.hasShouldRange = hasShouldRange;
        return this;
    }

    public List<Range> getShouldRanges() {
        return shouldRanges;
    }

    public CriteriasWithShould shouldRanges(List<Range> shouldRanges) {
        this.shouldRanges = shouldRanges;
        return this;
    }
}
