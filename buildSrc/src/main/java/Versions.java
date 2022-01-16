// No package to avoid having to use the fully qualified name in `buildscript{}` block (see https://github.com/gradle/gradle/issues/9270#issuecomment-400363045 )

public final class Versions {
  public static final String flexmark = "0.62.2";
  public static final String klaxon = "5.5";
  public static final String kotlin = "1.6.10";
  public static final String kotlinxHtml = "0.7.3";
  public static final String dokka = kotlin;
  public static final String kotlinSerialization = "1.3.2";
  public static final String jackson = "2.13.1";
  public static final String jacoco = "0.8.7";
  public static final String jgit = "5.13.0.202109080827-r"; // v6.0.0 would require an upgrade to Java 11
  public static final String junit = "5.8.2";
  public static final String okhttp = "4.9.3";
  public static final String pluginPublish = "0.19.0";
  public static final String proguardGradle = "7.1.1";
  public static final String wiremock = "2.32.0";
}
