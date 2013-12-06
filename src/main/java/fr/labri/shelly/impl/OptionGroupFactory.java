package fr.labri.shelly.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import fr.labri.shelly.ConverterFactory;
import fr.labri.shelly.Option;
import fr.labri.shelly.ShellyItem;
import fr.labri.shelly.annotations.CommandGroup;
import fr.labri.shelly.impl.Visitor.TraversalVisitor;

public class OptionGroupFactory {
	public static final OptionGroupFactory DEFAULT = new OptionGroupFactory();
	
	public ConverterFactory factory = new fr.labri.shelly.impl.ConverterFactory();
	
	private ConverterFactory loadFactory(Class<? extends ConverterFactory> newFactory) {
		if(factory.getClass().equals(newFactory))
			return factory;
		try {
			return newFactory.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public OptionGroup createModel(Class<?> clazz) {
		CommandGroup annotation = clazz.getAnnotation(CMDGRP_CLASS);
		if(annotation == null)
			throw new RuntimeException("Cannot create model from a non command group class " + clazz);
		String name = annotation.name() == fr.labri.shelly.annotations.Option.NO_NAME ? annotation.name() : clazz.getSimpleName().toLowerCase();

		return new Builder().build(name, clazz);
	}

	private class Builder extends TraversalVisitor {
		
		public Builder() {}

		public OptionGroup build(String name, Class<?> clazz) {
			OptionGroup grp = createGroup(null, name, clazz);
			visit(grp);
			return grp;
		}
		
		@Override
		public void visit(fr.labri.shelly.OptionGroup optionGroup) {
			populate((OptionGroup)optionGroup);
			optionGroup.visit_all(this);
		}
		
		public OptionGroup createGroup(OptionGroup parent, String name, Class<?> clazz) {
			if((parent == null) != (clazz.getEnclosingClass() == null))
				throw new RuntimeException("Cannot create option group when not starting at top level"); // FIXME this message is obviousy wrong, but I have no better idea for now
			OptionGroup grp = new OptionGroup(parent, name, clazz);
			return grp;
		}

		protected void populate(OptionGroup grp) {
			Class<?> clazz = grp.getAssociatedClass();
			
			for (Field f : clazz.getFields())
				if (f.isAnnotationPresent(OPT_CLASS)) 
					grp.addOption(newItem(f.getAnnotation(OPT_CLASS), f, grp));
			for (Method m : clazz.getMethods())
				if (m.isAnnotationPresent(CMD_CLASS))
					grp.addCommand(newItem(m.getAnnotation(CMD_CLASS), m, grp));
			for (Class<?> c : clazz.getClasses())
				if (c.isAnnotationPresent(CMDGRP_CLASS))
					grp.addCommand(newItem(c.getAnnotation(CMDGRP_CLASS), c, Modifier.isStatic(c.getModifiers()) ? null : grp));
				else if (c.isAnnotationPresent(OPTGRP_CLASS))
					grp.addCommand(newItem(c.getAnnotation(OPTGRP_CLASS), c, Modifier.isStatic(c.getModifiers()) ? null : grp));
		}
		
		protected Option newItem(fr.labri.shelly.annotations.Option annotation, Field field, fr.labri.shelly.OptionGroup parent) {
			String name = annotation.name() != fr.labri.shelly.annotations.Option.NO_NAME ? annotation.name() : field.getName().toLowerCase();
			return new SimpleOption(loadFactory(annotation.factory()), parent, name, field);
		}
		
		protected ShellyItem newItem(fr.labri.shelly.annotations.Command annotation, Method method, OptionGroup parent) {
			String name = annotation.name() == fr.labri.shelly.annotations.Option.NO_NAME ? annotation.name() : method.getName().toLowerCase();
			return new SimpleCommand(loadFactory(annotation.factory()), parent, name, method);
		}
		
		protected ShellyItem newItem(fr.labri.shelly.annotations.CommandGroup annotation, Class<?> clazz, OptionGroup parent) {
			return null;
		}
		
		protected ShellyItem newItem(fr.labri.shelly.annotations.OptionGroup annotation, Class<?> clazz, OptionGroup parent) {
			String name = annotation.name() == fr.labri.shelly.annotations.Option.NO_NAME ? annotation.name() : clazz.getSimpleName().toLowerCase();
			return createGroup(parent, name, clazz);
		}
	}
	
	static public final Class<fr.labri.shelly.annotations.Option> OPT_CLASS = fr.labri.shelly.annotations.Option.class;
	static public final Class<fr.labri.shelly.annotations.Command> CMD_CLASS = fr.labri.shelly.annotations.Command.class;
	static public final Class<fr.labri.shelly.annotations.OptionGroup> OPTGRP_CLASS = fr.labri.shelly.annotations.OptionGroup.class;
	static public final Class<fr.labri.shelly.annotations.CommandGroup> CMDGRP_CLASS = fr.labri.shelly.annotations.CommandGroup.class;
}
