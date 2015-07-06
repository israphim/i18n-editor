package com.jvms.i18neditor;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jvms.i18neditor.event.ResourceEvent;
import com.jvms.i18neditor.event.ResourceListener;
import com.jvms.i18neditor.util.TranslationKeys;

public class Resource {
	private final Path path;
	private final Locale locale;
	private final SortedMap<String,String> translations;
	private final List<ResourceListener> listeners;
	private final ResourceType type;
	
	public Resource(ResourceType type, Path path, Locale locale) {
		this(type, path, locale, Maps.newTreeMap());
	}
	
	public Resource(ResourceType type, Path path, Locale locale, SortedMap<String,String> translations) {
		this.path = path;
		this.translations = translations;
		this.locale = locale;
		this.listeners = Lists.newLinkedList();
		this.type = type;
	}
	
	public ResourceType getType() {
		return type;
	}
	
	public Path getPath() {
		return path;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public SortedMap<String,String> getTranslations() {
		return ImmutableSortedMap.copyOf(translations);
	}
	
	public void storeTranslation(String key, String value) {
		String existing = translations.get(key);
		if (existing != null && existing.equals(value)) return;
		if (existing == null && value.isEmpty()) return;
		removeParents(key);
		removeChildren(key);
		if (value.isEmpty()) {
			translations.remove(key);
		} else {
			translations.put(key, value);
		}
		notifyListeners();
	}
	
	public void removeTranslation(String key) {
		removeChildren(key);
		translations.remove(key);
		notifyListeners();
	}
	
	public void renameTranslation(String key, String newKey) {
		Map<String,String> newTranslations = Maps.newTreeMap();
		translations.keySet().forEach(k -> {
			if (TranslationKeys.isChildKeyOf(k, key)) {
				String nk = TranslationKeys.create(newKey, TranslationKeys.childKey(k, key));
				newTranslations.put(nk, translations.get(k));
			}
		});
		if (translations.containsKey(key)) {
			newTranslations.put(newKey, translations.get(key));
		}
		removeChildren(newKey);
		translations.remove(newKey);
		removeChildren(key);
		translations.remove(key);
		translations.putAll(newTranslations);
		notifyListeners();
	}
	
	public void addListener(ResourceListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ResourceListener listener) {
		listeners.remove(listener);
	}
	
	private void removeChildren(String key) {
		Lists.newLinkedList(translations.keySet()).forEach(k -> {
			if (TranslationKeys.isChildKeyOf(k, key)) {
				translations.remove(k);
			}
		});
	}
	
	private void removeParents(String key) {
		Lists.newLinkedList(translations.keySet()).forEach(k -> {
			if (TranslationKeys.isChildKeyOf(key, k)) {
				translations.remove(k);
			}
		});
	}
	
	private void notifyListeners() {
		listeners.forEach(l -> l.resourceChanged(new ResourceEvent(this)));
	}
	
	public enum ResourceType {
		ES6, JSON
	}
}
