/* TemplateResolver.java

	Purpose:
		
	Description:
		
	History:
		2012/1/4 Created by Dennis Chen

Copyright (C) 2011 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.bind.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.zkoss.bind.BindContext;
import org.zkoss.bind.Binder;
import org.zkoss.bind.IterationStatus;
import org.zkoss.bind.sys.BindEvaluatorX;
import org.zkoss.bind.sys.BinderCtrl;
import org.zkoss.bind.sys.Binding;
import org.zkoss.bind.sys.LoadBinding;
import org.zkoss.bind.sys.TemplateResolver;
import org.zkoss.bind.xel.zel.BindELContext;
import org.zkoss.xel.ExpressionX;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.util.Template;

/**
 * rename it to template binding?
 * @author dennis
 *
 */
public class TemplateResolverImpl implements TemplateResolver, /*Binding,*/ Serializable{//implement binding for tieValue
	
	
	private static final String EACH_VAR = "each";
	private static final String EACH_STATUS_VAR = "eachStatus";
	
	
	private static final long serialVersionUID = 1L;
	private final String _templateExpr;
	private final Map<String, Object> _templateArgs;
	private final Binder _binder;
	private final String _attr;
	private final Component _comp;
	private final ExpressionX _expression;
	
	public TemplateResolverImpl(Binder binder,Component comp,String attr,String templateExpr, Map<String, Object> templateArgs) {
		_binder = binder;
		_comp = comp;
		_templateExpr = templateExpr;
		_templateArgs = templateArgs;
		_attr = attr;
		//don't need to pass this, since add tracker was do by loadbinding
		final BindContext ctx = BindContextUtil.newBindContext(binder, null, false, null, _comp, null);
		_expression = binder.getEvaluatorX().parseExpressionX(ctx, templateExpr, Object.class);
	}
	
	public String getExpression(){
		return _templateExpr;
	}

	private Template lookupTemplate(Component comp, String name) {
		if(comp==null) return null;
		Template template = comp.getTemplate(name);
		return template==null?lookupTemplate(comp.getParent(),name):template;
	}

	
	@Override
	public Template resolveTemplate(Component eachComp, Object eachData, final int index) {
		
		final BindEvaluatorX eval = _binder.getEvaluatorX();
		//pass this as the binding to context, we need to enable tieValue
		final BindContext ctx = BindContextUtil.newBindContext(_binder, null, false, null, eachComp, null);
		
		Object oldEach = eachComp.getAttribute("each");
		Object oldStatus = eachComp.getAttribute("eachStatus");
		
		//prepare each
		eachComp.setAttribute(EACH_VAR, eachData);
		eachComp.setAttribute(EACH_STATUS_VAR, new AbstractIterationStatus(){
			private static final long serialVersionUID = 1L;
			@Override
			public int getIndex() {
				return index;
			}
		});

		Object value = eval.getValue(ctx, eachComp, _expression);
		
		if(oldEach==null){
			eachComp.removeAttribute(EACH_VAR);
		}else{
			eachComp.setAttribute(EACH_VAR,oldEach);
		}
		if(oldStatus==null){
			eachComp.removeAttribute(EACH_STATUS_VAR);
		}else{
			eachComp.setAttribute(EACH_STATUS_VAR,oldStatus);
		}
		
		if(value instanceof Template){
			return (Template) value;
		}else if(value instanceof String){
			Template template = lookupTemplate(_comp,(String)value);
			if (template == null && ((String)value).indexOf('.') > 0) { //might be a class path
				try {
					template = (Template) _comp.getPage().resolveClass(((String)value)).newInstance();
				} catch (Exception e) {
					//ignore;
				}
			}
			return template;
		}else{
			throw new UiException("unknow template type "+value);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(_templateExpr).append("]").append(this);
		return sb.toString();
	}
}
