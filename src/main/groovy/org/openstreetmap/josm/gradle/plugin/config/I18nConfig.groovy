package org.openstreetmap.josm.gradle.plugin.config;

@groovy.transform.CompileStatic
public class I18nConfig {
  /**
   * E-Mail address to which bugs regarding i18n should be reported.
   * This will be put into the *.pot files that are forwarded to the translators.
   * <p><strong>Default value:</strong> {@code null}</p>
   */
  def String bugReportEmail = null;
  /**
   * Person or organization that holds the copyright on the project.
   * This will appear in the header of the *.pot file as follows:
   * <pre># Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER</pre>
   */
  def String copyrightHolder = null;
}
