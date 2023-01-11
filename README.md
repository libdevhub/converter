# converter
Rest api to upload and download xml and json files with correspondence parsers.
This is the backend java spring project - expose a rest api for file upload and file download.
It parsed the xml files from מכון לחקר הקיבוץ to marcxml needed in Alma.
It parsed json files from ASAI ארכיון הסיפור העממי to marcxml needed im Primo - 
the json parser works with predefinded schema model and uses gson to convert the data from json to our model
