package com.file.converter.converter.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.ControlFieldImpl;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

  private final Path root = Paths.get("uploads");
  ArrayList<String> records = new ArrayList<String>();
  int PRIMO_HEADER_ID_LEN = 12;
  //HashMap<String, Integer> temp = new HashMap<String, Integer>();
  
  @Override
  public void init() {
    try {
      if (!Files.exists(root)){
    	  Files.createDirectory(root);
    	  System.out.println("Happen only once!!!");
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not initialize folder for upload!");
    }
  }

  @Override
  public void save(MultipartFile file) {
	  File marcXml = file.getOriginalFilename().endsWith(".json") ? jsonToMarcXmlProcess(file) : xmlToMarcXmlProcess(file);
//	  if(file.getName().endsWith(".json")) {
//		  marcXml = jsonToMarcXmlProcess(file);
//	  }
//	  else {
//		  marcXml = xmlToMarcXmlProcess(file);  
//	  }
	  
    try {
//      Files.copy(marcXml.toPath(), this.root.resolve(file.getOriginalFilename().endsWith(".json") ? file.getOriginalFilename().replace(".json", ".xml") : file.getOriginalFilename()));
        Files.copy(marcXml.toPath(), this.root.resolve(file.getOriginalFilename().endsWith(".json") ? "asai.xml" : file.getOriginalFilename()));

    } catch (Exception e) {
      throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
    }
  }

  @Override
  public Resource load(String filename) {
    try {
      Path file = root.resolve(filename);
      Resource resource = new UrlResource(file.toUri());

      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new RuntimeException("Could not read the file!");
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException("Error: " + e.getMessage());
    }
  }

  @Override
  public void deleteAll() {
    FileSystemUtils.deleteRecursively(root.toFile());
  }
  
  @Override
  public void delete(String fileName) {
	  System.out.println(fileName);
	  try {
		  Path file = root.resolve(fileName);
		  ///This is for return asai xml file with d in leader
//		  if(fileName.contains("asai")) {
//			  InputStream in = new FileInputStream(file.toFile());
//			    MarcReader reader = new MarcXmlReader(in);
//			    while (reader.hasNext()) {
//			        Record record = reader.next();
//			        Leader leader = record.getLeader();
//			        if (leader != null) {
//			        	leader.setRecordStatus('d');
//			        	record.setLeader(leader);
//			        	Files.copy(file, this.root.resolve("asai_for_delete.xml"));
//			        }
//			    }
//		  }
	      if(!file.toFile().delete()) {
	    	  throw new RuntimeException("Could not delete file " + fileName);
	      }
	  } catch (Exception e) {
		  throw new RuntimeException("Could not delete file " + fileName + " " + e.getMessage());
	}
      
  }

  @Override
  public Stream<Path> loadAll() {
    try {
      return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
    } catch (IOException e) {
      throw new RuntimeException("Could not load the files!");
    }
  }

  @Override
  public File xmlToMarcXmlProcess(MultipartFile file) {
	  //Check that the file is xml file
	  //if(isXmlFile(file)) {
		//Takes all records
	  NodeList recordsList = getAllRecords(file);
	  return createXmlFile(recordsList);
//	  }
//	  else {
//		  throw new RuntimeException("Unexpected file format!!! Please check the uploaded file: " + file.getName());
//	  }
	  
//	  FileItem fileItem = new DiskFileItem("mainFile", Files.probeContentType(marcXmlFile.toPath()), false, marcXmlFile.getName(), (int) marcXmlFile.length(), marcXmlFile.getParentFile());
//	  new FileInputStream(marcXmlFile).transferTo(fileItem.getOutputStream());
//	  return new CommonsMultipartFile(fileItem);
	  //return null;
  }
  
  public File jsonToMarcXmlProcess(MultipartFile file) {
	  File primoAsaiDir = new File("C:\\Users\\ajacobsmo\\Desktop\\asai\\asai_out5");
	  if (!primoAsaiDir.exists()){
		  primoAsaiDir.mkdirs();
	  }
	  ArrayList<Model> models = extractJsonData(file);
	  for (int i=0; i<models.size(); i++) {
		  ArrayList<String> asai = models.get(i).title.getSubFieldValue();
		  String asaiNum = asai.get(0).substring(6);
		  //Code to find asai story that appears more then once for internal use of Michal
//		  if(temp.containsKey(asaiNum)) {
//			  int num = temp.get(asaiNum);
//			  System.out.println(asaiNum + " appears " + num + " times");
//			  temp.replace(asaiNum, temp.get(asaiNum), num+1);
//		  }
//		  else {
//			  temp.put(asaiNum, 1);
//		  }
		  if("9173".equals(asaiNum) || "5756".equals(asaiNum)) {
			  System.out.println(asaiNum);
		  }
		  wrapXmlWithPrefixAndSuffix(createXmlFileFromJsonData(models.get(i), i), i, asaiNum);
	  }
	  return primoAsaiDir;
  }
  
  private ArrayList<Model> extractJsonData(MultipartFile file) {
	
	  ArrayList<Model> models = new ArrayList<Model>();
	  //Model model = null;
	  JSONArray objs;
	try {
//		File convFile = new File( file.getOriginalFilename());
//		file.transferTo(convFile);
		Reader r = new InputStreamReader(file.getInputStream());
		objs = (JSONArray) new JSONParser().parse(r);
		for(int i=0; i<objs.size(); i++) {
			JSONObject jo = (JSONObject) objs.get(i);
			JSONObject metaData = (JSONObject) jo.get("metaData");
	        JSONObject data = (JSONObject) metaData.get("Data");
	     
	        Gson gson = new Gson();
	        Model model = gson.fromJson(data.toJSONString(), Model.class);
	        if (model.asainumber != null) {
	        	models.add(model);
	        }
		}
        
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return models;
  }
  
  private boolean isXmlFile(MultipartFile file) {
	  boolean isXml = false;
	  return isXml;
  }
  
  private NodeList getAllRecords(MultipartFile file) {
	  try {
		  DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = factory.newDocumentBuilder();
          Document doc = dBuilder.parse(file.getInputStream());
          
          doc.getDocumentElement().normalize();

          System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());
          System.out.println("------");
          
          NodeList docRecords = doc.getElementsByTagName("record");

          for (int i=0; i<docRecords.getLength(); i++) {
        	  Node nNode = docRecords.item(i);
              System.out.println("\nCurrent Element: " + nNode.getNodeName());
              if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            	  Element elem = (Element) nNode;
                  String uid = elem.getAttribute("id");
                  records.add(uid);
              }
          }
          return docRecords;

      } catch (ParserConfigurationException | SAXException | IOException e) {
    	  throw new RuntimeException("Could not parse the file!");
      }
  }
  
  private String getTagDataField(String name) {
//	  String tag = null;
//	  if(name.length() >= 3) {
//		  tag = name.substring(0, 4);
//	  }
	  return name.substring(0, 3).matches("[0-9]{3,}") ? name.substring(0, 3) : null;
  }
  
  private char[] getIndicatorsDataField(String name) {
	  char[] indc = new char[2];
	  indc[0] = ' ';
	  indc[1] = ' ';
	  String firstIndc = name.substring(3, 4);
	  String secondIndc = name.length() > 4 ? name.substring(4, 5) : "";
	  if(firstIndc.matches("\\d")) {
		  indc[0] = firstIndc.charAt(0);
	  }
	  if(secondIndc.matches("\\d")) {
		  indc[1] = secondIndc.charAt(0);
	  }	  		  
	  return indc;
  }
  
  private char getSubFieldCode(String name) {
	  char firstCharacter = (char) name.chars()
		        .filter(c -> String.valueOf((char) c).matches("[a-zA-Z]"))
		        .findFirst()
		        .orElseThrow(() -> null);
	  return firstCharacter;
  }
	
  private File createXmlFile(NodeList recordsList) {
	  try {
	      File file = File.createTempFile("bib3_", null);
	      FileOutputStream fos = new FileOutputStream(file);
	      MarcXmlWriter writer = new MarcXmlWriter(fos, true);
	      ControlFieldImpl controllField = new ControlFieldImpl("008", "220106s9999||||xx |||||||||||||| ||und||");
	      LeaderImpl leader = new LeaderImpl("00609nam a2200193Ia 4500");
	      for (int i=0; i<recordsList.getLength(); i++) {
        	  Node nNode = recordsList.item(i);
              System.out.println("\nCurrent Element: " + nNode.getNodeName());
              if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            	  Element elem = (Element) nNode;
                  String uid = elem.getAttribute("id");
                  Record marcXmlRecord = new RecordImpl();
                  marcXmlRecord.setId(Long.getLong(uid));
                  marcXmlRecord.setLeader(leader);
                  //marcXmlRecord.setType(uid);
                  marcXmlRecord.addVariableField(controllField);
                  NodeList subRecords = elem.getElementsByTagName("subrecord");
                  for (int j=0; j<subRecords.getLength(); j++) {
                	  Node subRecordNode = subRecords.item(j); 
                	  System.out.println("\nCurrent Sub Element: " + subRecordNode.getNodeName());
                      if (subRecordNode.getNodeType() == Node.ELEMENT_NODE) {
                    	  Element subElem = (Element)subRecordNode;
                    	  String id = subElem.getAttribute("id");
//                    	  Node node1 = elem.getElementsByTagName("firstname").item(0);
//                          String fname = node1.getTextContent();
                    	  Node nameSubRecordNode = subElem.getElementsByTagName("name").item(0); 
                    	  String name = nameSubRecordNode.getTextContent();
                    	  if(name.length() > 3) {
                    		  Node valueSubRecordNode = subElem.getElementsByTagName("value").item(0); 
                        	  String value = valueSubRecordNode.getTextContent();
	                    	  if(value != "") {
		                    	  String tag = getTagDataField(name);
		                    	  char[] indicators = getIndicatorsDataField(name);
		                    	  char subFieldCode = getSubFieldCode(name);
		                    	  //String subFieldData = getSubFieldData(name);
		                    	  if (tag == null || subFieldCode == '\u0000') {
		                    		  System.out.println("Invalid name property in record id=" + uid + " subrecord id=" + id);
		                    		  continue;
		                    	  }
		                    	  DataFieldImpl dataField = new DataFieldImpl(tag, indicators[0], indicators[1]);
		                    	  SubfieldImpl subField = new SubfieldImpl(subFieldCode, value);//char code, string data
		                    	  List<DataField> recordsDataFeilds = marcXmlRecord.getDataFields();
		                    	  //if (recordsDataFeilds.contains(dataField)) {
		                    	  boolean flag = false;
		                    	  if(!tag.equals("650")) { //Not to join subfields in case of tag 650
		                    		  for (int k=0; k<recordsDataFeilds.size(); k++) {
		                    			  if (recordsDataFeilds.get(k).getTag().equals(dataField.getTag())) {
		                    				  recordsDataFeilds.get(k).addSubfield(subField);
		                    				  flag = true;
		                    				  break;
		                    			  }
		                    		  }
		                    	  }
		                    	  //}
	                    		  
		                    	  if(!flag) {
			                    	  dataField.addSubfield(subField);
			                    	  marcXmlRecord.addVariableField(dataField);
		                    	  }
	                    	  }
                          }
                    	  else {
                    		  System.out.println("Invalid name property in record id=" + uid + " subrecord id=" + id);
                    	  }
//                    	  <subrecord id="5515">
//              				<element>265</element>
//              				<name>24510a</name> => 245 1 0  a
//              				<title>כותר ראשי (Title)</title>
//              				<type>Text</type>
//              				<value>Privatization, demographic growth, and perceived sustainability: Lessons from the Israeli renewing kibbutzim</value>
//              			  </subrecord>
                      }
                  }
                  writer.write(marcXmlRecord);
              }
          }
	      
//	        BiblioDAO biblioDao = new BiblioDAO();
//	        int limit = 100;
//	        int recordCount = biblioDao.countAll(database);
//	        for (int offset = 0; offset < recordCount; offset += limit) {
//	            ArrayList<RecordDTO> records = biblioDao.list(database, MaterialType.ALL, offset, limit, false);
//	            for (RecordDTO dto : records) {
//	                final Record record = MarcUtils.iso2709ToRecord(dto.getIso2709());
//	                if (record != null) {
//	                    writer.write(record);
//	                }
//	            }
//	        }
	      writer.close();
	      return file;
	  } catch (Exception e) {
	    throw new RuntimeException("Could not read the file!");
	  }
  }
  
  private void addAsaiConstantFields(Record marcXmlRecord) {
	  
	  ArrayList<GenericField> constantFields = new ArrayList<GenericField>();
	  constantFields.add(new Constant("040", new char[]{'b', 'e'}, new String[]{"heb", "rda"}, ' ' , ' '));
	  constantFields.add(new Constant("336", new char[]{'a', 'b', '2'}, new String[]{"text", "txt", "rdacontent"}, ' ' , ' '));
	  constantFields.add(new Constant("337", new char[]{'a', 'b', '2'}, new String[]{"computer", "c", "rdamedia"}, ' ' , ' '));
	  constantFields.add(new Constant("338", new char[]{'a', 'b', 'c'}, new String[]{"online resource", "cr", "rdacarrier"}, ' ' , ' '));
	  constantFields.add(new Constant("347", new char[]{'a', 'b', '2'}, new String[]{"image file", "TIF", "rda"}, ' ' , ' '));
	  constantFields.add(new Constant("347", new char[]{'a', 'b', '2'}, new String[]{"text file", "PDF", "rda"}, ' ' , ' '));
	  constantFields.add(new Constant("542", new char[]{'a'}, new String[]{"מצויות בידי ארכיון הסיפור העממי בישראל ע\"ש דב נוי (אסע\"י) באוניברסיטת חיפה. השימוש בסיפור מחייב לציין את שמו המלא של הארכיון:  \"ארכיון הסיפור העממי בישראל ע\"ש דב נוי (אסע\"י), באוניברסיטת חיפה\" ואת הפרטים הבאים: שם רושם/רושמת הסיפור, שם מספר/ת הסיפור, עדת המוצא, המספר הסידורי של הסיפור בארכיון"}, ' ' , ' '));
	  constantFields.add(new Constant("552", new char[]{'a', 'u'}, new String[]{"הסיפור נקלט ותועד ע\"י ארכיון הסיפור העממי בישראל ע\"ש דב נוי (אסע\"י), באוניברסיטת חיפה", "http://ifa.haifa.ac.il"}, ' ' , ' '));
	  constantFields.add(new Constant("583", new char[]{'a', 'k'}, new String[]{"התאמת הרשומה לקטלוג:", "ספריית אוניברסיטת חיפה"}, ' ' , ' '));
	  constantFields.add(new Constant("595", new char[]{'a'}, new String[]{"ASAI" + LocalDate.now().getYear()}, ' ' , ' '));
//	  constantFields.add(new Constant("710", new char[]{'a', '9', 'e'}, new String[]{"ארכיון הספור העממי בישראל ע\"ש דב נוי", "heb", "גוף מנפיק"}, '2' , ' '));
	  constantFields.add(new Constant("999", new char[]{'a'}, new String[]{"DOC"}, ' ' , ' '));
	  constantFields.add(new Constant("999", new char[]{'a'}, new String[]{"DTEXT"}, ' ' , ' '));
	  


	  for (GenericField constant : constantFields) {
		  DataFieldImpl marcXmlDataField = new DataFieldImpl(constant.TAG, constant.IND1, constant.IND2);
	      for (int i=0; i<constant.SUB_FIELD_CODE.length; i++) {
	     	 SubfieldImpl marcXmlSubField = new SubfieldImpl(constant.SUB_FIELD_CODE[i], constant.SUB_FIELD_VALUES[i]);//char code, string data
	     	 marcXmlDataField.addSubfield(marcXmlSubField);
	      }
	      marcXmlRecord.addVariableField(marcXmlDataField);
	  }
  }
  
  private File wrapXmlWithPrefixAndSuffix(File marcXml, int index, String asaiNum) {
	  if(asaiNum.equalsIgnoreCase("25")) {
		  System.out.println("25");
	  }
	  
	  try {
		  File tempFile = new File("C:\\\\Users\\\\ajacobsmo\\\\Desktop\\\\asai\\\\asai_out5\\\\asai_out_" + index + ".xml");

		  BufferedReader reader = new BufferedReader(new FileReader(marcXml));
		  BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		  
		  int i = PRIMO_HEADER_ID_LEN - asaiNum.length();
		  StringBuffer id = new StringBuffer();
		  for(int j=0; j<i; j++) {
			  id.append("0");
		  }
		  id.append(asaiNum);
		  
		  
		  String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
		      		+ "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\r\n"
		      		+ "	<ListRecords>\r\n"
		      		+ "		<record>\r\n"
		      		+ "			<header>\r\n"
		      		+ "				<identifier>urm-publish:" + id + "</identifier>\r\n"
		      		+ "			</header>\r\n"
		      		+ "			<metadata>"
		      		+ "	"
		      		+ "			";
		  writer.write(prefix + System.getProperty("line.separator"));
		  
		  //String lineToRemove = "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>";
		  String lineToRemove = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><marc:collection xmlns:marc=\"http://www.loc.gov/MARC21/slim\">";
		  String endlineToRemove = "</marc:collection>";
		  String currentLine;

		  while((currentLine = reader.readLine()) != null) {
		      // trim newline when comparing with lineToRemove
		      String trimmedLine = currentLine.trim();
		      if(trimmedLine.contains(lineToRemove) || trimmedLine.contains(endlineToRemove)) continue;
		      currentLine = currentLine.replaceFirst("<marc:", "<");
		      currentLine = currentLine.replace("</marc:", "</");
		      writer.write("			" + currentLine + System.getProperty("line.separator"));
		  }
	      String suffix = "			</metadata>\r\n"
    		+ "		</record>\r\n"
    		+ "	</ListRecords>\r\n"
    		+ "</OAI-PMH>";
	      writer.write(suffix);
		  writer.close(); 
		  reader.close(); 
		  return tempFile;
	  } catch (Exception e) {
		  throw new RuntimeException("Could not wrap the file with primo prefix and suffix! error caused by: " + e.getMessage());
	}
	  
//	  FileOutputStream fos = null;
//      try {
//          FileInputStream in = new FileInputStream(marcXml);
//	      File file = File.createTempFile("bib4_", null);
//	      fos = new FileOutputStream(file);
//	      String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
//	      		+ "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\r\n"
//	      		+ "	<ListRecords>\r\n"
//	      		+ "		<record>\r\n"
//	      		+ "			<header>\r\n"
//	      		+ "				<identifier>aleph-publish:000003834</identifier>\r\n"
//	      		+ "			</header>\r\n"
//	      		+ "			<metadata>"
//	      		+ "				";
//	      byte[] array = prefix.getBytes();
//	      fos.write(array);
//	      int n;
//	      while ((n = in.read()) != -1) {
//              // write() function to write
//              // the byte of data
//	    	  fos.write(n);
//          }
//	      String suffix = "			</metadata>\r\n"
//		      		+ "		</record>\r\n"
//		      		+ "	</ListRecords>\r\n"
//		      		+ "</OAI-PMH>";
//	      byte[] suffixArr = suffix.getBytes();
//	      fos.write(suffixArr);
//	      fos.close();
//	      in.close();
//	      return file;
//	  } catch (Exception e) {
//		    throw new RuntimeException("Could not read the file!");
//	  }
	   
  }
  
  private File createXmlFileFromJsonData(Model data, int index) {
	  FileOutputStream fos = null;
	  FileDescriptor fd = null;
	  MarcXmlWriter writer = null;
	  try {
		  /////////////
		  //FileWriter myWriter = new FileWriter("filename.txt");
		  //myWriter.write("Files in Java might be tricky, but it is fun enough!");
		  //myWriter.close();
		  ////////////
	      File file = File.createTempFile("C:\\\\Users\\\\ajacobsmo\\\\Desktop\\\\asai\\\\asai_out5\\\\bib3_" + index, null);
	      fos = new FileOutputStream(file);
//	      fd = fos.getFD();
//	      String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
//	      		+ "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\r\n"
//	      		+ "	<ListRecords>\r\n"
//	      		+ "		<record>\r\n"
//	      		+ "			<header>\r\n"
//	      		+ "				<identifier>aleph-publish:000003834</identifier>\r\n"
//	      		+ "			</header>\r\n"
//	      		+ "			<metadata>"
//	      		+ "				";
//	      byte[] array = prefix.getBytes();
//	      fos.write(array);
	      writer = new MarcXmlWriter(fos, true);
	      MarcFactory factory = MarcFactory.newInstance();
	      Record marcXmlRecord = factory.newRecord("00000nam a2200000 i 4500");
//	      Record marcXmlRecord = new RecordImpl();
	      Date date = new Date();
	      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
	      //ControlFieldImpl controllField = new ControlFieldImpl("008", "s1971 is s ||| | heb d");
	      ControlFieldImpl controllField = new ControlFieldImpl("008", formatter.format(date));
//	      LeaderImpl leader = new LeaderImpl("00000nam a2200000 i 4500");
//	      marcXmlRecord.setLeader(leader);
	      marcXmlRecord.addVariableField(controllField);
	      addAsaiConstantFields(marcXmlRecord);
	      Field[] dataFields = data.getClass().getFields();
	      HashMap<String, ComplexField.ComplexFieldData> complexFieldsHash = new HashMap<String, ComplexField.ComplexFieldData>();
	      for(int i = 0; i < dataFields.length; i++) {
	         //System.out.println("The field is: " + dataFields[i].getName());
	         Field field = Model.class.getField(dataFields[i].getName());
	         GenericField f;
	         try {
		         f = (GenericField)field.get(data);
	         }
	         catch (Exception e) {
	        	if(field.get(data) instanceof ComplexField) {
		        	complexFieldsHash.putAll(((ComplexField)(field.get(data))).getCodeAndTextValues());
		        }
				continue;
			}
	         //No such feild in model
	         if (f == null) {
	        	 continue;
	         }
	         //ArrayList<String> subFieldDataTextValues = ((GenericField)(field.get(data))).getSubFieldValue();
	         //In complex field get the ids instead of values because of wrong issue in the order of keysoft else take the textvalue
	         ArrayList<String> subFieldDataTextValues = ((GenericField)(field.get(data))).getSubFieldValueAsRelated() != null ? ((GenericField)(field.get(data))).getSubFieldValueAsRelated() : ((GenericField)(field.get(data))).getSubFieldValue();
	         String[] subFieldValuesOriginal = f.SUB_FIELD_VALUES;
	         for (String subFieldDataTextValue : subFieldDataTextValues) {
		         String[] subFieldValues = subFieldValuesOriginal.clone();
//		         if("sogazikaf".equalsIgnoreCase(dataFields[i].getName())) {
//		        	 System.out.println("sogazikaf");
//		        	 
//		         }
//		         if("compplant".equalsIgnoreCase(dataFields[i].getName())) {
//		        	 System.out.println("plant");
//		         }
//		         if(asaiNum.equals("8771") && "storyattype".equalsIgnoreCase(dataFields[i].getName())) {
//		        	 System.out.println("");
//		         }
//		         ArrayList<String> asai = models.get(i).title.getSubFieldValue();
//				  String asaiNum = asai.get(0).substring(6);
//		         if (("compcharacterknownf".equalsIgnoreCase(dataFields[i].getName()) || "compcharacterunonymf".equalsIgnoreCase(dataFields[i].getName()) || 
//		        		 "compobjectf".equalsIgnoreCase(dataFields[i].getName()) || "componentf".equalsIgnoreCase(dataFields[i].getName()) || 
//		        		 "compplant".equalsIgnoreCase(dataFields[i].getName()) || "compshortgenref".equalsIgnoreCase(dataFields[i].getName()) ||
//		        		 "compspacef".equalsIgnoreCase(dataFields[i].getName()) || "sogazikaf".equalsIgnoreCase(dataFields[i].getName()) ||
//		        		 "transformationf".equalsIgnoreCase(dataFields[i].getName())) && !subFieldDataTextValue.contains("|")) {
//		        	 System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + "asai # " + asaiNum + " name " + dataFields[i].getName() + " vlaue " + subFieldDataTextValue);
//		        	 myWriter.write("asai # " + asaiNum + " name " + dataFields[i].getName() + " vlaue " + subFieldDataTextValue);
//		        	 myWriter.write(System.getProperty( "line.separator" ));
//		         }
		         
	        	 int counter = 0;
	        	 for(String x : subFieldValues){
        		    if(x!=null && x.equals("|")){
        		    	counter++;
        		    }
        		 }
	        	 if (counter >= 2) {
	        		 counter = 1;
	        		 String[] values = new String[2];
	        		 if (subFieldDataTextValue.contains("|")) {
	        			 values = subFieldDataTextValue.split(" \\| ");
	        		 }
	        		 else {
	        			 values[0] = subFieldDataTextValue;
	        			 values[1] = "###";
	        		 }
	        		 for (int k=0; k<subFieldValues.length; k++) {
	        			 if (subFieldValues[k].equals("|")) {
	        				 //As Nir asked soga goes to c and zika goes to d 
	        				 if("sogazikaf".equalsIgnoreCase(dataFields[i].getName())) {
	        					 subFieldValues[k] = counter == 1 ? subFieldValues[k].replace("|", values[0]) : subFieldValues[k].replace("|", values[1]);
	        				 }
	        				 else {
	        					 subFieldValues[k] = counter == 1 ? subFieldValues[k].replace("|", values[1]) : subFieldValues[k].replace("|", values[0]);
	        				 }
		        			 counter++;
	        			 }
	        		 }

	        	 }
		         /////////////////////////////////////////////////////////
//		         if (subFieldDataTextValue.contains("|")) {
//		        	 int counter = 0;
//		        	 for(String x : subFieldValues){
//	        		    if(x!=null && x.equals("|")){
//	        		    	counter++;
//	        		    }
//	        		 }
//		        	 if (counter >= 2) {
//		        		 counter = 1;
//		        		 String[] values = subFieldDataTextValue.split(" \\| ");
//		        		 for (int k=0; k<subFieldValues.length; k++) {
//		        			 if (subFieldValues[k].equals("|")) {
//		        				 //As Nir asked soga goes to c and zika goes to d 
//		        				 if("sogazikaf".equalsIgnoreCase(dataFields[i].getName())) {
//		        					 subFieldValues[k] = counter == 1 ? subFieldValues[k].replace("|", values[0]) : subFieldValues[k].replace("|", values[1]);
//		        				 }
//		        				 else {
//		        					 subFieldValues[k] = counter == 1 ? subFieldValues[k].replace("|", values[1]) : subFieldValues[k].replace("|", values[0]);
//		        				 }
//			        			 counter++;
//		        			 }
//		        		 }
//	
//		        	 }
//		         }
//		         //In soga zike there are lots of problems in cataloging many times there is no value of zika
//		         if("sogazikaf".equalsIgnoreCase(dataFields[i].getName()) && "|".equals(subFieldValues[0])) {
////		        	 System.out.println("sogazikaf");
//		        	 subFieldValues[0] = subFieldValues[0].replace("|", subFieldDataTextValue);
//		        	 subFieldValues[1] = "";
//		         }
		         /////////////////////////////////////////////////////////
//		         if("|".equals(subFieldValues[0]) && "|".equals(subFieldValues[1])) {
//		        	 ArrayList<String> subFieldDataRelationValues = ((GenericField)(field.get(data))).getRelatedFieldValue();
//		        	 if(subFieldDataRelationValues != null && subFieldDataRelationValues.size() > 0 && subFieldDataRelationValues.size() < 3) {
//		        		 
//		        	 }
//		         }
		         DataFieldImpl marcXmlDataField = new DataFieldImpl(f.TAG, f.IND1, f.IND2);
		         for (int j=0; j<subFieldValues.length; j++) {
		        	 String value = subFieldValues[j] != null ? subFieldValues[j] : f.SUB_FIELD_CODE[j] != '9' ? subFieldDataTextValue : ((GenericField)(field.get(data))).getSubFieldLangValue();
		        	 
		        	 SubfieldImpl marcXmlSubField = new SubfieldImpl(f.SUB_FIELD_CODE[j], value);//char code, string data
		        	 marcXmlDataField.addSubfield(marcXmlSubField);
		         }
		         marcXmlRecord.addVariableField(marcXmlDataField);
	         }

	      }
	      
	      //Fixes manually!
	      //Title and asia number are unique fields - should edit it manually and should add 246 field
	      DataFieldImpl title = (DataFieldImpl)marcXmlRecord.getVariableField("245");
	      DataFieldImpl asaiNumber = (DataFieldImpl)marcXmlRecord.getVariableField("093");
	      String titleSubFieldDataA = title.getSubfield('a').getData();
	      title.getSubfield('a').setData(titleSubFieldDataA + " - " + asaiNumber.getSubfield('a').getData());
	      DataFieldImpl marcXmlDataField = new DataFieldImpl("246", '3', '3');
	      SubfieldImpl marcXmlSubField = new SubfieldImpl('a', asaiNumber.getSubfield('a').getData() + " - " + titleSubFieldDataA);//char code, string data
	      marcXmlDataField.addSubfield(marcXmlSubField);
	      marcXmlRecord.addVariableField(marcXmlDataField);
	      asaiNumber.getSubfield('a').setData(titleSubFieldDataA);
	      //Edit controlfield 008
	      String yearInControlField = "n####";
	      VariableField year = marcXmlRecord.getVariableField("264");
	      if (year != null) {
	    	  yearInControlField = "s" + ((DataFieldImpl) year).getSubfield('c').getData();
	      }
	      else {
	    	  DataFieldImpl yearMarcXmlDataField = new DataFieldImpl("264", ' ', '1');
		      SubfieldImpl yearMarcXmlSubField = new SubfieldImpl('c', "0");//char code, string data
		      yearMarcXmlDataField.addSubfield(yearMarcXmlSubField);
		      marcXmlRecord.addVariableField(yearMarcXmlDataField);
	      }
	      List<ControlField> cfl = marcXmlRecord.getControlFields();
	      for (ControlField cf : cfl) {
	    	  if(cf.getTag().equals("008")) {
	    		  String cfData = cf.getData();
	    		  String updatedCf = cfData + yearInControlField + "is######s#####0|0#|#heb#d";
	    		  cf.setData(updatedCf);
	    	  }
	      }
	      
	      List<VariableField> allRelationDFInRecord = marcXmlRecord.getVariableFields("690");
	      for (VariableField vf : allRelationDFInRecord) {
	    	  DataField df = (DataField)vf;
	    	  if(df.getSubfields().size() < 2) { //The only place where 690 has only one subfield is with compplant so ignore this case and continue
	    		  continue;
	    	  }
	    	  Subfield sf1 = df.getSubfields().get(0);
	    	  Subfield sf2 = df.getSubfields().get(1);
	    	  ComplexField.ComplexFieldData complexFieldValue1 = complexFieldsHash.get(sf1.getData());
	    	  ComplexField.ComplexFieldData complexFieldValue2 = complexFieldsHash.get(sf2.getData());
	    	  if(complexFieldValue1!=null && complexFieldValue2!=null) {
	    		  if(complexFieldValue1.type == ComplexField.TYPE_FIELD && complexFieldValue2.type == ComplexField.VALUE_FIELD) {
	    			  sf1.setData(complexFieldValue1.value);
	    			  sf2.setData(complexFieldValue2.value);
	    		  }
	    		  else if(complexFieldValue1.type == ComplexField.VALUE_FIELD && complexFieldValue2.type == ComplexField.TYPE_FIELD) {
	    			  sf1.setData(complexFieldValue2.value);
	    			  sf2.setData(complexFieldValue1.value);
	    		  }
	    		  else {
	    			  System.out.println("Something wrong with data field 690" + " and subfield: " + sf1.getCode() + " value: " + sf1.getData() + " and subfield: " + sf2.getCode() + " value " + sf2.getData());
	    		  }
	    	  }
	    	  else if(complexFieldValue1!=null && complexFieldValue2==null) {
	    		  if(complexFieldValue1.type == ComplexField.TYPE_FIELD) {
	    			  sf1.setData(complexFieldValue1.value);
	    		  }
	    		  else if(complexFieldValue1.type == ComplexField.VALUE_FIELD) {
	    			  sf1.setData(sf2.getData());
	    			  sf2.setData(complexFieldValue1.value);
	    		  }
	    	  }
	    	  else if(complexFieldValue1==null && complexFieldValue2!=null) {
	    		  if(complexFieldValue2.type == ComplexField.VALUE_FIELD) {
	    			  sf2.setData(complexFieldValue2.value);
	    		  }
	    		  else if(complexFieldValue2.type == ComplexField.TYPE_FIELD) {
	    			  sf2.setData(sf1.getData());
	    			  sf1.setData(complexFieldValue2.value);
	    		  }
	    	  }
	    	  
	      }
	      Collections.sort(allRelationDFInRecord, new Comparator<VariableField>() {
	    	  @Override
	    	  public int compare(VariableField vf1, VariableField vf2) {
	    		DataField df1 = (DataField)vf1;
	    		DataField df2 = (DataField)vf2;
	    	    return ((Character.toString(df1.getSubfields().get(0).getCode())).compareTo(Character.toString(df2.getSubfields().get(0).getCode())));
	    	  }
	      });
	      for (VariableField vf : allRelationDFInRecord) {
	    	  marcXmlRecord.removeVariableField(vf);
	      }
	      for (VariableField vf : allRelationDFInRecord) {
	    	  marcXmlRecord.addVariableField(vf);
	      }
	      
	      List<DataField> allDFInRecord = marcXmlRecord.getDataFields();
	      //Collections.sort(allDFInRecord, (DataField df1, DataField df2) -> df1.tag-df2.tag);
	      //allDFInRecord.sort((df1,df2->df1.getTimeStarted().compareTo(df2.getTimeStarted())));
	      Collections.sort(allDFInRecord, new Comparator<DataField>() {
	    	  @Override
	    	  public int compare(DataField df1, DataField df2) {
	    	    return df1.getTag().compareTo(df2.getTag());
	    	  }
	      });
	      
	      writer.write(marcXmlRecord);
	      // flush data from the stream into the buffer
//	      fos.flush();
	      // confirms data to be written to the disk
//	      fd.sync();
//	      System.out.println(marcXmlRecord.toString());
	      // create in
//	      String suffix = "			</metadata>\r\n"
//		      		+ "		</record>\r\n"
//		      		+ "	</ListRecords>\r\n"
//		      		+ "</OAI-PMH>";
//	      byte[] suffixArr = suffix.getBytes();
//	      fos.write(suffixArr);
//	      writer.close();
	      return file;
	  } catch (Exception e) {
	    throw new RuntimeException("Could not read the file!");
	  } finally {
		  if(fos!=null && writer!=null) {
//			  String suffix = "			</metadata>\r\n"
//			      		+ "		</record>\r\n"
//			      		+ "	</ListRecords>\r\n"
//			      		+ "</OAI-PMH>";
//		      byte[] suffixArr = suffix.getBytes();
//		      try {
//				fos.write(suffixArr);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		      writer.close();
		  }  
	}
  }
}