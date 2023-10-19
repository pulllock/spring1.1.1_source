/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.MethodOverrides;
import org.springframework.beans.factory.support.ReplaceOverride;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the XmlBeanDefinitionParser interface.
 * Parses bean definitions according to the "spring-beans" DTD.
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 18.12.2003
 * XmlBeanDefinitionParser的默认实现
 */
public class DefaultXmlBeanDefinitionParser implements XmlBeanDefinitionParser {

	public static final String BEAN_NAME_DELIMITERS = ",; ";

	public static final String GENERATED_ID_SEPARATOR = "#";
	

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	public static final String TRUE_VALUE = "true";
	public static final String DEFAULT_VALUE = "default";
	public static final String DESCRIPTION_ELEMENT = "description";

	public static final String AUTOWIRE_BY_NAME_VALUE = "byName";
	public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";
	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";
	public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

	public static final String DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE = "all";
	public static final String DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE = "simple";
	public static final String DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE = "objects";

	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";
	public static final String DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE = "default-dependency-check";
	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

	public static final String IMPORT_ELEMENT = "import";
	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String BEAN_ELEMENT = "bean";
	public static final String ID_ATTRIBUTE = "id";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String PARENT_ATTRIBUTE = "parent";

	public static final String CLASS_ATTRIBUTE = "class";
	public static final String ABSTRACT_ATTRIBUTE = "abstract";
	public static final String SINGLETON_ATTRIBUTE = "singleton";
	public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";
	public static final String AUTOWIRE_ATTRIBUTE = "autowire";
	public static final String DEPENDENCY_CHECK_ATTRIBUTE = "dependency-check";
	public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
	public static final String INIT_METHOD_ATTRIBUTE = "init-method";
	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
	public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";
	public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
	public static final String INDEX_ATTRIBUTE = "index";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

	public static final String REPLACED_METHOD_ELEMENT = "replaced-method";
	public static final String REPLACER_ATTRIBUTE = "replacer";
	public static final String ARG_TYPE_ELEMENT = "arg-type";
	public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

	public static final String REF_ELEMENT = "ref";
	public static final String IDREF_ELEMENT = "idref";
	public static final String BEAN_REF_ATTRIBUTE = "bean";
	public static final String LOCAL_REF_ATTRIBUTE = "local";
	public static final String PARENT_REF_ATTRIBUTE = "parent";

	public static final String LIST_ELEMENT = "list";
	public static final String SET_ELEMENT = "set";
	public static final String MAP_ELEMENT = "map";
	public static final String ENTRY_ELEMENT = "entry";
	public static final String KEY_ATTRIBUTE = "key";
	public static final String PROPS_ELEMENT = "props";
	public static final String PROP_ELEMENT = "prop";
	public static final String VALUE_ELEMENT = "value";
	public static final String NULL_ELEMENT = "null";


	protected final Log logger = LogFactory.getLog(getClass());

	private BeanDefinitionReader beanDefinitionReader;

	private Resource resource;

	private String defaultLazyInit;

	private String defaultDependencyCheck;

	private String defaultAutowire;

