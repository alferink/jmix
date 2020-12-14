/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.ui.app.filter.condition;

import io.jmix.core.Messages;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.Action;
import io.jmix.ui.action.filter.FilterAddConditionAction;
import io.jmix.ui.action.list.EditAction;
import io.jmix.ui.action.list.RemoveAction;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.HasValue;
import io.jmix.ui.component.ListComponent;
import io.jmix.ui.component.LogicalFilterComponent;
import io.jmix.ui.component.TextField;
import io.jmix.ui.component.Tree;
import io.jmix.ui.component.filter.FilterConditionsBuilder;
import io.jmix.ui.component.filter.registration.FilterComponents;
import io.jmix.ui.component.groupfilter.LogicalFilterSupport;
import io.jmix.ui.entity.FilterCondition;
import io.jmix.ui.entity.GroupFilterCondition;
import io.jmix.ui.entity.LogicalFilterCondition;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.model.InstanceLoader;
import io.jmix.ui.screen.EditedEntityContainer;
import io.jmix.ui.screen.Install;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@UiController("ui_GroupFilterCondition.edit")
@UiDescriptor("group-filter-condition-edit.xml")
@EditedEntityContainer("filterConditionDc")
public class GroupFilterConditionEdit extends LogicalFilterConditionEdit<GroupFilterCondition> {

    @Autowired
    protected LogicalFilterSupport logicalFilterSupport;
    @Autowired
    protected FilterComponents filterComponents;
    @Autowired
    protected FilterConditionsBuilder builder;
    @Autowired
    protected Messages messages;

    @Autowired
    protected InstanceContainer<GroupFilterCondition> filterConditionDc;
    @Autowired
    protected InstanceLoader<GroupFilterCondition> filterConditionDl;
    @Autowired
    protected CollectionContainer<FilterCondition> filterConditionsDc;

    @Autowired
    protected Button moveDownButton;
    @Autowired
    protected Button moveUpButton;
    @Autowired
    protected Tree<FilterCondition> conditionsTree;
    @Autowired
    protected TextField<String> captionField;
    @Autowired
    protected Notifications notifications;

    @Override
    public InstanceContainer<GroupFilterCondition> getInstanceContainer() {
        return filterConditionDc;
    }

    @Override
    public CollectionContainer<FilterCondition> getCollectionContainer() {
        return filterConditionsDc;
    }

    @Nullable
    @Override
    public FilterAddConditionAction getAddAction() {
        return (FilterAddConditionAction) conditionsTree.getAction("addCondition");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public EditAction<FilterCondition> getEditAction() {
        return (EditAction<FilterCondition>) conditionsTree.getAction("edit");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public RemoveAction<FilterCondition> getRemoveAction() {
        return (RemoveAction<FilterCondition>) conditionsTree.getAction("remove");
    }

    @Nullable
    @Override
    public ListComponent<FilterCondition> getListComponent() {
        return conditionsTree;
    }

    @Subscribe
    protected void onAfterShow(AfterShowEvent event) {
        conditionsTree.expandTree();
    }

    @SuppressWarnings("unchecked")
    @Install(to = "conditionsTree", subject = "lookupSelectHandler")
    protected void conditionsTreeLookupSelectHandler(Collection<FilterCondition> collection) {
        EditAction<FilterCondition> editAction = (EditAction<FilterCondition>) conditionsTree.getAction("edit");
        if (editAction != null) {
            editAction.execute();
        }
    }

    @Install(to = "operationField", subject = "optionCaptionProvider")
    protected String operationFieldOptionCaptionProvider(LogicalFilterComponent.Operation operation) {
        return logicalFilterSupport.getOperationCaption(operation);
    }

    @Subscribe("conditionsTree.moveUp")
    protected void onConditionsTreeMoveUp(Action.ActionPerformedEvent event) {
        FilterCondition selectedCondition = conditionsTree.getSingleSelected();
        if (selectedCondition != null) {
            FilterCondition parent = selectedCondition.getParent();
            if (parent instanceof LogicalFilterCondition) {
                List<FilterCondition> items = filterConditionsDc.getMutableItems();
                List<FilterCondition> ownConditions = ((LogicalFilterCondition) parent).getOwnFilterConditions();

                int selectedItemIndex = items.indexOf(selectedCondition);
                int selectedOwnItemIndex = ownConditions.indexOf(selectedCondition);

                Collections.swap(items, selectedItemIndex, selectedItemIndex - 1);
                Collections.swap(ownConditions, selectedOwnItemIndex, selectedOwnItemIndex - 1);
                updateMoveButtonsState(selectedCondition);
            }
        }
    }

    @Subscribe("conditionsTree.moveDown")
    protected void onConditionsTreeMoveDown(Action.ActionPerformedEvent event) {
        FilterCondition selectedCondition = conditionsTree.getSingleSelected();
        if (selectedCondition != null) {
            FilterCondition parent = selectedCondition.getParent();
            if (parent instanceof LogicalFilterCondition) {
                List<FilterCondition> items = filterConditionsDc.getMutableItems();
                List<FilterCondition> ownConditions = ((LogicalFilterCondition) parent).getOwnFilterConditions();

                int selectedItemIndex = items.indexOf(selectedCondition);
                int selectedOwnItemIndex = ownConditions.indexOf(selectedCondition);

                Collections.swap(items, selectedItemIndex, selectedItemIndex + 1);
                Collections.swap(ownConditions, selectedOwnItemIndex, selectedOwnItemIndex + 1);
                updateMoveButtonsState(selectedCondition);
            }
        }
    }

    @Subscribe("conditionsTree")
    protected void onConditionsTreeSelection(Tree.SelectionEvent<FilterCondition> event) {
        if (!event.getSelected().isEmpty()) {
            FilterCondition selectedCondition = event.getSelected().iterator().next();
            updateMoveButtonsState(selectedCondition);
        }
    }

    protected void updateMoveButtonsState(FilterCondition selectedCondition) {
        boolean moveUpButtonEnabled = false;
        boolean moveDownButtonEnabled = false;
        FilterCondition parent = selectedCondition.getParent();
        if (parent instanceof LogicalFilterCondition) {
            int index = ((LogicalFilterCondition) parent).getOwnFilterConditions().indexOf(selectedCondition);
            moveUpButtonEnabled = index > 0;
            moveDownButtonEnabled = index < ((LogicalFilterCondition) parent).getOwnFilterConditions().size() - 1;
        }

        moveUpButton.setEnabled(moveUpButtonEnabled);
        moveDownButton.setEnabled(moveDownButtonEnabled);
    }

    @Subscribe("operationField")
    protected void onOperationFieldValueChange(HasValue.ValueChangeEvent<LogicalFilterComponent.Operation> event) {
        LogicalFilterComponent.Operation operation = event.getValue();
        if (operation != null && event.isUserOriginated()) {
            captionField.setValue(logicalFilterSupport.getOperationCaption(operation));
        }
    }
}
