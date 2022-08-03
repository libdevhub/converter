package com.file.converter.converter.service;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public interface FilesStorageService {
  public void init();

  public void save(MultipartFile file);

  public Resource load(String fileName);
  
  public void delete(String fileName);

  public void deleteAll();

  public Stream<Path> loadAll();
  
  public File xmlToMarcXmlProcess(MultipartFile file);
  
  public File jsonToMarcXmlProcess(MultipartFile file);
}