	/**
	 * 使用给定的BeanDefinitionReader，Document和Resource注册Bean的信息
	 * @param reader the bean definition reader, containing the bean factory
	 * to work on and the bean class loader to use. Can also be used to load
	 * further bean definition files referenced by the given document.带有BeanFactory
	 * @param doc the DOM document
	 * @param resource descriptor of the original XML resource
	 * (useful for displaying parse errors)
	 * @return
	 * @throws BeansException
	 */
	public int registerBeanDefinitions(BeanDefinitionReader reader, Document doc, Resource resource)
			throws BeansException {
		this.beanDefinitionReader = reader;
		this.resource = resource;

		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();

		this.defaultLazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		logger.debug("Default lazy init '" + this.defaultLazyInit + "'");
		this.defaultDependencyCheck = root.getAttribute(DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE);
		logger.debug("Default dependency check '" + this.defaultDependencyCheck + "'");
		this.defaultAutowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		logger.debug("Default autowire '" + this.defaultAutowire + "'");

		NodeList nl = root.getChildNodes();
		int beanDefinitionCounter = 0;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				//解析import标签
				if (IMPORT_ELEMENT.equals(node.getNodeName())) {
					importBeanDefinitionResource(ele);
				}
				//解析bean标签
				else if (BEAN_ELEMENT.equals(node.getNodeName())) {
					beanDefinitionCounter++;
					registerBeanDefinition(ele);
				}
			}
		}
		logger.debug("Found " + beanDefinitionCounter + " <" + BEAN_ELEMENT + "> elements defining beans");
		return beanDefinitionCounter;
	}

	protected BeanDefinitionReader getBeanDefinitionReader() {
		return beanDefinitionReader;
	}

	protected String getDefaultLazyInit() {
		return defaultLazyInit;
	}

	protected String getDefaultDependencyCheck() {
		return defaultDependencyCheck;
	}

	protected String getDefaultAutowire() {
		return defaultAutowire;
	}

	protected Resource getResource() {
		return resource;
	}


	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * 解析import标签
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//获取import标签中的resource属性，也就是resource的位置
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		try {
			//获取import中对应的资源
			Resource relativeResource = this.resource.createRelative(location);
			//使用XmlBeanDefinitionReader来解析import中对应的资源
			this.beanDefinitionReader.loadBeanDefinitions(relativeResource);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Invalid relative resource location [" + location + "] to import bean definitions from", ex);
		}
	}

	/**
	 * Parse a "bean" element and register it with the bean factory.
	 * 解析Bean标签
	 */
	protected void registerBeanDefinition(Element ele) {
		//解析BeanDefinition，解析完之后BeanDefinitionHolder中就有了配置文件中配置的各种属性，比如class，name，id，alias等
		BeanDefinitionHolder bdHolder = parseBeanDefinition(ele);
		logger.debug("Registering bean definition with id '" + bdHolder.getBeanName() + "'");
		//从BeanDefinitionReader中获取Bean工厂，然后将BeanDefinition注册到工厂中去
		this.beanDefinitionReader.getBeanFactory().registerBeanDefinition(
				bdHolder.getBeanName(), bdHolder.getBeanDefinition());
		//注册别名
		if (bdHolder.getAliases() != null) {
			for (int i = 0; i < bdHolder.getAliases().length; i++) {
				this.beanDefinitionReader.getBeanFactory().registerAlias(
						bdHolder.getBeanName(), bdHolder.getAliases()[i]);
			}
		}
	}

	/**
	 * Parse a standard bean definition into a BeanDefinitionHolder,
	 * including bean name and aliases.
	 * <p>Bean elements specify their canonical name as "id" attribute
	 * and their aliases as a delimited "name" attribute.
	 * <p>If no "id" specified, uses the first name in the "name" attribute
	 * as canonical name, registering all others as aliases.
	 * 从Element中解析成BeanDefinition，存放到BeanDefinitionHolder中
	 * 如果没有id指定，使用name中第一个作为id
	 */
	protected BeanDefinitionHolder parseBeanDefinition(Element ele) {
		//id属性
		String id = ele.getAttribute(ID_ATTRIBUTE);
		//name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		//存放别名
		List aliases = new ArrayList();
		if (StringUtils.hasLength(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, BEAN_NAME_DELIMITERS, true, true);
			aliases.addAll(Arrays.asList(nameArr));
		}
		//没有id属性，使用别名的第一个作为id
		if (!StringUtils.hasLength(id) && !aliases.isEmpty()) {
			id = (String) aliases.remove(0);
			logger.debug("No XML 'id' specified - using '" + id + "' as ID and " + aliases + " as aliases");
		}
		//使用Element和id来解析BeanDefinition
		BeanDefinition beanDefinition = parseBeanDefinition(ele, id);
		//id不存在
		if (!StringUtils.hasLength(id)) {
			if (beanDefinition instanceof RootBeanDefinition) {
				//获取Bean的类名
				String className = ((RootBeanDefinition) beanDefinition).getBeanClassName();
				//将类名作为id
				id = className;
				int counter = 1;
				//如果已经存在，就使用类名#1之类的来当做id
				while (this.beanDefinitionReader.getBeanFactory().containsBeanDefinition(id)) {
					counter++;
					id = className + GENERATED_ID_SEPARATOR + counter;
				}
				logger.debug("Neither XML 'id' nor 'name' specified - using bean class name [" + id + "] as ID");
			}
			else if (beanDefinition instanceof ChildBeanDefinition) {
				throw new BeanDefinitionStoreException(
						this.resource, "", "Child bean definition has neither 'id' nor 'name'");
			}
		}

		String[] aliasesArray = (String[]) aliases.toArray(new String[aliases.size()]);
		//保存在BeanDefinitionHolder中
		return new BeanDefinitionHolder(beanDefinition, id, aliasesArray);
	}

	/**
	 * Parse the BeanDefinition itself, without regard to name or aliases.
	 * 解析BeanDefinition，不解析name和aliases
	 */
	protected BeanDefinition parseBeanDefinition(Element ele, String beanName) {
		String className = null;
		try {
			//class属性
			if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
				className = ele.getAttribute(CLASS_ATTRIBUTE);
			}
			String parent = null;
			//parent属性
			if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
				parent = ele.getAttribute(PARENT_ATTRIBUTE);
			}
			//constructor-arg元素
			ConstructorArgumentValues cargs = getConstructorArgSubElements(beanName, ele);
			//property元素
			MutablePropertyValues pvs = getPropertyValueSubElements(beanName, ele);
			//创建用于属性承载的AbstractBeanDefinition
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					className, parent, cargs, pvs, this.beanDefinitionReader.getBeanClassLoader());
			//depends-on属性
			if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
				String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
				bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, BEAN_NAME_DELIMITERS, true, true));
			}
			//factory-method属性
			if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
				bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
			}
			//factory-bean属性
			if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
				bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
			}
			//dependency-check属性
			String dependencyCheck = ele.getAttribute(DEPENDENCY_CHECK_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(dependencyCheck)) {
				dependencyCheck = this.defaultDependencyCheck;
			}
			//解析dependency-check属性
			bd.setDependencyCheck(getDependencyCheck(dependencyCheck));
			//autowire属性
			String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(autowire)) {
				autowire = this.defaultAutowire;
			}
			//解析autowire属性
			bd.setAutowireMode(getAutowireMode(autowire));
			//init-method属性
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			if (!initMethodName.equals("")) {
				bd.setInitMethodName(initMethodName);
			}
			//destory-method属性
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			if (!destroyMethodName.equals("")) {
				bd.setDestroyMethodName(destroyMethodName);
			}
			//解析lookup-method属性
			getLookupOverrideSubElements(bd.getMethodOverrides(), beanName, ele);
			//解析replaced-method属性
			getReplacedMethodSubElements(bd.getMethodOverrides(), beanName, ele);
			//描述
			bd.setResourceDescription(this.resource.getDescription());
			//abstract属性
			if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
				bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
			}
			//singleton属性
			if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
				bd.setSingleton(TRUE_VALUE.equals(ele.getAttribute(SINGLETON_ATTRIBUTE)));
			}
			//lazy-init属性
			String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(lazyInit) && bd.isSingleton()) {
				// just apply default to singletons, as lazy-init has no meaning for prototypes
				lazyInit = this.defaultLazyInit;
			}
			bd.setLazyInit(TRUE_VALUE.equals(lazyInit));

			return bd;
		}
		catch (ClassNotFoundException ex) {
			throw new BeanDefinitionStoreException(
					this.resource, beanName, "Bean class [" + className + "] not found", ex);
		}
		catch (NoClassDefFoundError err) {
			throw new BeanDefinitionStoreException(
					this.resource, beanName, "Class that bean class [" + className + "] depends on not found", err);
		}
	}

	/**
	 * Parse constructor argument subelements of the given bean element.
	 * 解析子元素的构造器参数，constructor-arg
	 */
	protected ConstructorArgumentValues getConstructorArgSubElements(String beanName, Element beanEle)
			throws ClassNotFoundException {
		//子结点
		NodeList nl = beanEle.getChildNodes();
		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//constructor-arg
			if (node instanceof Element && CONSTRUCTOR_ARG_ELEMENT.equals(node.getNodeName())) {
				//解析constructor-arg
				parseConstructorArgElement(beanName, cargs, (Element) node);
			}
		}
		return cargs;
	}

	/**
	 * Parse property value subelements of the given bean element.
	 * 解析property子元素
	 */
	protected MutablePropertyValues getPropertyValueSubElements(String beanName, Element beanEle) {
		NodeList nl = beanEle.getChildNodes();
		MutablePropertyValues pvs = new MutablePropertyValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//property元素
			if (node instanceof Element && PROPERTY_ELEMENT.equals(node.getNodeName())) {
				parsePropertyElement(beanName, pvs, (Element) node);
			}
		}
		return pvs;
	}

	/**
	 * Parse lookup-override sub elements
	 * 解析lookup-method
	 */
	protected void getLookupOverrideSubElements(MethodOverrides overrides, String beanName, Element beanEle) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//lookup-method
			if (node instanceof Element && LOOKUP_METHOD_ELEMENT.equals(node.getNodeName())) {
				Element ele = (Element) node;
				//要修饰的方法
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				//获取配置返回的bean
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				overrides.addOverride(new LookupOverride(methodName, beanRef));
			}
		}
	}

	/**
	 * replaced-method
	 * @param overrides
	 * @param beanName
	 * @param beanEle
	 */
	protected void getReplacedMethodSubElements(MethodOverrides overrides, String beanName, Element beanEle) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//replaces-method
			if (node instanceof Element && REPLACED_METHOD_ELEMENT.equals(node.getNodeName())) {
				Element replacedMethodEle = (Element) node;
				//要替换的旧方法
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				//新的替换方法
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);

				// Look for arg-type match elements
				//参数
				NodeList argTypeNodes = replacedMethodEle.getElementsByTagName(ARG_TYPE_ELEMENT);
				for (int j = 0; j < argTypeNodes.getLength(); j++) {
					Element argTypeEle = (Element) argTypeNodes.item(j);
					replaceOverride.addTypeIdentifier(argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE));
				}
				overrides.addOverride(replaceOverride);
			}
		}
	}

	/**
	 * Parse a constructor-arg element.
	 * 解析constructor-arg
	 * ele为constructor-arg
	 */
	protected void parseConstructorArgElement(String beanName, ConstructorArgumentValues cargs, Element ele)
			throws DOMException, ClassNotFoundException {
		//获取ele对应属性元素
		Object val = getPropertyValue(ele, beanName, null);
		//index属性
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		//type属性
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		//有index属性，则封装到indexedArgumentValue属性中
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					throw new BeanDefinitionStoreException(this.resource, beanName, "'index' cannot be lower than 0");
				}
				//有type属性
				if (StringUtils.hasLength(typeAttr)) {
					//将index，val，type添加到ConstructorArgumentValues的indexedArgumentValues map中
					cargs.addIndexedArgumentValue(index, val, typeAttr);
				}
				else {//没有type属性
					//将index，val添加到ConstructorArgumentValues的indexedArgumentValues map中
					cargs.addIndexedArgumentValue(index, val);
				}
			}
			catch (NumberFormatException ex) {
				throw new BeanDefinitionStoreException(this.resource, beanName,
						"Attribute 'index' of tag 'constructor-arg' must be an integer");
			}
		}
		//没有index属性，则封装到genericArgumentValues中
		else {
			//有type属性，则将val和type封装到genericArgumentValues中
			if (StringUtils.hasLength(typeAttr)) {
				cargs.addGenericArgumentValue(val, typeAttr);
			}
			else {//没有type属性，则只将value封装到genericArgumentValues属性中
				cargs.addGenericArgumentValue(val);
			}
		}
	}

	/**
	 * Parse a property element.
	 * 解析property元素
	 */
	protected void parsePropertyElement(String beanName, MutablePropertyValues pvs, Element ele)
			throws DOMException {
		//name属性
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
			throw new BeanDefinitionStoreException(
					this.resource, beanName, "Tag 'property' must have a 'name' attribute");
		}
		//获取property元素的值
		Object val = getPropertyValue(ele, beanName, propertyName);
		//封装成PropertyValue，添加到MutablePropertyValues中
		pvs.addPropertyValue(new PropertyValue(propertyName, val));
	}

	/**
	 * Get the value of a property element. May be a list etc.
	 * @param ele property element
	 * 获取property元素的值
	 */
	protected Object getPropertyValue(Element ele, String beanName, String propertyName) {
		// should only have one element child: value, ref, collection
		//一个属性只能有一个子元素，也就是只能有一种类型
		NodeList nl = ele.getChildNodes();
		Element valueRefOrCollectionElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element candidateEle = (Element) nl.item(i);
				//description属性不处理
				if (DESCRIPTION_ELEMENT.equals(candidateEle.getTagName())) {
					// keep going: we don't use this value for now
				}
				else {
					// child element is what we're looking for
					//我们要的元素
					valueRefOrCollectionElement = candidateEle;
				}
			}
		}
		if (valueRefOrCollectionElement == null) {
			String elementName = (propertyName != null) ?
					"<property> element for property '" + propertyName + "'" :
					"<constructor-arg> element";
			throw new BeanDefinitionStoreException(
					this.resource, beanName, elementName + " must have a subelement like <value> or <ref>");
		}
		//解析property子元素
		return parsePropertySubelement(valueRefOrCollectionElement, beanName);
	}

	/**
	 * Parse a value, ref or collection subelement of a property element
	 * @param ele subelement of property element; we don't know which yet
	 * 解析property的子元素，value或者ref或者集合
	 */
	protected Object parsePropertySubelement(Element ele, String beanName) {
		//bean，需要走bean解析流程
		if (ele.getTagName().equals(BEAN_ELEMENT)) {
			return parseBeanDefinition(ele);
		}
		//ref元素
		else if (ele.getTagName().equals(REF_ELEMENT)) {
			// a generic reference to any name of any bean
			//ref元素对应的bean属性
			String beanRef = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			//不存在bean属性
			if (!StringUtils.hasLength(beanRef)) {
				// a reference to the id of another bean in the same XML file
				//ref元素对应的local属性
				beanRef = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
				//不存在local
				if (!StringUtils.hasLength(beanRef)) {
					// a reference to the id of another bean in the same XML file
					//ref对应的parent属性
					beanRef = ele.getAttribute(PARENT_REF_ATTRIBUTE);
					if (!StringUtils.hasLength(beanRef)) {//不存在parent，需要抛异常
						throw new BeanDefinitionStoreException(
								this.resource, beanName, "'bean', 'local' or 'parent' is required for a reference");
					}
					//存在parent属性，使用RuntimeBeanReference封装，这里toParent属性为true
					return new RuntimeBeanReference(beanRef, true);
				}
			}
			//存在bean属性或者local属性，使用RuntimeBeanReference封装ref名称
			return new RuntimeBeanReference(beanRef);
		}
		//idref元素
		else if (ele.getTagName().equals(IDREF_ELEMENT)) {
			// a generic reference to any name of any bean
			//idref下的bean属性
			String beanRef = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			if (!StringUtils.hasLength(beanRef)) {//不存在bean属性
				// a reference to the id of another bean in the same XML file
				//idref下的local属性
				beanRef = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
				if (!StringUtils.hasLength(beanRef)) {
					//不存在bean和local。抛异常
					throw new BeanDefinitionStoreException(
							this.resource, beanName, "Either 'bean' or 'local' is required for an idref");
				}
			}
			return beanRef;
		}
		//list子元素
		else if (ele.getTagName().equals(LIST_ELEMENT)) {
			return getList(ele, beanName);
		}
		//set子元素
		else if (ele.getTagName().equals(SET_ELEMENT)) {
			return getSet(ele, beanName);
		}
		//map子元素
		else if (ele.getTagName().equals(MAP_ELEMENT)) {
			return getMap(ele, beanName);
		}
		//props子元素
		else if (ele.getTagName().equals(PROPS_ELEMENT)) {
			return getProps(ele, beanName);
		}
		//value子元素
		else if (ele.getTagName().equals(VALUE_ELEMENT)) {
			// it's a literal value
			return getTextValue(ele, beanName);
		}
		//null子元素
		else if (ele.getTagName().equals(NULL_ELEMENT)) {
			// it's a distinguished null value
			return null;
		}
		//其他子元素抛异常
		throw new BeanDefinitionStoreException(
				this.resource, beanName, "Unknown subelement of <property>: <" + ele.getTagName() + ">");
	}

	protected List getList(Element collectionEle, String beanName) {
		NodeList nl = collectionEle.getChildNodes();
		ManagedList list = new ManagedList(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element ele = (Element) nl.item(i);
				list.add(parsePropertySubelement(ele, beanName));
			}
		}
		return list;
	}

	protected Set getSet(Element collectionEle, String beanName) {
		NodeList nl = collectionEle.getChildNodes();
		ManagedSet set = new ManagedSet(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element ele = (Element) nl.item(i);
				set.add(parsePropertySubelement(ele, beanName));
			}
		}
		return set;
	}

	protected Map getMap(Element mapEle, String beanName) {
		List list = getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		Map map = new ManagedMap(list.size());
		for (int i = 0; i < list.size(); i++) {
			Element entryEle = (Element) list.get(i);
			String key = entryEle.getAttribute(KEY_ATTRIBUTE);
			// TODO hack: make more robust
			NodeList subEles = entryEle.getElementsByTagName("*");
			map.put(key, parsePropertySubelement((Element) subEles.item(0), beanName));
		}
		return map;
	}

	/**
	 * Don't use the horrible DOM API to get child elements:
	 * Get an element's children with a given element name
	 */
	protected List getChildElementsByTagName(Element mapEle, String elementName) {
		NodeList nl = mapEle.getChildNodes();
		List nodes = new ArrayList();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element && elementName.equals(n.getNodeName())) {
				nodes.add(n);
			}
		}
		return nodes;
	}

	protected Properties getProps(Element propsEle, String beanName) {
		Properties props = new Properties();
		NodeList nl = propsEle.getElementsByTagName(PROP_ELEMENT);
		for (int i = 0; i < nl.getLength(); i++) {
			Element propEle = (Element) nl.item(i);
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			// trim the text value to avoid unwanted whitespace
			// caused by typical XML formatting
			String value = getTextValue(propEle, beanName).trim();
			props.setProperty(key, value);
		}
		return props;
	}

	/**
	 * Make the horrible DOM API slightly more bearable:
	 * get the text value we know this element contains.
	 */
	protected String getTextValue(Element ele, String beanName) {
		StringBuffer value = new StringBuffer();
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item instanceof org.w3c.dom.CharacterData) {
				if (!(item instanceof Comment)) {
					value.append(item.getNodeValue());
				}
			}
			else {
				throw new BeanDefinitionStoreException(
						this.resource, beanName,
						"<value> element is just allowed to have text and comment nodes, not: " + item.getClass().getName());
			}
		}
		return value.toString();
	}

	protected int getDependencyCheck(String att) {
		int dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_NONE;
		if (DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_ALL;
		}
		else if (DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE;
		}
		else if (DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS;
		}
		// else leave default value
		return dependencyCheckCode;
	}

	protected int getAutowireMode(String att) {
		int autowire = RootBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_BY_NAME;
		}
		else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_BY_TYPE;
		}
		else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		}
		else if (AUTOWIRE_AUTODETECT_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_AUTODETECT;
		}
		// else leave default value
		return autowire;
	}

}
