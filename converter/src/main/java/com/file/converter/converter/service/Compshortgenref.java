package com.file.converter.converter.service;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Compshortgenref extends GenericField {
	@JsonProperty("$type") 
	 public String type;
	 public int order;
	 public String fieldCode;
	 public ArrayList<Entry> entries;
	 
	 public Compshortgenref() {
		super("690", new char[] {'e', 'f', '2'}, new String[] {"|", "|", "אסעי"}, ' ', ' ');
	 }
	 
	 public ArrayList<String> getSubFieldValue() {
		 ArrayList<String> values = new ArrayList<String>();
		 for (Entry e : entries) {
			 values.add(e.lines.get(0).textValue);
		 }
		 return values;
	 }
	 
	 public String getSubFieldLangValue() {
		return entries.get(0).lines.get(0).langCode;
	}
}
