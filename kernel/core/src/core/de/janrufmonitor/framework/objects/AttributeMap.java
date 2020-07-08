package de.janrufmonitor.framework.objects;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.janrufmonitor.framework.IAttribute;
import de.janrufmonitor.framework.IAttributeMap;

public class AttributeMap implements IAttributeMap {

	private final static int m_capacity = 1;
	
	private Map m_map;
	
	public AttributeMap() {
		this(m_capacity);
	}

	public AttributeMap(int capacity) {
		this.m_map = new ConcurrentHashMap(capacity);
	}

	public boolean contains(IAttribute att) {
		return this.m_map.containsKey(att.getName());
	}

	public boolean contains(String attName) {
		return this.m_map.containsKey(attName);
	}

	public void addAll(IAttributeMap map) {
		if (map instanceof AttributeMap)
			this.m_map.putAll(((AttributeMap)map).m_map);
	}
	
	public void add(IAttribute att) {
		this.m_map.put(att.getName(), att);
	}

	public void remove(IAttribute att) {
		this.m_map.remove(att.getName());
	}

	public void remove(String attName) {
		this.m_map.remove(attName);
	}

	public IAttribute get(String name) {
		return (IAttribute) this.m_map.get(name);
	}

	public int size() {
		return this.m_map.size();
	}

	public Iterator iterator() {
		return this.m_map.values().iterator();
	}

	public String toString() {
		return this.m_map.toString();
	}
}
