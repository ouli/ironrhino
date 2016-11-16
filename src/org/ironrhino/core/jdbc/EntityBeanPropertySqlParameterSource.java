package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class EntityBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {

	private final BeanWrapper beanWrapper;

	public EntityBeanPropertySqlParameterSource(Object object) {
		super(object);
		beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}

	@Override
	public boolean hasValue(String name) {
		boolean b = super.hasValue(name);
		if (b)
			return b;
		for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
			Column column = pd.getReadMethod().getAnnotation(Column.class);
			if (column == null) {
				try {
					column = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
							.getAnnotation(Column.class);
				} catch (NoSuchFieldException e) {
				}
			}
			if (column != null && name.equals(column.name()))
				return true;
		}
		return false;
	}

	@Override
	public Object getValue(String name) throws IllegalArgumentException {
		if (beanWrapper.isReadableProperty(name)) {
			Object value = beanWrapper.getPropertyValue(name);
			if (value instanceof Enum) {
				Enumerated enumerated = beanWrapper.getPropertyDescriptor(name).getReadMethod()
						.getAnnotation(Enumerated.class);
				if (enumerated == null) {
					try {
						enumerated = ReflectionUtils.getField(beanWrapper.getWrappedClass(), name)
								.getAnnotation(Enumerated.class);
					} catch (NoSuchFieldException e) {
					}
				}
				Enum<?> en = ((Enum<?>) value);
				return enumerated != null && enumerated.value() == EnumType.STRING ? en.name() : en.ordinal();
			}
			return value;
		} else {
			for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
				Enumerated enumerated = null;
				if (Enum.class.isAssignableFrom(pd.getPropertyType())) {
					enumerated = pd.getReadMethod().getAnnotation(Enumerated.class);
					if (enumerated == null) {
						try {
							enumerated = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
									.getAnnotation(Enumerated.class);
						} catch (NoSuchFieldException e) {
						}
					}
				}
				Column column = pd.getReadMethod().getAnnotation(Column.class);
				if (column == null) {
					try {
						column = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
								.getAnnotation(Column.class);
					} catch (NoSuchFieldException e) {
					}
				}
				if (column != null && name.equals(column.name())) {
					Object value = beanWrapper.getPropertyValue(pd.getName());
					if (value instanceof Enum) {
						Enum<?> en = ((Enum<?>) value);
						value = enumerated != null && enumerated.value() == EnumType.STRING ? en.name() : en.ordinal();
					}
					return value;
				}
			}
			throw new IllegalArgumentException(
					"No such property '" + name + "' of bean " + beanWrapper.getWrappedClass().getName());
		}

	}

}