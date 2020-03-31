package com.pousheng.middle.item;

import com.pousheng.middle.item.service.CriteriasWithShould;
import io.terminus.search.api.query.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Iterator;
import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/7/17
 */
public class PsCriteriasBuilder extends CriteriasBuilder {
    @Getter
    @Setter
    private List<Term> notTerm;
    @Getter
    @Setter
    private List<Terms> notTerms;
    @Getter
    @Setter
    private List<Term> wildcard;

    public boolean isHasShouldNot() {
        return hasShouldNot;
    }

    public PsCriteriasBuilder hasShouldNot(boolean hasShouldNot) {
        this.hasShouldNot = hasShouldNot;
        return this;
    }

    public List<Terms> getShouldNotTerms() {
        return shouldNotTerms;
    }

    public PsCriteriasBuilder shouldNotTerms(List<Terms> shouldNotTerms) {
        this.shouldNotTerms = shouldNotTerms;
        return this;
    }

    public boolean isHasShould() {
        return hasShould;
    }

    public PsCriteriasBuilder hasShould(boolean hasShould) {
        this.hasShould = hasShould;
        return this;
    }

    public List<Terms> getShouldTerms() {
        return shouldTerms;
    }

    public PsCriteriasBuilder shouldTerms(List<Terms> shouldTerms) {
        this.shouldTerms = shouldTerms;
        return this;
    }

    private boolean hasShouldNot;

    private List<Terms> shouldNotTerms;

    public boolean isHasShouldRange() {
        return hasShouldRange;
    }

    public PsCriteriasBuilder hasShouldRange(boolean hasShouldRange) {
        this.hasShouldRange = hasShouldRange;
        return this;
    }

    private boolean hasShould;

    private List<Terms> shouldTerms;

    private boolean hasShouldRange;

    public List<Range> getShouldRanges() {
        return shouldRanges;
    }

    public PsCriteriasBuilder shouldRanges(List<Range> shouldRanges) {
        this.shouldRanges = shouldRanges;
        return this;
    }

    private List<Range> shouldRanges;

    public PsCriteriasBuilder withShouldTerms(List<Terms> termsList) {
        if (!CollectionUtils.isEmpty(termsList)) {
            this.shouldTerms = termsList;
            Iterator it = this.shouldTerms.iterator();

            while (it.hasNext()) {
                Terms tms = (Terms) it.next();
                List<Element> values = tms.getValues();
                if (tms.getValues() != null && !tms.getValues().isEmpty()) {
                    Element element = values.get(values.size() - 1);
                    element.setLast(true);
                }
            }

            this.hasShould = true;
        }

        return this;
    }

    public PsCriteriasBuilder withShouldNotTerms(List<Terms> termsList) {
        if (!CollectionUtils.isEmpty(termsList)) {
            this.shouldNotTerms = termsList;
            Iterator it = this.shouldNotTerms.iterator();

            while (it.hasNext()) {
                Terms tms = (Terms) it.next();
                List<Element> values = tms.getValues();
                if (tms.getValues() != null && !tms.getValues().isEmpty()) {
                    Element element = values.get(values.size() - 1);
                    element.setLast(true);
                }
            }
        }

        return this;
    }

    public CriteriasWithShould build(CriteriasWithShould psCr) {
        if (psCr.getShouldNotTerms() != null && !psCr.getShouldNotTerms().isEmpty()) {
            Terms t = psCr.getShouldNotTerms().get(this.shouldNotTerms.size() - 1);
            t.setLast(true);
        }

        if (psCr.getShouldTerms() != null && !psCr.getShouldTerms().isEmpty()) {
            Terms t = psCr.getShouldTerms().get(this.shouldTerms.size() - 1);
            t.setLast(true);
        }

        if (psCr.getShouldRanges() != null && !psCr.getShouldRanges().isEmpty()) {
            Range r = psCr.getShouldRanges().get(psCr.getShouldRanges().size() - 1);
            r.setLast(true);
        }

        if (!ObjectUtils.isEmpty(psCr.getShouldTerms()) || !ObjectUtils.isEmpty(psCr.getShouldNotTerms()) || !ObjectUtils.isEmpty(psCr.getShouldRanges())) {
            psCr.hasShould(true);
        }

        if (!ObjectUtils.isEmpty(psCr.getShouldTerms()) && !ObjectUtils.isEmpty(psCr.getShouldNotTerms())) {
            psCr.hasShouldNot(true);
        }

        if (!ObjectUtils.isEmpty(psCr.getShouldNotTerms()) || !ObjectUtils.isEmpty(psCr.getShouldTerms())) {
            if (!ObjectUtils.isEmpty(psCr.getShouldRanges())) {
                psCr.hasShouldRange(true);
            }
        }

        psCr.setHasMustNotTerm(!CollectionUtils.isEmpty(notTerm) || !CollectionUtils.isEmpty(notTerms));
        if (!CollectionUtils.isEmpty(notTerms)) {
            psCr.setNotTerms(notTerms);
            notTerms.get(notTerms.size() - 1).setLast(Boolean.TRUE);
        }
        if (!CollectionUtils.isEmpty(notTerm)) {
            psCr.setNotTerm(notTerm);
            notTerm.get(notTerm.size() - 1).setLast(Boolean.TRUE);
        }

        if (getRanges() != null && !getRanges().isEmpty()) {
            Range r = getRanges().get(getRanges().size() - 1);
            r.setLast(true);
        } else if (getTerms() != null && !getTerms().isEmpty()) {
            Terms t = getTerms().get(getTerms().size() - 1);
            t.setLast(true);

        } else if (getTerm() != null && !getTerm().isEmpty()) {
            Term t = getTerm().get(getTerm().size() - 1);
            t.setLast(true);
        }
        if (getMustNotTerms() != null && !getMustNotTerms().isEmpty()) {
            Terms t = getMustNotTerms().get(getMustNotTerms().size() - 1);
            t.setLast(true);
        }

        return psCr;
    }
}
