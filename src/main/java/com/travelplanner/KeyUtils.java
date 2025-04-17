package com.travelplanner;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KeyUtils {

  public static String readOpenAiKey() {
    return readKey("OPENAI_API_KEY");
  }


  public static boolean hasValidKeys() {
    try {
      return !readOpenAiKey().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  private static String readKey(String key) {

    // first read from env var
    var value = System.getenv(key);

    // if not available, read from src/main/resources/.env.local file
    if (value == null) {
      var properties = new Properties();

      try (InputStream in = KeyUtils.class.getClassLoader().getResourceAsStream(".env.local")) {

        if (in == null) throw new IllegalStateException("No .env.local file found");
        else properties.load(in);

        return properties.getProperty(key);

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return value;
  }
}
