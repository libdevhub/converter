package com.file.converter.converter.service;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Compspace {
	@JsonProperty("$type") 
	 public String type;
	 public int order;
	 public String fieldCode;
	 public ArrayList<Entry> entries;
}
