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

package io.jmix.ui.component.impl;

import io.jmix.ui.component.ValuesPicker;
import io.jmix.ui.widget.JmixPickerField;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class WebValuesPicker<V> extends WebValuePicker<Collection<V>> implements ValuesPicker<V> {

    protected JmixPickerField<Collection<V>> createComponent() {
        return new JmixPickerField<>();
    }

    @Override
    public void setValue(@Nullable Collection<V> value) {
        setValueInternal(value, false);
    }

    @Override
    public void setValueFromUser(@Nullable Collection<V> value) {
        setValueInternal(value, true);
    }

    protected void setValueInternal(@Nullable Collection<V> value, boolean userOriginated) {
        Collection<V> oldValue = getOldValue(value);

        oldValue = new ArrayList<>(oldValue != null
                ? oldValue
                : Collections.emptyList());

        setValueToPresentation(convertToPresentation(value));

        this.internalValue = value;

        fireValueChange(oldValue, value, userOriginated);
        refreshActionsState();
    }

    @Nullable
    protected Collection<V> getOldValue(@Nullable Collection<V> newValue) {
        return equalCollections(newValue, internalValue)
                ? component.getValue()
                : internalValue;
    }

    @Override
    protected boolean fieldValueEquals(@Nullable Collection<V> value, @Nullable Collection<V> oldValue) {
        return equalCollections(value, oldValue);
    }

    protected boolean equalCollections(@Nullable Collection<V> a, @Nullable Collection<V> b) {
        if (CollectionUtils.isEmpty(a) && CollectionUtils.isEmpty(b)) {
            return true;
        }

        if ((CollectionUtils.isEmpty(a) && CollectionUtils.isNotEmpty(b))
                || (CollectionUtils.isNotEmpty(a) && CollectionUtils.isEmpty(b))) {
            return false;
        }

        //noinspection ConstantConditions
        return CollectionUtils.isEqualCollection(a, b);
    }

    protected void fireValueChange(@Nullable Collection<V> oldValue, @Nullable Collection<V> value,
                                   boolean userOriginated) {
        if (!fieldValueEquals(oldValue, value)) {
            ValueChangeEvent<Collection<V>> event =
                    new ValueChangeEvent<>(this, oldValue, value, userOriginated);
            publish(ValueChangeEvent.class, event);
        }
    }
}
