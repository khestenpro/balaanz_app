package com.bitsvalley.micro.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
@Slf4j
public class FileWriter {

  public static final String WINDOWS = "wind";
  public static final String OS_NAME = "os.name";
  public static final String LOCAL_TEMP_PATH = "C:\\";
  private static final String UNIX_SEPARATOR = "/";
  private static final String WIN_SEPARATOR = "\\";

  @Value("${server.file.path.root}")
  private String serverFilePath;
  @Value("${imageUrlPath}")
  private String imageUrlPath;

  public Optional<String> writingFile(MultipartFile file){
    try {
      if(null != file){
        String separator = UNIX_SEPARATOR;
        byte[] bytes = file.getBytes();

        String rootPath = serverFilePath.concat(separator);
        String actualPath = String.valueOf(System.currentTimeMillis())
          .concat(separator)
          .concat(file.getOriginalFilename());
        if(System.getProperty(OS_NAME).toLowerCase().contains(WINDOWS)){
          rootPath = LOCAL_TEMP_PATH;
          separator = WIN_SEPARATOR;
          actualPath = actualPath.replace(UNIX_SEPARATOR, WIN_SEPARATOR);
          imageUrlPath = rootPath;
        }

        Path path = Paths.get(rootPath.concat(actualPath));
        File f = new File(String.valueOf(path));
        File parentFile = f.getParentFile();
        if (!parentFile.exists()) {
          boolean mkdirs = parentFile.mkdirs();
        }
        Files.write(path, bytes);
        return Optional.of((imageUrlPath.concat(separator).concat(actualPath)));
      }
    } catch (IOException e) {
      log.error("Error caught while writing file to server",e);
    }
    return Optional.empty();
  }

}
