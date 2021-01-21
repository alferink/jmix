package io.jmix.graphql.schema;

import graphql.language.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.jmix.graphql.schema.Types.listValueDef;
import static io.jmix.graphql.schema.Types.valueDef;

@Component
public class FilterTypesBuilder extends BaseTypesBuilder {

    private static final Logger log = LoggerFactory.getLogger(FilterTypesBuilder.class);

    public enum ConditionUnionType {
// todo need to investigate how to implement using jmix conditions
//        NOT,
        AND,
        OR;

        @Nullable
        public static ConditionUnionType find(String type) {
            return Arrays.stream(FilterTypesBuilder.ConditionUnionType.values())
                    .filter(conditionUnionType -> conditionUnionType.name().equals(type))
                    .findAny().orElse(null);
        }
    }

    @Autowired
    InpTypesBuilder inpTypesBuilder;

    @Override
    protected String normalizeName(String entityName) {
        return inpTypesBuilder.normalizeName(entityName);
    }

    // todo operators to constants or enums
    public InputObjectTypeDefinition buildScalarFilterConditionType(String scalarTypeName) {

        String comment = String.format(
                "expression to compare columns of type %s. All fields are combined with logical 'AND'", scalarTypeName);

        String name = composeFilterConditionTypeName(scalarTypeName);
        return InputObjectTypeDefinition.newInputObjectDefinition()
                .name(name)
                .comments(Collections.singletonList(new Comment(comment, null)))
                .inputValueDefinition(valueDef("EQ", scalarTypeName, "equals"))
                .inputValueDefinition(valueDef("NEQ", scalarTypeName, "not equals"))
                .inputValueDefinition(valueDef("GT", scalarTypeName, "greater than"))
                .inputValueDefinition(valueDef("GTE", scalarTypeName, "greater than or equals"))
                .inputValueDefinition(valueDef("LT", scalarTypeName, "less that"))
                .inputValueDefinition(valueDef("LTE", scalarTypeName, "less than or equals"))
// todo not supported in data fetcher now
//                .inputValueDefinition(listValueDef("IN", scalarTypeName, "in"))
//                .inputValueDefinition(listValueDef("NIN", scalarTypeName, "not in"))
//                .inputValueDefinition(valueDef("IS_NULL", scalarTypeName, "is null"))
                .inputValueDefinition(listValueDef(ConditionUnionType.AND.name(), name, null))
//                .inputValueDefinition(listValueDef(ConditionUnionType.NOT.name(), name, null))
                .inputValueDefinition(listValueDef(ConditionUnionType.OR.name(), name, null))
                .build();
    }


    public InputObjectTypeDefinition buildFilterConditionType(MetaClass metaClass) {

        String className = composeFilterConditionTypeName(metaClass);

        InputObjectTypeDefinition.Builder builder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name(className);

        List<InputValueDefinition> valueDefs = metaClass.getProperties().stream()
                .map(metaProperty -> {

                    // todo support enums
                    if (metaProperty.getType().equals(MetaProperty.Type.ENUM)) {
                        return listValueDef(metaProperty.getName(), "String", null);
                    }

                    // todo "-to-many" relations are not supported now
                    if (metaProperty.getRange().getCardinality().isMany()) {
                        return null;
                    }

                    String typeName = composeFilterConditionTypeName(getFieldTypeName(metaProperty));
                    return listValueDef(metaProperty.getName(), typeName, null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // todo description
        valueDefs.add(listValueDef(ConditionUnionType.AND.name(), className, null));
//        valueDefs.add(listValueDef(ConditionUnionType.NOT.name(), className, null));
        valueDefs.add(listValueDef(ConditionUnionType.OR.name(), className, null));

        builder.inputValueDefinitions(valueDefs);

        log.debug("buildFilterConditionType: for class {}", metaClass);
        return builder.build();
    }

    public InputObjectTypeDefinition buildFilterOrderByType(MetaClass metaClass) {

        String className = composeFilterOrderByTypeName(metaClass);

        InputObjectTypeDefinition.Builder builder = InputObjectTypeDefinition.newInputObjectDefinition()
                .name(className);

        List<InputValueDefinition> valueDefs = metaClass.getProperties().stream()
                .map(metaProperty -> {

                    // todo support enums
                    if (metaProperty.getType().equals(MetaProperty.Type.ENUM)) {
                        return listValueDef(metaProperty.getName(), "String", null);
                    }

                    // todo "-to-many" relations are not supported now
                    if (metaProperty.getRange().getCardinality().isMany()) {
                        return null;
                    }

                    if (metaProperty.getJavaType().getSimpleName().equals("String")) {
                        return valueDef(metaProperty.getName(), "SortOrder", null);
                    }

                    if (metaProperty.getRange().isClass()) {
                        // todo now we support only persistent entities
                        if (!metadataTools.isPersistent(metaProperty.getJavaType())) {
                            return null;
                        }
                        String typeName = composeFilterOrderByTypeName(getFieldTypeName(metaProperty));
                        return valueDef(metaProperty.getName(), typeName, null);
                    }

                    // datatype attributes
                    return valueDef(metaProperty.getName(), Types.SortOrder.class.getSimpleName(), null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        builder.inputValueDefinitions(valueDefs);

        log.debug("buildFilterOrderByType: for class {}", metaClass);
        return builder.build();
    }

    protected String composeFilterOrderByTypeName(MetaClass metaClass) {
        String name = metadataTools.getEntityName(metaClass.getJavaClass());
        return composeFilterOrderByTypeName(name);
    }

    protected String composeFilterOrderByTypeName(String name) {
        return composeFilterTypeName(name, "OrderBy");
    }

    protected String composeFilterConditionTypeName(MetaClass metaClass) {
        String name = metadataTools.getEntityName(metaClass.getJavaClass());
        return composeFilterTypeName(name, "FilterCondition");
    }

    private String composeFilterConditionTypeName(String name) {
        return composeFilterTypeName(name, "FilterCondition");
    }

    protected String composeFilterTypeName(String name, String suffix) {
        // verify that name is normalized
        if (!name.startsWith("inp_")) {
            name = normalizeName(name);
        }
        return name + suffix;
    }

}
