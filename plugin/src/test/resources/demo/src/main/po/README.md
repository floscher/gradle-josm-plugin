# Translations

Put the `*.po` files that you get from the translators in here. The names of the files should be only the abbreviation of the language and the extension `po`.

As an example a file `de.po` is provided. Use one such file per language except `en`, English is considered the base language that is translated directly in the source code.

If you prefer, you could alternatively use `*.mo` or `*.lang` files. Then rename the directory to `src/main/mo` or `src/main/lang`.

If you have `gettext` installed, Gradle will pick these up and produce an internationalized plugin (see tasks `compilePo` and `compileMo`).
