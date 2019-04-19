package com.pousheng.middle.utils;

import io.terminus.common.utils.Splitters;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.pousheng.middle.utils.MatchPolicy.MATCH_ANY;
import static com.pousheng.middle.utils.MatchPolicy.MATCH_NON;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-18 10:21<br/>
 */
class OnEnvCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context,
                                            AnnotatedTypeMetadata metadata) {
        List<AnnotationAttributes> allAnnotationAttributes = annotationAttributesFromMultiValueMap(
                metadata.getAllAnnotationAttributes(
                        ConditionalOnEnv.class.getName()));
        List<ConditionMessage> noMatch = new ArrayList<>();
        List<ConditionMessage> match = new ArrayList<>();
        for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
            ConditionOutcome outcome = determineOutcome(annotationAttributes,
                    context.getEnvironment());
            (outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
        }
        if (!noMatch.isEmpty()) {
            return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
        }
        return ConditionOutcome.match(ConditionMessage.of(match));
    }

    private List<AnnotationAttributes> annotationAttributesFromMultiValueMap(
            MultiValueMap<String, Object> multiValueMap) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : multiValueMap.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                Map<String, Object> map;
                if (i < maps.size()) {
                    map = maps.get(i);
                } else {
                    map = new HashMap<>();
                    maps.add(map);
                }
                map.put(entry.getKey(), entry.getValue().get(i));
            }
        }
        List<AnnotationAttributes> annotationAttributes = new ArrayList<AnnotationAttributes>(
                maps.size());
        for (Map<String, Object> map : maps) {
            annotationAttributes.add(AnnotationAttributes.fromMap(map));
        }
        return annotationAttributes;
    }

    private ConditionOutcome determineOutcome(AnnotationAttributes annotationAttributes,
                                              PropertyResolver resolver) {
        Spec spec = new Spec(annotationAttributes);
        List<String> missingProperties = new ArrayList<>();
        List<String> nonMatchingProperties = new ArrayList<>();
        spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
        if (!missingProperties.isEmpty()) {
            return ConditionOutcome.noMatch(
                    ConditionMessage.forCondition(ConditionalOnEnv.class, spec)
                            .didNotFind("property", "properties")
                            .items(ConditionMessage.Style.QUOTE, missingProperties));
        }
        if (!nonMatchingProperties.isEmpty()) {
            return ConditionOutcome.noMatch(
                    ConditionMessage.forCondition(ConditionalOnEnv.class, spec)
                            .found("evaluate failed in property",
                                    "evaluate failed in properties")
                            .items(ConditionMessage.Style.QUOTE, nonMatchingProperties));
        }
        return ConditionOutcome.match(ConditionMessage
                .forCondition(ConditionalOnEnv.class, spec).because("matched"));
    }

    private static class Spec {

        private final String name;
        private final List<String> envs;

        private final String havingValue;

        private final MatchPolicy matchPolicy;

        private final boolean matchIfMissing;

        private final boolean matchIfEnvMissing;

        private final boolean matchIfValueMissing;

        Spec(AnnotationAttributes annotationAttributes) {
            this.name = annotationAttributes.getString("name");
            this.envs = env();

            this.havingValue = annotationAttributes.getString("havingValue");
            this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
            this.matchIfEnvMissing = annotationAttributes.getBoolean("matchIfEnvMissing");
            this.matchIfValueMissing = annotationAttributes.getBoolean("matchIfValueMissing");
            this.matchPolicy = annotationAttributes.getEnum("matchPolicy");
        }

        private void collectProperties(PropertyResolver resolver,
                                       List<String> missing,
                                       List<String> nonMatching) {
            resolver = new RelaxedPropertyResolver(resolver, "");

            if (resolver.containsProperty(havingValue)) {
                if (!isMatch(resolver.getProperty(havingValue))) {
                    nonMatching.add(name);
                }
            } else {
                if (!this.matchIfMissing) {
                    missing.add(name);
                }
            }
        }

        private boolean isMatch(String value) {
            if (matchIfEnvMissing && envs.isEmpty()) {
                return true;
            }
            if (StringUtils.hasLength(value)) {
                List<String> values = Splitters.COMMA.splitToList(value);

                boolean allMatch = true;
                for (String env : envs) {
                    boolean contains = values.contains(env);
                    if (matchPolicy == MATCH_ANY && contains) {
                        return true;
                    } else if(matchPolicy == MATCH_NON && contains) {
                        return false;
                    }
                    allMatch = allMatch && contains;
                }

                if (matchPolicy == MATCH_ANY) {
                    return false;
                } else if (matchPolicy == MATCH_NON) {
                    return true;
                } else {
                    return allMatch;
                }
            } else {
                if (matchIfValueMissing) {
                    return true;
                }

                if (matchPolicy == MATCH_NON) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        private List<String> env() {
            if (StringUtils.isEmpty(System.getenv(name))) {
                return Collections.emptyList();
            }
            return Splitters.COMMA.splitToList(System.getenv(name));
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("(");
            result.append(this.name);

            if (StringUtils.hasLength(this.havingValue)) {
                result.append(" ").append(this.matchPolicy.getDetail()).append(" ");
                result.append(this.havingValue);
            }
            result.append(")");
            return result.toString();
        }

    }

}
